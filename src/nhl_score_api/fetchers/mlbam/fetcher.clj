(ns nhl-score-api.fetchers.mlbam.fetcher
  (:require [nhl-score-api.fetchers.mlbam.game-scores :as game-scores]
            [nhl-score-api.fetchers.mlbam.latest-games :as latest-games]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [clj-http.lite.client :as http])) ; clj-http-lite supports SNI (unlike http-kit or clj-http)

(def scores-url "https://statsapi.web.nhl.com/api/v1/schedule")

(def mocked-latest-games-info-file (System/getenv "MOCK_MLBAM_API"))

(defn- format-date [date]
  (format/unparse (format/formatters :year-month-day) date))

(defn get-query-params []
  (let [date (time/date-time 2018 6 8)
        start-date (format-date (time/minus date (time/days 1)))
        end-date (format-date date)]
    {:startDate start-date
     :endDate end-date
     :expand "schedule.teams,schedule.scoringplays,schedule.game.seriesSummary,seriesSummary.series,schedule.linescore"}))

(defn- fetch-latest-games-info []
  (:body (http/get scores-url {:query-params (get-query-params)})))

(defn fetch-latest-scores []
  (let [latest-games-info
        (if mocked-latest-games-info-file
          (do (println "Using mocked MLBAM API response from" mocked-latest-games-info-file)
              (slurp mocked-latest-games-info-file))
          (fetch-latest-games-info))]
    (-> latest-games-info
        latest-games/filter-latest-games
        game-scores/parse-game-scores)))
