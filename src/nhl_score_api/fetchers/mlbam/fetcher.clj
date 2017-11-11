(ns nhl-score-api.fetchers.mlbam.fetcher
  (:require [nhl-score-api.fetchers.mlbam.game-scores :as game-scores]
            [nhl-score-api.fetchers.mlbam.latest-games :as latest-games]
            [clojure.data.json :as json]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [clj-http.lite.client :as http])) ; clj-http-lite supports SNI (unlike http-kit or clj-http)

(def scores-url "https://statsapi.web.nhl.com/api/v1/schedule")

(defn- format-date [date]
  (format/unparse (format/formatters :year-month-day) date))

(defn get-query-params []
  (let [now (time/now)
        start-date (format-date (time/minus now (time/days 1)))
        end-date (format-date now)]
    {:startDate start-date
     :endDate end-date
     :expand "schedule.teams,schedule.scoringplays,schedule.game.seriesSummary,seriesSummary.series"}))

(defn- fetch-latest-games-info []
  (:body (http/get scores-url {:query-params (get-query-params)})))

(defn fetch-latest-scores []
  (-> (fetch-latest-games-info)
      latest-games/filter-latest-games
      game-scores/parse-game-scores))
