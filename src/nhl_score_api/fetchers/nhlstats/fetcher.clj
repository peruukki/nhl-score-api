(ns nhl-score-api.fetchers.nhlstats.fetcher
  (:require [nhl-score-api.fetchers.nhlstats.game-scores :as game-scores]
            [nhl-score-api.fetchers.nhlstats.transformer :refer [get-games get-latest-games]]
            [clojure.data.json :as json]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [clj-http.lite.client :as http])) ; clj-http-lite supports SNI (unlike http-kit or clj-http)

(def base-url "https://statsapi.web.nhl.com/api/v1")
(def scores-url (str base-url "/schedule"))
(def standings-url (str base-url "/standings"))

(def mocked-latest-games-info-file (System/getenv "MOCK_NHL_STATS_API"))

(defn- format-date [date]
  (format/unparse (format/formatters :year-month-day) date))

(defn get-schedule-query-params [start-date end-date]
  (let [fetch-latest? (and (nil? start-date) (nil? end-date))
        date-now (time/date-time 2021 7 8)
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

(defn fetch-latest-scores []
  (let [latest-games-info
        (if mocked-latest-games-info-file
          (do (println "Using mocked NHL Stats API response from" mocked-latest-games-info-file)
              (api-response-to-json (slurp mocked-latest-games-info-file)))
          (fetch-games-info nil nil))
        date-and-api-games (get-latest-games latest-games-info)
        standings-info (fetch-standings-info (:raw (:date date-and-api-games)))]
    (game-scores/parse-game-scores date-and-api-games (:records standings-info))))

(defn fetch-scores-in-date-range [start-date end-date]
  (let [games-info (fetch-games-info start-date end-date)
        dates-and-api-games (get-games games-info)
        standings-info (fetch-standings-info (:raw (:date (first dates-and-api-games))))]
    (map #(game-scores/parse-game-scores % (:records standings-info) false) dates-and-api-games)))
