(ns nhl-score-api.fetchers.nhl-api-web.fetcher
  (:require [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clj-http.client :as http]
            [clj-time.core :as time]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [nhl-score-api.cache :as cache]
            [nhl-score-api.fetchers.nhl-api-web.game-scores :as game-scores]
            [nhl-score-api.fetchers.nhl-api-web.transformer :refer [get-games-in-date-range
                                                                    get-latest-games
                                                                    started-game?]]
            [nhl-score-api.utils :refer [format-date parse-date]]))

(defn- log-cache-sizes!
  "Logs all cache sizes and returns passed response"
  [response]
  (println "Cache sizes:" (str/join ", "
                                    (map (fn [[id cache]] (str id " " (count @cache))) cache/caches)))
  response)

(def base-url "https://api-web.nhle.com/v1")

(defprotocol ApiRequest
  (archive? [_ response])
  (description [_])
  (url [_]))

(defrecord LandingApiRequest [game-id]
  ApiRequest
  (archive? [_ response] (= "OFF" (:game-state response)))
  (description [_] (str "landing " {:game-id game-id}))
  (url [_] (str base-url "/gamecenter/" game-id "/landing")))

(defrecord ScheduleApiRequest [date-str]
  ApiRequest
  (archive? [_ _] false)
  (description [_] (str "schedule " {:date date-str}))
  (url [_] (str base-url "/schedule/" date-str)))

(defrecord StandingsApiRequest [date-str]
  ApiRequest
  (archive? [_ _] false)
  (description [_] (str "standings " {:date date-str}))
  (url [_] (str base-url "/standings/" date-str)))

(def mocked-latest-games-info-file (System/getenv "MOCK_NHL_API_WEB"))

(defn get-schedule-start-date [start-date]
  (let [fetch-latest? (nil? start-date)
        date-now (time/now)]
    (format-date (if fetch-latest? (time/minus date-now (time/days 1)) start-date))))

(defn get-current-standings-request-date [{:keys [requested-date-str
                                                  current-date-str
                                                  regular-season-end-date-str]}]
  (first (sort [requested-date-str current-date-str regular-season-end-date-str])))

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
  (println "Fetching" (description api-request))
  (let [start-time (System/currentTimeMillis)
        response (-> (http/get (url api-request) {:debug false})
                     :body
                     api-response-to-json)]
    (println "Fetched " (description api-request) "(took" (- (System/currentTimeMillis) start-time) "ms)")
    response))

(defn- fetch-cached [api-request]
  (let [response (cache/get-cached (url api-request) #(fetch api-request))
        from-cache? (:from-cache? (meta response))]
    (if (and (not from-cache?) (archive? api-request response))
      (cache/archive (url api-request) response)
      response)))

(defn- fetch-games-info [date-str]
  (let [start-date (get-schedule-start-date date-str)]
    (fetch-cached (ScheduleApiRequest. start-date))))

(defn- get-standings-date-strs [{:keys [date-strs regular-season-start-date-str regular-season-end-date-str]}]
  (let [current-date-str (format-date (time/now))]
    (map #(let [standings-date-str
                (get-current-standings-request-date {:requested-date-str %
                                                     :current-date-str current-date-str
                                                     :regular-season-end-date-str regular-season-end-date-str})
                pre-game-standings-date-str
                (get-pre-game-standings-request-date {:current-standings-date-str standings-date-str
                                                      :regular-season-start-date-str regular-season-start-date-str})]
            {:current standings-date-str
             :pre-game pre-game-standings-date-str})
         date-strs)))

(defn fetch-standings-infos [date-str-params]
  (let [standings-date-strs (get-standings-date-strs date-str-params)
        unique-date-strs (->> standings-date-strs
                              (map vals)
                              flatten
                              set)
        standings-per-unique-date-str (map #(if (nil? %)
                                              nil
                                              (:standings (fetch-cached (StandingsApiRequest. %)))) unique-date-strs)
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
       (map #(vector % (fetch-cached (LandingApiRequest. %))))
       (into {})))

(defn fetch-latest-scores []
  (let [latest-games-info
        (if mocked-latest-games-info-file
          (do (println "Using mocked NHL Stats API response from" mocked-latest-games-info-file)
              (api-response-to-json (slurp mocked-latest-games-info-file)))
          (fetch-games-info nil))
        date-and-schedule-games (get-latest-games latest-games-info)
        standings-date-str (if (= (count (:games date-and-schedule-games)) 0)
                             nil
                             (:raw (:date date-and-schedule-games)))
        standings-info (first
                        (fetch-standings-infos {:date-strs [standings-date-str]
                                                :regular-season-start-date-str (:regular-season-start-date latest-games-info)
                                                :regular-season-end-date-str (:regular-season-end-date latest-games-info)}))
        landings-info (fetch-landings-info (:games date-and-schedule-games))]
    (->
     (game-scores/parse-game-scores date-and-schedule-games standings-info landings-info)
     log-cache-sizes!)))

(defn fetch-scores-in-date-range [start-date end-date]
  (let [games-info (fetch-games-info start-date)
        dates-and-schedule-games (get-games-in-date-range games-info start-date end-date)
        standings-date-strs (map #(if (= (count (:games %)) 0)
                                    nil
                                    (:raw (:date %)))
                                 dates-and-schedule-games)
        standings-infos (fetch-standings-infos {:date-strs standings-date-strs
                                                :regular-season-start-date-str (:regular-season-start-date games-info)
                                                :regular-season-end-date-str (:regular-season-end-date games-info)})
        landings-infos (map #(fetch-landings-info (:games %)) dates-and-schedule-games)]
    (->
     (doall (map-indexed #(game-scores/parse-game-scores %2 (nth standings-infos %1) (nth landings-infos %1))
                         dates-and-schedule-games))
     log-cache-sizes!)))
