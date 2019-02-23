(ns nhl-score-api.fetchers.nhlstats.fetcher
  (:require [nhl-score-api.fetchers.nhlstats.game-scores :as game-scores]
            [nhl-score-api.fetchers.nhlstats.latest-games :as latest-games]
            [clojure.data.json :as json]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [clj-http.lite.client :as http])) ; clj-http-lite supports SNI (unlike http-kit or clj-http)

(def scores-url "https://statsapi.web.nhl.com/api/v1/schedule")

(def mocked-latest-games-info-file (System/getenv "MOCK_NHL_STATS_API"))

(defn- format-date [date]
  (format/unparse (format/formatters :year-month-day) date))

(defn get-query-params []
  (let [now (time/now)
        start-date (format-date (time/minus now (time/days 1)))
        end-date (format-date now)]
    {:startDate start-date
     :endDate end-date
     :expand "schedule.teams,schedule.scoringplays,schedule.game.seriesSummary,seriesSummary.series,schedule.linescore"}))

(defn api-response-to-json [api-response]
  (json/read-str api-response :key-fn ->kebab-case-keyword))

(defn- fetch-latest-games-info []
  (api-response-to-json (:body (http/get scores-url {:query-params (get-query-params)}))))

(defn fetch-latest-scores []
  (let [latest-games-info
        (if mocked-latest-games-info-file
          (do (println "Using mocked NHL Stats API response from" mocked-latest-games-info-file)
              (api-response-to-json (slurp mocked-latest-games-info-file)))
          (fetch-latest-games-info))]
    (-> latest-games-info
        latest-games/filter-latest-games
        game-scores/parse-game-scores)))
