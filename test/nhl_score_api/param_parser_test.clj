(ns nhl-score-api.param-parser-test
  (:require [clojure.test :refer [deftest is testing]]
            [nhl-score-api.param-parser :refer [parse-params]]
            [clj-time.core :as time]))

(deftest parsing-date-param
  (testing "Parsing valid date"
    (is (= {:values {:date (time/date-time 2021 7 30)}
            :errors []}
           (parse-params [{:field :date :type :date}] {:date "2021-07-30"}))
        "Success result with parsed date"))

  (testing "Parsing multiple valid dates"
    (is (= {:values {:start-date (time/date-time 2021 7 30)
                     :end-date (time/date-time 2021 7 31)}
            :errors []}
           (parse-params [{:field :start-date :type :date} {:field :end-date :type :date}]
                         {:start-date "2021-07-30" :end-date "2021-07-31"}))
        "Success result with parsed dates"))

  (testing "Parsing invalid date value"
    (is (= {:values nil
            :errors ["Invalid parameter date: Cannot parse \"2021-07-0\": Value 0 for dayOfMonth must be in the range [1,31]"]}
           (parse-params [{:field :date :type :date}] {:date "2021-07-0"}))
        "Invalid parameter error"))

  (testing "Parsing totally invalid value"
    (is (= {:values nil
            :errors ["Invalid parameter date: Invalid format: \"whatisdis\""]}
           (parse-params [{:field :date :type :date}] {:date "whatisdis"}))
        "Invalid parameter error"))

  (testing "Parsing optional missing date"
    (is (= {:values {:date nil}
            :errors []}
           (parse-params [{:field :date :type :date}] {}))
        "Success result with nil date"))

  (testing "Parsing required missing date"
    (is (= {:values nil
            :errors ["Missing required parameter date"]}
           (parse-params [{:field :date :required? true :type :date}] {}))
        "Missing value error")))
