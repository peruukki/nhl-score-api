(ns nhl-score-api.fetchers.nhl-api-web.api-test
  (:require [clojure.test :refer [deftest is testing]]
            [nhl-score-api.fetchers.nhl-api-web.api :as api]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]))

(def base-url "https://api-web.nhle.com/v1")

(deftest landing-api-request-test
  (testing "archive?"
    (testing "Archives game in OFF state"
      (is (= true
             (api/archive? (api/->LandingApiRequest "2023020209")
                           (resources/get-landing "2023020209")))))

    (testing "Does not archive game in CRIT state"
      (is (= false
             (api/archive? (api/->LandingApiRequest "2023020206")
                           (resources/get-landing "2023020206"))))))

  (testing "cache-key"
    (is (= "landing-2023020209"
           (api/cache-key (api/->LandingApiRequest "2023020209")))))

  (testing "description"
    (is (= "landing {:game-id \"2023020209\"}"
           (api/description (api/->LandingApiRequest "2023020209")))))

  (testing "url"
    (is (= (str base-url "/gamecenter/2023020209/landing")
           (api/url (api/->LandingApiRequest "2023020209"))))))

(deftest right-rail-api-request-test
  (testing "archive?"
    (testing "Archives game with recap video"
      (is (= true
             (api/archive? (api/->RightRailApiRequest "2023020209")
                           (resources/get-right-rail "2023020209")))))

    (testing "Does not archive game without recap video"
      (is (= false
             (api/archive? (api/->RightRailApiRequest "2023020206")
                           (resources/get-right-rail "2023020206"))))))

  (testing "cache-key"
    (is (= "right-rail-2023020209"
           (api/cache-key (api/->RightRailApiRequest "2023020209")))))

  (testing "description"
    (is (= "right-rail {:game-id \"2023020209\"}"
           (api/description (api/->RightRailApiRequest "2023020209")))))

  (testing "url"
    (is (= (str base-url "/gamecenter/2023020209/right-rail")
           (api/url (api/->RightRailApiRequest "2023020209"))))))

(deftest schedule-api-request-test
  (testing "archive?"
    (testing "with single date"
      (testing "Archives when all games are in OFF state"
        (is (= true
               (api/archive? (api/->ScheduleApiRequest "2023-11-09" nil)
                             resources/games-finished-in-regulation-overtime-and-shootout))))

      (testing "Does not archive when not all games are in OFF state"
        (is (= false
               (api/archive? (api/->ScheduleApiRequest "2023-11-10" nil)
                             resources/games-finished-in-regulation-overtime-and-shootout)))))

    (testing "with date range"
      (testing "Archives when all games are in OFF state and have a video recap"
        (is (= true
               (api/archive? (api/->ScheduleApiRequest "2023-11-08" "2023-11-09")
                             resources/games-finished-in-regulation-overtime-and-shootout))))

      (testing "Does not archive when not all games are in OFF state"
        (is (= false
               (api/archive? (api/->ScheduleApiRequest "2023-11-08" "2023-11-10")
                             resources/games-finished-in-regulation-overtime-and-shootout))))

      (testing "Does not archive when not all games have a video recap"
        (is (= false
               (api/archive? (api/->ScheduleApiRequest "2023-11-08" "2023-11-09")
                             resources/games-finished-missing-video-recap))))))

  (testing "cache-key"
    (testing "with single date"
      (is (= "schedule-2023-02-02"
             (api/cache-key (api/->ScheduleApiRequest "2023-02-02" nil)))))

    (testing "with date range"
      (is (= "schedule-2023-02-02-2023-02-03"
             (api/cache-key (api/->ScheduleApiRequest "2023-02-02" "2023-02-03"))))))

  (testing "description"
    (testing "with single date"
      (is (= "schedule {:date \"2023-02-02\"}"
             (api/description (api/->ScheduleApiRequest "2023-02-02" nil)))))

    (testing "with date range"
      (is (= "schedule {:date \"2023-02-02\"}"
             (api/description (api/->ScheduleApiRequest "2023-02-02" "2023-02-03"))))))

  (testing "url"
    (testing "with single date"
      (is (= (str base-url "/schedule/2023-02-02")
             (api/url (api/->ScheduleApiRequest "2023-02-02" nil)))))

    (testing "with date range"
      (is (= (str base-url "/schedule/2023-02-02")
             (api/url (api/->ScheduleApiRequest "2023-02-02" "2023-02-03")))))))

(deftest standings-api-request-test
  (let [schedule-response resources/games-finished-in-regulation-overtime-and-shootout]
    (testing "archive?"
      (testing "without pre-game standings"
        (let [pre-game-standings nil]
          (testing "Archives standings from date where all games are in OFF state"
            (is (= true
                   (api/archive? (api/->StandingsApiRequest "2023-11-09" schedule-response pre-game-standings)
                                 resources/current-standings))))

          (testing "Does not archive standings from date where not all games are in OFF state"
            (is (= false
                   (api/archive? (api/->StandingsApiRequest "2023-11-10" schedule-response pre-game-standings)
                                 resources/current-standings))))))

      (testing "with pre-game standings"
        (let [pre-game-standings resources/pre-game-standings]
          (testing "Archives standings from date where all games are in OFF state and all standings have updated"
            (is (= true
                   (api/archive? (api/->StandingsApiRequest "2023-11-09" schedule-response pre-game-standings)
                                 resources/current-standings))))

          (testing "Does not archive standings from date where all games are in OFF state but not all standings have updated"
            (is (= false
                   (api/archive? (api/->StandingsApiRequest "2023-11-09" schedule-response pre-game-standings)
                                 resources/current-standings-not-fully-updated))))

          (testing "Does not archive standings from date where not all games are in OFF state"
            (is (= false
                   (api/archive? (api/->StandingsApiRequest "2023-11-10" schedule-response pre-game-standings)
                                 resources/current-standings)))))))

    (testing "cache-key"
      (is (= "standings-2023-11-09"
             (api/cache-key (api/->StandingsApiRequest "2023-11-09" schedule-response nil)))))

    (testing "description"
      (is (= "standings {:date \"2023-11-09\"}"
             (api/description (api/->StandingsApiRequest "2023-11-09" schedule-response nil)))))

    (testing "url"
      (is (= (str base-url "/standings/2023-11-09")
             (api/url (api/->StandingsApiRequest "2023-11-09" schedule-response nil)))))))
