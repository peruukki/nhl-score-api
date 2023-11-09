(ns nhl-score-api.fetchers.nhl-api-web.fetcher
  (:require [nhl-score-api.cache :as cache]
            [nhl-score-api.fetchers.nhl-api-web.game-scores :as game-scores]
            [nhl-score-api.fetchers.nhl-api-web.transformer :refer [get-games get-latest-games started-game?]]
            [nhl-score-api.utils :refer [format-date]]
            [clojure.data.json :as json]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [clj-http.lite.client :as http])) ; clj-http-lite supports SNI (unlike http-kit or clj-http)

(def base-url "https://api-web.nhle.com/v1")
(defn- get-schedule-url [date-str] (str base-url "/schedule/" date-str))
(defn- get-standings-url [date-str] (str base-url "/standings/" date-str))
(defn- get-landing-url [game-id] (str base-url "/gamecenter/" game-id "/landing"))

(def mocked-latest-games-info-file (System/getenv "MOCK_NHL_API_WEB"))

(defn get-schedule-start-date [start-date]
  (let [fetch-latest? (nil? start-date)
        date-now (time/now)]
    (format-date (if fetch-latest? (time/minus date-now (time/days 1)) start-date))))

(defn get-standings-query-params [date-str]
  (let [date (format/parse (format/formatters :year-month-day) date-str)
        year (time/year date)
        start-year (if (> (time/month date) 8) year (- year 1))]
    {:season (str start-year (+ start-year 1))}))

(defn api-response-to-json [api-response]
  (json/read-str api-response :key-fn ->kebab-case-keyword))

(defn- fetch-games-info [date-str]
  (api-response-to-json (:body (http/get (get-schedule-url (get-schedule-start-date date-str))))))

(defn fetch-standings-info [date-str]
  (if (nil? date-str)
    {:records nil}
    (api-response-to-json (:body (http/get (get-standings-url date-str))))))

(defn get-landing-urls-by-game-id [schedule-games]
  (->> schedule-games
       (filter started-game?)
       (map (fn [schedule-game] [(:id schedule-game) (get-landing-url (:id schedule-game))]))
       (into {})))

(defn fetch-landings-info [schedule-games]
  (->> schedule-games
       (get-landing-urls-by-game-id)
       (map (fn [pk-and-url] [(first pk-and-url) (api-response-to-json (:body (http/get (second pk-and-url))))]))
       (into {})))

(defn- fetch-latest-scores []
  (let [latest-games-info
        (if mocked-latest-games-info-file
          (do (println "Using mocked NHL Stats API response from" mocked-latest-games-info-file)
              (api-response-to-json (slurp mocked-latest-games-info-file)))
          (fetch-games-info nil))
        date-and-schedule-games (get-latest-games latest-games-info)
        standings-info (fetch-standings-info (:raw (:date date-and-schedule-games)))
        landings-info (fetch-landings-info (:games date-and-schedule-games))]
    (game-scores/parse-game-scores date-and-schedule-games (:standings standings-info) landings-info)))

(def fetch-latest-scores-cached
  (cache/get-cached-fn fetch-latest-scores "fetch-latest-scores" 60000))

(defn- fetch-scores-in-date-range [start-date end-date]
  (let [games-info (fetch-games-info start-date)
        dates-and-schedule-games (get-games games-info)
        standings-info (fetch-standings-info (:raw (:date (last dates-and-schedule-games))))
        landings-infos (map #(fetch-landings-info (:games %)) dates-and-schedule-games)]
    (map-indexed #(game-scores/parse-game-scores %2 (:standings standings-info) (nth landings-infos %1) false)
                 dates-and-schedule-games)))

(def fetch-scores-in-date-range-cached
  (cache/get-cached-fn fetch-scores-in-date-range "fetch-scores-in-date-range" 60000))