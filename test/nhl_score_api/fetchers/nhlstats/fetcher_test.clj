(ns nhl-score-api.fetchers.nhlstats.fetcher-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [nhl-score-api.fetchers.nhlstats.fetcher :refer :all]))

(deftest get-schedule-query-params-test

  (testing "The last two days' scores are requested"
    (let [now (time/date-time 2020 3 11)
          yesterday (format/unparse (format/formatters :year-month-day) (time/minus now (time/days 1)))
          today (format/unparse (format/formatters :year-month-day) now)
          query-params (get-schedule-query-params)]
      (is (= yesterday
             (:startDate query-params)) "Start date")
      (is (= today
             (:endDate query-params)) "End date")))

  (testing "All needed schedule details are requested"
    (is (= "schedule.teams,schedule.scoringplays,schedule.game.seriesSummary,seriesSummary.series,schedule.linescore"
           (:expand (get-schedule-query-params))) "Expanded fields")))

(deftest fetch-standings-info-test

  (testing "Standings are not fetched if there are no latest games"
    (is (= {:records nil}
           (fetch-standings-info nil)) "No team records")))
