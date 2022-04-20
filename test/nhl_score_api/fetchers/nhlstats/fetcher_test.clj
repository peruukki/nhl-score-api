(ns nhl-score-api.fetchers.nhlstats.fetcher-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [nhl-score-api.fetchers.nhlstats.fetcher :refer :all]))

(deftest get-schedule-query-params-test

  (testing "The last two days' scores are requested if no dates are given"
    (let [now (time/now)
          yesterday (format/unparse (format/formatters :year-month-day) (time/minus now (time/days 1)))
          today (format/unparse (format/formatters :year-month-day) now)
          query-params (get-schedule-query-params nil nil)]
      (is (= yesterday
             (:startDate query-params)) "Start date")
      (is (= today
             (:endDate query-params)) "End date")))

  (testing "Scores are requested from given start date"
    (let [start-date (time/date-time 2021 9 25)
          query-params (get-schedule-query-params start-date nil)]
      (is (= "2021-09-25"
             (:startDate query-params)) "Start date")
      (is (= "2021-09-25"
             (:endDate query-params)) "End date")))

  (testing "Scores are requested from given date range"
    (let [start-date (time/date-time 2021 9 25)
          end-date (time/date-time 2021 10 1)
          query-params (get-schedule-query-params start-date end-date)]
      (is (= "2021-09-25"
             (:startDate query-params)) "Start date")
      (is (= "2021-10-01"
             (:endDate query-params)) "End date")))

  (testing "All needed schedule details are requested"
    (is (= "schedule.teams,schedule.scoringplays,schedule.game.seriesSummary,seriesSummary.series,schedule.linescore"
           (:expand (get-schedule-query-params nil nil))) "Expanded fields")))

(deftest fetch-standings-info-test

  (testing "Standings are not fetched if there are no latest games"
    (is (= {:records nil}
           (fetch-standings-info nil)) "No team records")))

(deftest get-boxscore-urls-by-game-pk-test

  (testing "Boxscore URL is returned for live and finished games"
    (is (= {"1" (str base-url "/game/1/boxscore")
            "2" (str base-url "/game/2/boxscore")}
           (get-boxscore-urls-by-game-pk [{:game-pk "1" :status {:abstract-game-state "Live"}}
                                          {:game-pk "2" :status {:abstract-game-state "Final"}}
                                          {:game-pk "3" :status {:abstract-game-state "Preview"}}])))))
