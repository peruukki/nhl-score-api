(ns nhl-score-api.fetchers.nhl-api-web.fetcher
  (:require [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clj-http.client :as http]
            [clj-time.core :as time]
            [clojure.data.json :as json]
            [nhl-score-api.cache :as cache]
            [nhl-score-api.fetchers.nhl-api-web.api :as api]
            [nhl-score-api.fetchers.nhl-api-web.game-scores :as game-scores]
            [nhl-score-api.fetchers.nhl-api-web.transformer :refer [get-games-in-date-range
                                                                    get-latest-games
                                                                    started-game?]]
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

(defn get-current-standings-request-date [{:keys [requested-date-str
                                                  current-schedule-date-str
                                                  regular-season-end-date-str]}]
  (first (sort [requested-date-str current-schedule-date-str regular-season-end-date-str])))

(defn get-pre-game-standings-request-date [{:keys [current-standings-date-str
                                                   regular-season-start-date-str]}]
  (if (nil? current-standings-date-str)
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
  (println "Fetching" (api/description api-request))
  (let [start-time (System/currentTimeMillis)
        response (-> (http/get (api/url api-request) {:debug false})
                     :body
                     api-response-to-json)]
    (println "Fetched " (api/description api-request) "(took" (- (System/currentTimeMillis) start-time) "ms)")
    response))

(defn- fetch-cached [api-request]
  (let [response (cache/get-cached (api/cache-key api-request) #(fetch api-request))
        from-cache? (:from-cache? (meta response))]
    (if (and (not from-cache?) (api/archive? api-request response))
      (cache/archive (api/cache-key api-request) response)
      response)))

(defn- fetch-games-info [date-range-str]
  (let [{:keys [start end]} (or date-range-str (get-schedule-date-range-str-for-latest-scores))]
    (fetch-cached (api/->ScheduleApiRequest start (when (< (compare start end) 0) end)))))

(defn- get-standings-date-strs [{:keys [current-schedule-date-str date-strs regular-season-start-date-str regular-season-end-date-str]}]
  (map #(let [standings-date-str
              (get-current-standings-request-date {:requested-date-str %
                                                   :current-schedule-date-str current-schedule-date-str
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
                              (map vals)
                              flatten
                              set
                              sort)
        date-str-pairs (map-indexed (fn [index date-str] {:pre-game-date-str (if (= index 0) nil (nth unique-date-strs (dec index)))
                                                          :current-date-str date-str})
                                    unique-date-strs)
        standings-per-unique-date-str (map (fn [{:keys [pre-game-date-str current-date-str]}]
                                             (when current-date-str
                                               (->> (if pre-game-date-str
                                                      (fetch-cached (api/->StandingsApiRequest pre-game-date-str games-info nil))
                                                      nil)
                                                    (api/->StandingsApiRequest current-date-str games-info)
                                                    fetch-cached
                                                    (:standings))))
                                           date-str-pairs)
        standings-by-date-str (zipmap unique-date-strs standings-per-unique-date-str)]
    (map #(if (nil? (:current %))
            nil
            (hash-map :pre-game (get standings-by-date-str (:pre-game %))
                      :current (get standings-by-date-str (:current %))))
         standings-date-strs)))

(defn get-landing-game-ids [schedule-games]
  (->> schedule-games
       (filter started-game?)
       (map #(:id %))))

(defn fetch-landings-info [schedule-games]
  (->> schedule-games
       (get-landing-game-ids)
       (map #(vector % (fetch-cached (api/->LandingApiRequest %))))
       (into {})))

(defn- prune-cache-and-fetch-landings-info [games-info date-and-schedule-games]
  (when-not (:from-cache? (meta games-info))
    (println "Evicting" (:raw (:date date-and-schedule-games)) "landings from :short-lived")
    (cache/evict-from-short-lived! (map #(api/cache-key (api/->LandingApiRequest (:id %)))
                                        (:games date-and-schedule-games))))
  (fetch-landings-info (:games date-and-schedule-games)))

(defn fetch-latest-scores []
  (let [latest-games-info (fetch-games-info nil)
        date-and-schedule-games (get-latest-games latest-games-info)
        standings-date-str (if (= (count (:games date-and-schedule-games)) 0)
                             nil
                             (:raw (:date date-and-schedule-games)))
        standings-info (first
                        (fetch-standings-infos {:date-strs [standings-date-str]
                                                :regular-season-start-date-str (:regular-season-start-date latest-games-info)
                                                :regular-season-end-date-str (:regular-season-end-date latest-games-info)}
                                               latest-games-info))]
    (->> date-and-schedule-games
         (prune-cache-and-fetch-landings-info latest-games-info)
         (game-scores/parse-game-scores date-and-schedule-games standings-info)
         cache/log-cache-sizes!)))

(defn fetch-scores-in-date-range [start-date end-date]
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
                           (->> date-and-schedule-games
                                (prune-cache-and-fetch-landings-info games-info)
                                (game-scores/parse-game-scores date-and-schedule-games (nth standings-infos index))))
                         dates-and-schedule-games))
     cache/log-cache-sizes!)))
