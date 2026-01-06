(ns nhl-score-api.param-parser-test
  (:require [clj-time.core :as time]
            [clojure.test :refer [deftest is testing]]
            [nhl-score-api.param-parser :refer [parse-params parse-include-param]]))

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

(deftest parsing-string-param
  (testing "Parsing valid string"
    (is (= {:values {:name "test"}
            :errors []}
           (parse-params [{:field :name :type :string}] {:name "test"}))
        "Success result with string value"))

  (testing "Parsing optional missing string"
    (is (= {:values {:name nil}
            :errors []}
           (parse-params [{:field :name :type :string}] {}))
        "Success result with nil string"))

  (testing "Parsing required missing string"
    (is (= {:values nil
            :errors ["Missing required parameter name"]}
           (parse-params [{:field :name :required? true :type :string}] {}))
        "Missing value error")))

(deftest parsing-include-param
  (testing "Parsing single value"
    (is (= #{"rosters"}
           (parse-include-param "rosters"))
        "Single inclusion value"))

  (testing "Parsing multiple comma-separated values"
    (is (= #{"rosters" "otherThing"}
           (parse-include-param "rosters,otherThing"))
        "Multiple inclusion values"))

  (testing "Parsing with whitespace"
    (is (= #{"rosters" "otherThing"}
           (parse-include-param "rosters , otherThing"))
        "Values with whitespace trimmed"))

  (testing "Parsing with extra commas"
    (is (= #{"rosters" "otherThing"}
           (parse-include-param "rosters,,otherThing"))
        "Extra commas handled"))

  (testing "Parsing nil value"
    (is (= #{}
           (parse-include-param nil))
        "Nil returns empty set"))

  (testing "Parsing empty string"
    (is (= #{}
           (parse-include-param ""))
        "Empty string returns empty set"))

  (testing "Parsing empty string with whitespace"
    (is (= #{}
           (parse-include-param "   "))
        "Whitespace-only string returns empty set"))

  (testing "Checking if rosters is included"
    (is (= true
           (contains? (parse-include-param "rosters") "rosters"))
        "Rosters is in inclusion set"))

  (testing "Checking if rosters is not included"
    (is (= false
           (contains? (parse-include-param "otherThing") "rosters"))
        "Rosters is not in inclusion set"))

  (testing "Case sensitivity"
    (is (= #{"rosters" "ROSTERS"}
           (parse-include-param "rosters,ROSTERS"))
        "Case-sensitive parsing (preserves case)")))
