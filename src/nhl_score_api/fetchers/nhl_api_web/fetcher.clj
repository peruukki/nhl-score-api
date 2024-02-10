(ns nhl-score-api.fetchers.nhl-api-web.fetcher
  (:require [nhl-score-api.cache :as cache]
            [nhl-score-api.fetchers.nhl-api-web.game-scores :as game-scores]
            [nhl-score-api.fetchers.nhl-api-web.transformer :refer [get-games-in-date-range get-latest-games started-game?]]
            [nhl-score-api.utils :refer [format-date]]
            [clojure.data.json :as json]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clj-time.core :as time]
            [clj-http.client :as http]))

(def base-url "https://api-web.nhle.com/v1")

(defn- get-schedule-url [date-str] (str base-url "/schedule/" date-str))
(defn- get-standings-url [date-str] (str base-url "/standings/" date-str))
(defn- get-landing-url [game-id] (str base-url "/gamecenter/" game-id "/landing"))

(def mocked-latest-games-info-file (System/getenv "MOCK_NHL_API_WEB"))

(defn get-schedule-start-date [start-date]
  (let [fetch-latest? (nil? start-date)
        date-now (time/now)]
    (format-date (if fetch-latest? (time/minus date-now (time/days 1)) start-date))))

(defn get-standings-request-date [{:keys [requested-date-str current-date-str regular-season-end-date-str]}]
  (first (sort [requested-date-str current-date-str regular-season-end-date-str])))

(defn api-response-to-json [api-response]
  (json/read-str api-response :key-fn ->kebab-case-keyword))

(defn- fetch [endpoint-name endpoint-params url]
  (println "Fetching" endpoint-name endpoint-params)
  (let [start-time (System/currentTimeMillis)
        response (-> (http/get url {:debug false})
                     :body
                     api-response-to-json)]
    (println "Fetched" endpoint-name endpoint-params "(took" (- (System/currentTimeMillis) start-time) "ms)")
    response))

(defn- fetch-games-info [date-str]
  (let [start-date (get-schedule-start-date date-str)]
    (fetch "schedule" {:date start-date} (get-schedule-url start-date))))

(defn fetch-standings-info [date-str regular-season-end-date-str]
  (let [standings-date-str (if (nil? date-str)
                             nil
                             (get-standings-request-date {:requested-date-str date-str
                                                          :current-date-str (format-date (time/now))
                                                          :regular-season-end-date-str regular-season-end-date-str}))]
    (if (nil? standings-date-str)
      {:records nil}
      (fetch "standings" {:date standings-date-str} (get-standings-url standings-date-str)))))

(defn get-landing-urls-by-game-id [schedule-games]
  (->> schedule-games
       (filter started-game?)
       (map (fn [schedule-game] [(:id schedule-game) (get-landing-url (:id schedule-game))]))
       (into {})))

(defn fetch-landings-info [schedule-games]
  (->> schedule-games
       (get-landing-urls-by-game-id)
       (map (fn [[id url]] [id (fetch "landing" {:id id} url)]))
       (into {})))

(defn- fetch-latest-scores []
  (let [latest-games-info
        (if mocked-latest-games-info-file
          (do (println "Using mocked NHL Stats API response from" mocked-latest-games-info-file)
              (api-response-to-json (slurp mocked-latest-games-info-file)))
          (fetch-games-info nil))
        date-and-schedule-games (get-latest-games latest-games-info)
        regular-season-end-date-str (:regular-season-end-date latest-games-info)
        standings-info (fetch-standings-info (:raw (:date date-and-schedule-games)) regular-season-end-date-str)
        landings-info (fetch-landings-info (:games date-and-schedule-games))]
    (game-scores/parse-game-scores date-and-schedule-games (:standings standings-info) landings-info)))

(def fetch-latest-scores-cached
  (cache/get-cached-fn fetch-latest-scores "fetch-latest-scores" 60000))

(defn- fetch-scores-in-date-range [start-date end-date]
  (let [games-info (fetch-games-info start-date)
        dates-and-schedule-games (get-games-in-date-range games-info start-date end-date)
        regular-season-end-date-str (:regular-season-end-date games-info)
        standings-infos (map #(fetch-standings-info (:raw (:date %)) regular-season-end-date-str) dates-and-schedule-games)
        landings-infos (map #(fetch-landings-info (:games %)) dates-and-schedule-games)]
    (map-indexed #(game-scores/parse-game-scores %2 (:standings (nth standings-infos %1)) (nth landings-infos %1))
                 dates-and-schedule-games)))

(def fetch-scores-in-date-range-cached
  (cache/get-cached-fn fetch-scores-in-date-range "fetch-scores-in-date-range" 60000))
