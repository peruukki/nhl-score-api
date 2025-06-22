(ns nhl-score-api.fetchers.nhl-api-web.api.schedule-test
  (:require [clojure.test :refer [deftest is testing]]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.schedule :as schedule]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]))

(def base-url "https://api-web.nhle.com/v1")

(deftest schedule-api-request-test
  (testing "archive?"
    (testing "with single date"
      (testing "Archives when all games are in OFF state"
        (is (= true
               (api/archive? (schedule/->ScheduleApiRequest "2023-11-09" nil)
                             resources/games-finished-in-regulation-overtime-and-shootout))))

      (testing "Does not archive when not all games are in OFF state"
        (is (= false
               (api/archive? (schedule/->ScheduleApiRequest "2023-11-10" nil)
                             resources/games-finished-in-regulation-overtime-and-shootout)))))

    (testing "with date range"
      (testing "Archives when all games are in OFF state and have a video recap"
        (is (= true
               (api/archive? (schedule/->ScheduleApiRequest "2023-11-08" "2023-11-09")
                             resources/games-finished-in-regulation-overtime-and-shootout))))

      (testing "Does not archive when not all games are in OFF state"
        (is (= false
               (api/archive? (schedule/->ScheduleApiRequest "2023-11-08" "2023-11-10")
                             resources/games-finished-in-regulation-overtime-and-shootout))))

      (testing "Does not archive when not all games have a video recap"
        (is (= false
               (api/archive? (schedule/->ScheduleApiRequest "2023-11-08" "2023-11-09")
                             resources/games-finished-missing-video-recap))))))

  (testing "cache-key"
    (testing "with single date"
      (is (= "schedule-2023-02-02"
             (api/cache-key (schedule/->ScheduleApiRequest "2023-02-02" nil)))))

    (testing "with date range"
      (is (= "schedule-2023-02-02-2023-02-03"
             (api/cache-key (schedule/->ScheduleApiRequest "2023-02-02" "2023-02-03"))))))

  (testing "description"
    (testing "with single date"
      (is (= "schedule {:date \"2023-02-02\"}"
             (api/description (schedule/->ScheduleApiRequest "2023-02-02" nil)))))

    (testing "with date range"
      (is (= "schedule {:date \"2023-02-02\"}"
             (api/description (schedule/->ScheduleApiRequest "2023-02-02" "2023-02-03"))))))

  (testing "url"
    (testing "with single date"
      (is (= (str base-url "/schedule/2023-02-02")
             (api/url (schedule/->ScheduleApiRequest "2023-02-02" nil)))))

    (testing "with date range"
      (is (= (str base-url "/schedule/2023-02-02")
             (api/url (schedule/->ScheduleApiRequest "2023-02-02" "2023-02-03")))))))
