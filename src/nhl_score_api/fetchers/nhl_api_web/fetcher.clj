(ns nhl-score-api.fetchers.nhl-api-web.fetcher
  (:require [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clj-time.core :as time]
            [clojure.data.json :as json]
            [malli.core :as malli]
            [malli.error :as malli-error]
            [nhl-score-api.cache :as cache]
            [nhl-score-api.fetchers.nhl-api-web.api-request-queue :as api-request-queue]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.landing :as landing]
            [nhl-score-api.fetchers.nhl-api-web.api.right-rail :as right-rail]
            [nhl-score-api.fetchers.nhl-api-web.api.roster :as roster]
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

(defn api-response-to-json [api-response]
  (json/read-str api-response :key-fn ->kebab-case-keyword))

(defn- fetch [api-request]
  (let [response (-> (api-request-queue/fetch
                      (api/url api-request)
                      {:debug false}
                      (api/description api-request))
                     :body
                     api-response-to-json)
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
         cache-name (api/get-cache-with-context api-request response context)]
     (if (and (not from-cache?) cache-name)
       (cache/store cache-name (api/cache-key api-request) response)
       response))))

(defn- fetch-cached [api-request]
  (-> (cache/get-cached (api/cache-key api-request) #(fetch api-request))
      (store-in-cache api-request)))

(defn- fetch-html-cached
  "Fetches HTML content from URL and caches it.
   Returns the HTML content as a string."
  [cache-key url description]
  (cache/get-cached cache-key
                    (fn []
                      (try
                        (let [response (api-request-queue/fetch url {:debug false} description)]
                          (:body response))
                        (catch Exception e
                          (logger/warn (str "Failed to fetch HTML from " url ": " (.getMessage e)))
                          nil)))))

(defn- fetch-and-enrich-roster-for-game
  "Fetches roster HTML for a game, parses it, and enriches with API roster data.
   Returns enriched roster data or nil if roster URL is missing or parsing fails.

   gamecenter should contain the right-rail data with :rosters URL.
   team-rosters should be a map of {[team-abbrev season] roster-data}."
  [game-id gamecenter team-rosters schedule-game]
  (when-let [roster-url (get-in gamecenter [:game-reports :rosters])]
    (try
      (let [roster-html (fetch-html-cached (str "roster-html-" game-id) roster-url (str "roster HTML " game-id))
            html-roster (when roster-html (roster-parser/parse-roster-html roster-html))]
        (when html-roster
          (let [season (str (:season schedule-game))
                away-abbrev (get-in schedule-game [:away-team :abbrev])
                home-abbrev (get-in schedule-game [:home-team :abbrev])
                api-roster-away (get team-rosters [away-abbrev season])
                api-roster-home (get team-rosters [home-abbrev season])]
            (roster-parser/enrich-roster-with-api-data html-roster
                                                       (or api-roster-away {:forwards [] :defensemen [] :goalies []})
                                                       (or api-roster-home {:forwards [] :defensemen [] :goalies []})))))
      (catch Exception e
        (logger/warn (str "Failed to parse roster HTML for game " game-id ": " (.getMessage e)))
        nil))))

(defn- fetch-and-enrich-rosters-for-games
  "Fetches and enriches roster data for multiple games.
   Returns a map of {game-id enriched-roster-data}."
  [gamecenters team-rosters schedule-games]
  (let [schedule-games-by-id (into {} (map (fn [game] [(:id game) game]) schedule-games))]
    (->> gamecenters
         (pmap (fn [[game-id gamecenter]]
                 [game-id (fetch-and-enrich-roster-for-game game-id
                                                            gamecenter
                                                            team-rosters
                                                            (get schedule-games-by-id game-id))]))
         (filter (fn [[_game-id roster-data]] (some? roster-data)))
         (into {}))))

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

(defn- fetch-gamecenters [schedule-games]
  (->> schedule-games
       (get-gamecenter-game-ids)
       (pmap #(vector % (get-gamecenter (fetch-cached (landing/->LandingApiRequest %))
                                        (fetch-cached (right-rail/->RightRailApiRequest %)))))
       (into {})))

(defn- fetch-team-rosters-for-games
  "Fetches team roster API data for all unique team/season combinations in the games.
   Returns a map of {[team-abbrev season] roster-data}."
  [schedule-games]
  (let [team-seasons (->> schedule-games
                          (mapcat (fn [game]
                                    (let [season (str (:season game))
                                          away-abbrev (get-in game [:away-team :abbrev])
                                          home-abbrev (get-in game [:home-team :abbrev])]
                                      [[away-abbrev season] [home-abbrev season]])))
                          set)
        requests (map (fn [[team-abbrev season]]
                        [team-abbrev season (roster/->RosterApiRequest team-abbrev season)])
                      team-seasons)
        responses (pmap (fn [[team-abbrev season request]]
                          [team-abbrev season (fetch-cached request)])
                        requests)]
    (into {} (map (fn [[team-abbrev season response]]
                    [[team-abbrev season] response])
                  responses))))

(defn- prune-cache-and-fetch-gamecenters [games-info date-and-schedule-games]
  (when-not (:from-cache? (meta games-info))
    (logger/debug (str "Evicting "
                       (get-in date-and-schedule-games [:date :raw] "<no date>")
                       " landings and right-rails from :short-lived"))
    (cache/evict-from-short-lived! (->> (:games date-and-schedule-games)
                                        (map (fn [game] [(api/cache-key (landing/->LandingApiRequest (:id game)))
                                                         (api/cache-key (right-rail/->RightRailApiRequest (:id game)))]))
                                        flatten)))
  (fetch-gamecenters (:games date-and-schedule-games)))

(defn fetch-latest-scores
  ([]
   (fetch-latest-scores false))
  ([include-rosters]
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
         team-rosters (when include-rosters
                        (fetch-team-rosters-for-games (:games date-and-schedule-games)))
         gamecenters (prune-cache-and-fetch-gamecenters latest-games-info date-and-schedule-games)
         enriched-rosters (when include-rosters
                            (fetch-and-enrich-rosters-for-games gamecenters team-rosters (:games date-and-schedule-games)))]
     (->> (game-scores/parse-game-scores date-and-schedule-games standings-info gamecenters team-rosters enriched-rosters)
          cache/log-cache-sizes!))))

(defn fetch-scores-in-date-range
  ([start-date end-date]
   (fetch-scores-in-date-range start-date end-date false))
  ([start-date end-date include-rosters]
   (let [games-info (fetch-games-info {:start (format-date start-date) :end (format-date end-date)})
         dates-and-schedule-games (get-games-in-date-range games-info start-date end-date)
         standings-date-strs (map #(if (= (count (:games %)) 0)
                                     nil
                                     (:raw (:date %)))
                                  dates-and-schedule-games)
         standings-infos (fetch-standings-infos {:date-strs standings-date-strs
                                                 :regular-season-start-date-str (:regular-season-start-date games-info)
                                                 :regular-season-end-date-str (:regular-season-end-date games-info)}
                                                games-info)
         all-games (mapcat :games dates-and-schedule-games)
         team-rosters (when include-rosters
                        (fetch-team-rosters-for-games all-games))
         enriched-rosters-by-date (when include-rosters
                                    (map-indexed (fn [_index date-and-schedule-games]
                                                   (let [gamecenters (prune-cache-and-fetch-gamecenters games-info date-and-schedule-games)]
                                                     (fetch-and-enrich-rosters-for-games gamecenters team-rosters (:games date-and-schedule-games))))
                                                 dates-and-schedule-games))]
     (->
      (doall (map-indexed (fn [index date-and-schedule-games]
                            (let [gamecenters (prune-cache-and-fetch-gamecenters games-info date-and-schedule-games)
                                  enriched-rosters (when include-rosters (nth enriched-rosters-by-date index))]
                              (game-scores/parse-game-scores date-and-schedule-games (nth standings-infos index) gamecenters team-rosters enriched-rosters)))
                          dates-and-schedule-games))
      cache/log-cache-sizes!))))
