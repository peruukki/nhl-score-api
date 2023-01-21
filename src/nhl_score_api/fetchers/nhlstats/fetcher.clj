(ns nhl-score-api.fetchers.nhlstats.fetcher
  (:require [nhl-score-api.cache :as cache]
            [nhl-score-api.fetchers.nhlstats.game-scores :as game-scores]
            [nhl-score-api.fetchers.nhlstats.transformer :refer [get-games get-latest-games started-game?]]
            [nhl-score-api.utils :refer [format-date]]
            [clojure.data.json :as json]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clj-time.core :as time]
            [clj-http.lite.client :as http])) ; clj-http-lite supports SNI (unlike http-kit or clj-http)

(def base-url "https://statsapi.web.nhl.com/api/v1")
(def scores-url (str base-url "/schedule"))
(def standings-url (str base-url "/standings"))
(defn- get-boxscore-url [game-id] (str base-url "/game/" game-id "/boxscore"))

(def mocked-latest-games-info-file (System/getenv "MOCK_NHL_STATS_API"))

(defn get-schedule-query-params [start-date end-date]
  (let [fetch-latest? (and (nil? start-date) (nil? end-date))
        date-now (time/now)
        query-start-date (format-date (if fetch-latest? (time/minus date-now (time/days 1)) start-date))
        query-end-date (format-date (if fetch-latest? date-now (if (nil? end-date) start-date end-date)))]
    {:startDate query-start-date
     :endDate query-end-date
     :expand "schedule.teams,schedule.scoringplays,schedule.game.seriesSummary,seriesSummary.series,schedule.linescore"}))

(defn api-response-to-json [api-response]
  (json/read-str api-response :key-fn ->kebab-case-keyword))

(defn- fetch-games-info [start-date end-date]
  (api-response-to-json (:body (http/get scores-url {:query-params (get-schedule-query-params start-date end-date)}))))

(defn fetch-standings-info [latest-games-date-str]
  (if (nil? latest-games-date-str)
    {:records nil}
    (api-response-to-json (:body (http/get standings-url)))))

(defn get-boxscore-urls-by-game-pk [api-games]
  (->> api-games
       (filter started-game?)
       (map (fn [api-game] [(:game-pk api-game) (get-boxscore-url (:game-pk api-game))]))
       (into {})))

(defn fetch-boxscores-info [api-games]
  (->> api-games
       (get-boxscore-urls-by-game-pk)
       (map (fn [pk-and-url] [(first pk-and-url) (api-response-to-json (:body (http/get (second pk-and-url))))]))
       (into {})))

(defn- fetch-latest-scores []
  (let [latest-games-info
        (if mocked-latest-games-info-file
          (do (println "Using mocked NHL Stats API response from" mocked-latest-games-info-file)
              (api-response-to-json (slurp mocked-latest-games-info-file)))
          (fetch-games-info nil nil))
        date-and-api-games (get-latest-games latest-games-info)
        standings-info (fetch-standings-info (:raw (:date date-and-api-games)))
        boxscores-info (fetch-boxscores-info (:games date-and-api-games))]
    (game-scores/parse-game-scores date-and-api-games (:records standings-info) boxscores-info)))

(def fetch-latest-scores-cached
  (cache/get-cached-fn fetch-latest-scores "fetch-latest-scores" 60000))

(defn- fetch-scores-in-date-range [start-date end-date]
  (let [games-info (fetch-games-info start-date end-date)
        dates-and-api-games (get-games games-info)
        standings-info (fetch-standings-info (:raw (:date (first dates-and-api-games))))
        boxscores-infos (map #(fetch-boxscores-info (:games %)) dates-and-api-games)]
    (map-indexed #(game-scores/parse-game-scores %2 (:records standings-info) (nth boxscores-infos %1) false)
                 dates-and-api-games)))

(def fetch-scores-in-date-range-cached
  (cache/get-cached-fn fetch-scores-in-date-range "fetch-scores-in-date-range" 60000))
