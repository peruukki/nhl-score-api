(ns nhl-score-api.fetchers.nhl-api-web.fetcher-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [nhl-score-api.fetchers.nhl-api-web.fetcher :refer :all]))

(deftest get-scores-query-params-test

  (testing "Scores are requested starting from yesterday if no dates are given"
    (let [now (time/now)
          yesterday (format/unparse (format/formatters :year-month-day) (time/minus now (time/days 1)))]
      (is (= yesterday
             (get-schedule-start-date nil)) "Start date")))

  (testing "Scores are requested from given start date"
    (let [start-date (time/date-time 2021 9 25)]
      (is (= "2021-09-25"
             (get-schedule-start-date start-date)) "Start date"))))

(deftest get-standings-query-params-test

  (testing "Standings are requested for appropriate season"
    (is (= "20202021"
           (:season (get-standings-query-params "2021-08-31"))) "Date in August")
    (is (= "20212022"
           (:season (get-standings-query-params "2021-09-01"))) "Date in September")))

(deftest fetch-standings-info-test

  (testing "Standings are not fetched if there are no latest games"
    (is (= {:records nil}
           (fetch-standings-info nil)) "No team records")))

(deftest get-landing-urls-by-game-id-test

  (testing "Landing URL is returned for live and finished games"
    (is (= {1 (str base-url "/gamecenter/1/landing")
            2 (str base-url "/gamecenter/2/landing")}
           (get-landing-urls-by-game-id [{:id 1 :game-state "LIVE"}
                                         {:id 2 :game-state "OFF"}
                                         {:id 3 :game-state "FUT"}])))))
