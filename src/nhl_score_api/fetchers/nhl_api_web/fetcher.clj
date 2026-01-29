(ns nhl-score-api.fetchers.nhl-api-web.fetcher
  (:require [clj-time.core :as time]
            [malli.core :as malli]
            [malli.error :as malli-error]
            [nhl-score-api.cache :as cache]
            [nhl-score-api.fetchers.nhl-api-web.api-request-queue :as api-request-queue]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.landing :as landing]
            [nhl-score-api.fetchers.nhl-api-web.api.right-rail :as right-rail]
            [nhl-score-api.fetchers.nhl-api-web.api.rosters :as rosters]
            [nhl-score-api.fetchers.nhl-api-web.api.schedule :as schedule]
            [nhl-score-api.fetchers.nhl-api-web.api.standings :as standings]
            [nhl-score-api.fetchers.nhl-api-web.game-scores :as game-scores]
            [nhl-score-api.fetchers.nhl-api-web.roster-parser :as roster-parser]
            [nhl-score-api.fetchers.nhl-api-web.transformer :refer [get-games-in-date-range
                                                                    get-latest-games
                                                                    started-game?]]
            [nhl-score-api.logging :as logger]
            [nhl-score-api.utils :refer [format-date parse-date]]))

(defn get-current-schedule-date
  "Returns what is considered the current schedule date in UTC, which can be different from
   current actual date: the schedule date changes at midnight US/Pacific time (the earliest timezone
   in the NHL). By that time we consider the previous schedule date's games as at least started."
  [current-time]
  (let [adjusted-date-time (time/minus (time/to-time-zone current-time (time/time-zone-for-id "US/Pacific"))
                                       (time/hours 0))]
    (apply time/date-time (map #(% adjusted-date-time) [time/year time/month time/day]))))

(defn get-schedule-date-range-str-for-latest-scores []
  (let [current-date (get-current-schedule-date (time/now))]
    {:start (format-date (time/minus current-date (time/days 1)))
     :end (format-date current-date)}))

(defn get-current-standings-request-date
  "Returns the current standings date for games on the requested date."
  [{:keys [requested-date-str current-schedule-date-str regular-season-start-date-str regular-season-end-date-str]}]
  (let [current-regular-season-date-str (last (sort [current-schedule-date-str regular-season-start-date-str]))]
    (first (sort [requested-date-str current-regular-season-date-str regular-season-end-date-str]))))

(defn get-pre-game-standings-request-date [{:keys [current-standings-date-str
                                                   regular-season-start-date-str]}]
  (if (or (nil? current-standings-date-str)
          (= current-standings-date-str regular-season-start-date-str))
    nil
    (do (assert (<= (compare regular-season-start-date-str current-standings-date-str) 0)
                (str "Regular season start date " regular-season-start-date-str
                     " is later than current standings date " current-standings-date-str))
        (let [current-standings-date (parse-date current-standings-date-str)
              previous-date-str (format-date (time/minus current-standings-date (time/days 1)))]
          (last (sort [previous-date-str regular-season-start-date-str]))))))

(defn- fetch [api-request]
  (let [response (->> (api-request-queue/fetch
                       (api/url api-request)
                       {:debug false}
                       (api/description api-request))
                      :body
                      (api/transform api-request))
        response-schema (api/response-schema api-request)]
    (when-not (malli/validate response-schema response)
      (logger/warn (str "Response validation failed for " (api/description api-request) ": "
                        (malli-error/humanize (malli/explain response-schema response)))))
    response))

(defn- store-in-cache
  ([response api-request]
   (store-in-cache response api-request nil))
  ([response api-request context]
   (let [from-cache? (:from-cache? (meta response))
         unwrapped-response (:value response)
         cache-name (api/get-cache-with-context api-request unwrapped-response context)]
     (if (and (not from-cache?) cache-name)
       (do
         (cache/store cache-name (api/cache-key api-request) unwrapped-response)
         response)
       response))))

(defn- fetch-cached [api-request]
  (let [wrapped-response (-> (cache/get-cached (api/cache-key api-request) #(fetch api-request))
                             (store-in-cache api-request))
        unwrapped-value (:value wrapped-response)]
    (if (instance? clojure.lang.IObj unwrapped-value)
      (with-meta unwrapped-value (meta wrapped-response))
      unwrapped-value)))

(defn- fetch-games-info [date-range-str]
  (let [{:keys [start end]} (or date-range-str (get-schedule-date-range-str-for-latest-scores))]
    (fetch-cached (schedule/->ScheduleApiRequest start (when (< (compare start end) 0) end)))))

(defn- get-standings-date-strs [{:keys [current-schedule-date-str date-strs regular-season-start-date-str regular-season-end-date-str]}]
  (map #(let [standings-date-str
              (get-current-standings-request-date {:requested-date-str %
                                                   :current-schedule-date-str current-schedule-date-str
                                                   :regular-season-start-date-str regular-season-start-date-str
                                                   :regular-season-end-date-str regular-season-end-date-str})
              pre-game-standings-date-str
              (get-pre-game-standings-request-date {:current-standings-date-str standings-date-str
                                                    :regular-season-start-date-str regular-season-start-date-str})]
          {:current standings-date-str
           :pre-game pre-game-standings-date-str})
       date-strs))

(defn fetch-standings-infos [date-str-params games-info]
  (let [current-schedule-date-str (format-date (get-current-schedule-date (time/now)))
        standings-date-strs (get-standings-date-strs (assoc date-str-params :current-schedule-date-str current-schedule-date-str))
        unique-date-strs (->> standings-date-strs
                              (mapcat vals)
                              (filter some?)
                              set
                              sort)

        ; Prepare requests
        requests (map #(standings/->StandingsApiRequest {:current-schedule-date-str current-schedule-date-str
                                                         :standings-date-str %}
                                                        games-info)
                      unique-date-strs)
        request-by-date (zipmap unique-date-strs requests)

        ; Fetch needed standings
        response-by-date (zipmap unique-date-strs (pmap #(fetch-cached %) requests))

        ; Store responses in cache with pre-game standings as context
        standings-by-date (zipmap unique-date-strs
                                  (map-indexed
                                   (fn [idx date-str]
                                     (let [pre-game-date-str
                                           (when (> idx 0)
                                             (nth unique-date-strs (dec idx)))
                                           pre-game-response
                                           (when pre-game-date-str
                                             (store-in-cache (get response-by-date pre-game-date-str)
                                                             (get request-by-date pre-game-date-str)))]
                                       (->> (store-in-cache (get response-by-date date-str)
                                                            (get request-by-date date-str)
                                                            pre-game-response)
                                            (:standings))))
                                   unique-date-strs))]
    (map (fn [{:keys [current pre-game]}]
           (when current
             {:pre-game (get standings-by-date pre-game)
              :current (get standings-by-date current)}))
         standings-date-strs)))

(defn get-gamecenter-game-ids [schedule-games]
  (->> schedule-games
       (filter started-game?)
       (map #(:id %))))

(defn get-gamecenter [landing right-rail]
  (merge landing right-rail))

(defn get-rosters-url [gamecenter]
  (get-in gamecenter [:game-reports :rosters]))

(defn- fetch-and-parse-rosters [rosters-url game-id]
  (when rosters-url
    (when-let [html (fetch-cached (rosters/->RostersApiRequest rosters-url game-id))]
      (roster-parser/parse-roster-html html))))

(defn- fetch-rosters [gamecenter game-id]
  (when-let [rosters-url (get-rosters-url gamecenter)]
    (fetch-and-parse-rosters rosters-url game-id)))

(defn- fetch-gamecenters [schedule-games include-rosters?]
  (->> schedule-games
       (get-gamecenter-game-ids)
       (pmap (fn [game-id]
               (let [gamecenter (get-gamecenter (fetch-cached (landing/->LandingApiRequest game-id))
                                                (fetch-cached (right-rail/->RightRailApiRequest game-id)))
                     rosters (when include-rosters? (fetch-rosters gamecenter game-id))]
                 [game-id {:gamecenter gamecenter
                           :rosters rosters}])))
       (into {})))

(defn- prune-cache-and-fetch-gamecenters [games-info date-and-schedule-games include-rosters?]
  (when-not (:from-cache? (meta games-info))
    (logger/debug (str "Evicting "
                       (get-in date-and-schedule-games [:date :raw] "<no date>")
                       " landings and right-rails from :short-lived"))
    (cache/evict-from-short-lived! (->> (:games date-and-schedule-games)
                                        (map (fn [game] [(api/cache-key (landing/->LandingApiRequest (:id game)))
                                                         (api/cache-key (right-rail/->RightRailApiRequest (:id game)))]))
                                        flatten)))
  (fetch-gamecenters (:games date-and-schedule-games) include-rosters?))

(defn fetch-latest-scores [include-rosters?]
  (let [latest-games-info (fetch-games-info nil)
        date-and-schedule-games (get-latest-games latest-games-info)
        standings-date-str (if (= (count (:games date-and-schedule-games)) 0)
                             nil
                             (:raw (:date date-and-schedule-games)))
        standings-info (first
                        (fetch-standings-infos {:date-strs [standings-date-str]
                                                :regular-season-start-date-str (:regular-season-start-date latest-games-info)
                                                :regular-season-end-date-str (:regular-season-end-date latest-games-info)}
                                               latest-games-info))
        gamecenters-and-rosters (prune-cache-and-fetch-gamecenters latest-games-info date-and-schedule-games include-rosters?)
        gamecenters (into {} (map (fn [[game-id data]] [game-id (:gamecenter data)]) gamecenters-and-rosters))
        rosters (into {} (map (fn [[game-id data]] [game-id (:rosters data)]) gamecenters-and-rosters))]
    (->> (game-scores/parse-game-scores date-and-schedule-games standings-info gamecenters rosters)
         cache/log-cache-sizes!)))

(defn fetch-scores-in-date-range [start-date end-date include-rosters?]
  (let [games-info (fetch-games-info {:start (format-date start-date) :end (format-date end-date)})
        dates-and-schedule-games (get-games-in-date-range games-info start-date end-date)
        standings-date-strs (map #(if (= (count (:games %)) 0)
                                    nil
                                    (:raw (:date %)))
                                 dates-and-schedule-games)
        standings-infos (fetch-standings-infos {:date-strs standings-date-strs
                                                :regular-season-start-date-str (:regular-season-start-date games-info)
                                                :regular-season-end-date-str (:regular-season-end-date games-info)}
                                               games-info)]
    (->
     (doall (map-indexed (fn [index date-and-schedule-games]
                           (let [gamecenters-and-rosters (prune-cache-and-fetch-gamecenters games-info date-and-schedule-games include-rosters?)
                                 gamecenters (into {} (map (fn [[game-id data]] [game-id (:gamecenter data)]) gamecenters-and-rosters))
                                 rosters (into {} (map (fn [[game-id data]] [game-id (:rosters data)]) gamecenters-and-rosters))]
                             (game-scores/parse-game-scores date-and-schedule-games (nth standings-infos index) gamecenters rosters)))
                         dates-and-schedule-games))
     cache/log-cache-sizes!)))
