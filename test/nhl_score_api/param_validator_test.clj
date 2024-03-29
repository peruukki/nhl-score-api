(ns nhl-score-api.param-validator-test
  (:require [clj-time.core :as time]
            [clojure.test :refer [deftest is testing]]
            [nhl-score-api.param-validator :refer [validate-date-range]]))

(deftest validating-date-range
  (testing "Validating date range with maximum limit"
    (is (= nil
           (validate-date-range (time/date-time 2021 9 30) nil 3))
        "Single date range validates")

    (is (= nil
           (validate-date-range (time/date-time 2021 9 30) (time/date-time 2021 9 30) 3))
        "Same start and end date validates")

    (is (= nil
           (validate-date-range (time/date-time 2021 9 30) (time/date-time 2021 10 2) 3))
        "Maximum limit date range validates")

    (is (= "Date range exceeds maximum limit of 3 days"
           (validate-date-range (time/date-time 2021 9 30) (time/date-time 2021 10 3) 3))
        "Too long date range fails")

    (is (= "End date is before start date"
           (validate-date-range (time/date-time 2021 9 30) (time/date-time 2021 9 29) 3))
        "End date before start date fails")))
