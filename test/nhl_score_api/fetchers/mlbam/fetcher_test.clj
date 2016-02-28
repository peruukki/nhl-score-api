(ns nhl-score-api.fetchers.mlbam.fetcher-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [nhl-score-api.fetchers.mlbam.fetcher :refer :all]))

(deftest fetching-latest-scores

  (testing "The last two days' scores are requested"
    (let [now (time/now)
          yesterday (format/unparse (format/formatters :year-month-day) (time/minus now (time/days 1)))
          today (format/unparse (format/formatters :year-month-day) now)
          query-params (get-query-params)]
      (is (= yesterday
             (:startDate query-params)))
      (is (= today
             (:endDate query-params)))))

  (testing "All needed details are requested"
    (is (= "schedule.teams,schedule.scoringplays"
           (:expand (get-query-params))))))
