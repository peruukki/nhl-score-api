(ns nhl-score-api.fetchers.nhl-api-web.fetcher-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [nhl-score-api.fetchers.nhl-api-web.fetcher :refer :all]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]
            [nhl-score-api.utils :refer [format-date]]))

(deftest get-scores-query-params-test

  (testing "Scores are requested starting from yesterday if no dates are given"
    (let [now (time/now)
          yesterday (format-date (time/minus now (time/days 1)))]
      (is (= yesterday
             (get-schedule-start-date nil)) "Start date")))

  (testing "Scores are requested from given start date"
    (let [start-date (time/date-time 2021 9 25)]
      (is (= "2021-09-25"
             (get-schedule-start-date start-date)) "Start date"))))

(deftest get-standings-request-date-test

  (testing "Standings are requested for appropriate date"
    (is (= "2023-11-18"
           (get-standings-request-date "2023-11-18" resources/standings-parameters))
        "Date during regular season")
    (is (= "2023-04-14"
           (get-standings-request-date "2023-08-31" resources/standings-parameters))
        "Date in the past between regular seasons")
    (is (= "2023-11-18"
           (get-standings-request-date "2023-11-19" resources/standings-parameters))
        "Date in the future")
    (is (= nil
           (get-standings-request-date "1900-11-19" resources/standings-parameters))
        "Date before any season in NHL history")))

(deftest fetch-standings-info-test

  (testing "Standings are not fetched if there are no latest games"
    (is (= {:records nil}
           (fetch-standings-info nil nil)) "No team records")))

(deftest get-landing-urls-by-game-id-test

  (testing "Landing URL is returned for live and finished games"
    (is (= {1 (str base-url "/gamecenter/1/landing")
            2 (str base-url "/gamecenter/2/landing")}
           (get-landing-urls-by-game-id [{:id 1 :game-state "LIVE"}
                                         {:id 2 :game-state "OFF"}
                                         {:id 3 :game-state "FUT"}])))))
