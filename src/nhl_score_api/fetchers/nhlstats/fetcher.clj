(ns nhl-score-api.fetchers.nhlstats.fetcher
  (:require [nhl-score-api.fetchers.nhlstats.game-scores :as game-scores]
            [nhl-score-api.fetchers.nhlstats.latest-games :as latest-games]
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

(defn get-schedule-query-params []
  (let [date (time/date-time 2019 6 12)
        start-date (format-date (time/minus date (time/days 1)))
        end-date (format-date date)]
    {:startDate start-date
     :endDate end-date
     :expand "schedule.teams,schedule.scoringplays,schedule.game.seriesSummary,seriesSummary.series,schedule.linescore"}))

(defn get-standings-query-params [latest-games-date-str]
  (let [latest-games-date (format/parse latest-games-date-str)
        standings-date (format-date (time/minus latest-games-date (time/days 1)))]
    {:date standings-date}))

(defn api-response-to-json [api-response]
  (json/read-str api-response :key-fn ->kebab-case-keyword))

(defn- fetch-latest-games-info []
  (api-response-to-json (:body (http/get scores-url {:query-params (get-schedule-query-params)}))))

(defn fetch-standings-info [latest-games-date-str]
  (if (nil? latest-games-date-str)
    {:records nil}
    (api-response-to-json (:body (http/get standings-url {:query-params (get-standings-query-params latest-games-date-str)})))))

(defn fetch-latest-scores []
  (let [latest-games-info
        (if mocked-latest-games-info-file
          (do (println "Using mocked NHL Stats API response from" mocked-latest-games-info-file)
              (api-response-to-json (slurp mocked-latest-games-info-file)))
          (fetch-latest-games-info))
        date-and-api-games (latest-games/filter-latest-games latest-games-info)
        standings-info (fetch-standings-info (:raw (:date date-and-api-games)))]
    (game-scores/parse-game-scores date-and-api-games (:records standings-info))))
