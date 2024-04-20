(ns nhl-score-api.fetchers.nhl-api-web.fetcher-test
  (:require [clj-time.core :as time]
            [clojure.test :refer [deftest is testing]]
            [nhl-score-api.fetchers.nhl-api-web.fetcher :refer [fetch-standings-infos
                                                                get-current-schedule-date
                                                                get-current-standings-request-date
                                                                get-landing-game-ids
                                                                get-pre-game-standings-request-date
                                                                get-schedule-date-range-str-for-latest-scores]]
            [nhl-score-api.utils :refer [format-date]]))

(deftest get-schedule-date-range-str-for-latest-scores-test

  (testing "Scores are requested from yesterday and today"
    (let [current-date (get-current-schedule-date (time/now))
          the-day-before (time/minus current-date (time/days 1))]
      (is (= {:start (format-date the-day-before) :end (format-date current-date)}
             (get-schedule-date-range-str-for-latest-scores)) "Date range"))))

(deftest get-current-standings-request-date-test

  (testing "Standings are requested for appropriate date"
    (is (= "2023-11-18"
           (get-current-standings-request-date {:requested-date-str "2023-11-18"
                                                :current-date-str "2023-11-18"
                                                :regular-season-end-date-str "2024-04-14"}))
        "Date during regular season")
    (is (= "2023-08-31"
           (get-current-standings-request-date {:requested-date-str "2023-08-31"
                                                :current-date-str "2023-11-18"
                                                :regular-season-end-date-str "2024-04-14"}))
        "Date in the past between regular seasons")
    (is (= "2023-11-18"
           (get-current-standings-request-date {:requested-date-str "2024-08-31"
                                                :current-date-str "2023-11-18"
                                                :regular-season-end-date-str "2024-04-14"}))
        "Date in the future after current regular season")
    (is (= "2023-11-18"
           (get-current-standings-request-date {:requested-date-str "2023-11-19"
                                                :current-date-str "2023-11-18"
                                                :regular-season-end-date-str "2024-04-14"}))
        "Date in the future during current regular season")
    (is (= nil
           (get-current-standings-request-date {:requested-date-str "1900-11-19"
                                                :current-date-str "2023-11-18"
                                                :regular-season-end-date-str nil}))
        "Date before any season in NHL history")
    (is (= nil
           (get-current-standings-request-date {:requested-date-str nil
                                                :current-date-str "2023-11-18"
                                                :regular-season-end-date-str "2024-04-14"}))
        "No requested date")))

(deftest get-pre-game-standings-request-date-test

  (testing "Standings are requested for appropriate date"
    (is (= "2023-11-17"
           (get-pre-game-standings-request-date {:current-standings-date-str "2023-11-18"
                                                 :regular-season-start-date-str "2023-10-10"}))
        "Current standings date during regular season")
    (is (= "2023-10-10"
           (get-pre-game-standings-request-date {:current-standings-date-str "2023-10-10"
                                                 :regular-season-start-date-str "2023-10-10"}))
        "Current standings date on first day of regular season")
    (is (= nil
           (get-pre-game-standings-request-date {:current-standings-date-str nil
                                                 :regular-season-start-date-str "2023-10-10"}))
        "No current standings date"))

  (testing "Error is thrown for invalid inputs"
    (is (thrown? AssertionError
                 (get-pre-game-standings-request-date {:current-standings-date-str "2023-10-09"
                                                       :regular-season-start-date-str "2023-10-10"}))
        "Regular season start date after current standings date")))

(deftest fetch-standings-info-test

  (testing "Standings are not fetched if there are no latest games"
    (is (= [nil]
           (fetch-standings-infos {:date-strs [nil]
                                   :regular-season-start-date-str nil
                                   :regular-season-end-date-str nil}
                                  []))
        "No team records")))

(deftest get-landing-game-ids-test

  (testing "Landing URL is returned for live and finished games"
    (is (= [1 2]
           (get-landing-game-ids [{:id 1 :game-state "LIVE"}
                                  {:id 2 :game-state "OFF"}
                                  {:id 3 :game-state "FUT"}])))))

(deftest get-current-schedule-date-test

  (testing "Previous date is returned before midnight US/Pacific (-07:00 on tested date)"
    (is (= "2024-03-20" (format-date (get-current-schedule-date (time/date-time 2024 3 21 6 59 59))))))

  (testing "Current date is returned at midnight US/Pacific (-07:00 on tested date)"
    (is (= "2024-03-21" (format-date (get-current-schedule-date (time/date-time 2024 3 21 7 00 00))))))

  (testing "Current date is returned after midnight US/Pacific (-07:00 on tested date)"
    (is (= "2024-03-21" (format-date (get-current-schedule-date (time/date-time 2024 3 21 7 00 01)))))))
