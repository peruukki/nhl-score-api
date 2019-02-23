(ns nhl-score-api.fetchers.nhlstats.fetcher-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [nhl-score-api.fetchers.nhlstats.fetcher :refer :all]))

(deftest fetching-latest-scores

  (testing "The last two days' scores are requested"
    (let [now (time/now)
          yesterday (format/unparse (format/formatters :year-month-day) (time/minus now (time/days 1)))
          today (format/unparse (format/formatters :year-month-day) now)
          query-params (get-schedule-query-params)]
      (is (= yesterday
             (:startDate query-params)) "Start date")
      (is (= today
             (:endDate query-params)) "End date")))

  (testing "All needed schedule details are requested"
    (is (= "schedule.teams,schedule.scoringplays,schedule.game.seriesSummary,seriesSummary.series,schedule.linescore"
           (:expand (get-schedule-query-params))) "Expanded fields"))

  (testing "Standings from the date before the latest selected games are requested"
    (is (= "2019-02-28"
           (:date (get-standings-query-params "2019-03-01"))) "Standings date")))
