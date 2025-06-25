(ns nhl-score-api.fetchers.nhl-api-web.api.standings-test
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as malli]
            [malli.error :as malli-error]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.standings :as standings]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]))

(def base-url "https://api-web.nhle.com/v1")

(deftest standings-api-request-test
  (let [schedule-response resources/games-finished-in-regulation-overtime-and-shootout]
    (testing "archive?"
      (testing "without pre-game standings"
        (let [pre-game-standings nil]
          (testing "Archives standings from date where all games are in OFF state"
            (is (= true
                   (api/archive? (standings/->StandingsApiRequest "2023-11-09" schedule-response pre-game-standings)
                                 resources/current-standings))))

          (testing "Does not archive standings from date where not all games are in OFF state"
            (is (= false
                   (api/archive? (standings/->StandingsApiRequest "2023-11-10" schedule-response pre-game-standings)
                                 resources/current-standings))))))

      (testing "with pre-game standings"
        (let [pre-game-standings resources/pre-game-standings]
          (testing "Archives standings from date where all games are in OFF state and all standings have updated"
            (is (= true
                   (api/archive? (standings/->StandingsApiRequest "2023-11-09" schedule-response pre-game-standings)
                                 resources/current-standings))))

          (testing "Does not archive standings from date where all games are in OFF state but not all standings have updated"
            (is (= false
                   (api/archive? (standings/->StandingsApiRequest "2023-11-09" schedule-response pre-game-standings)
                                 resources/current-standings-not-fully-updated))))

          (testing "Does not archive standings from date where not all games are in OFF state"
            (is (= false
                   (api/archive? (standings/->StandingsApiRequest "2023-11-10" schedule-response pre-game-standings)
                                 resources/current-standings)))))))

    (testing "cache-key"
      (is (= "standings-2023-11-09"
             (api/cache-key (standings/->StandingsApiRequest "2023-11-09" schedule-response nil)))))

    (testing "description"
      (is (= "standings {:date \"2023-11-09\"}"
             (api/description (standings/->StandingsApiRequest "2023-11-09" schedule-response nil)))))

    (testing "response-schema"
      (testing "Matches valid response"
        (let [schema (api/response-schema (standings/->StandingsApiRequest "2023-11-09" schedule-response nil))
              response resources/current-standings]
          (is (= true
                 (malli/validate schema response)))
          (is (= nil
                 (malli-error/humanize (malli/explain schema response))))))

      (testing "Detects invalid response"
        (let [schema (api/response-schema (standings/->StandingsApiRequest "2023-11-09" schedule-response nil))
              response (merge resources/current-standings
                              {:standings (conj (:standings resources/current-standings)
                                                (-> (last (:standings resources/current-standings))
                                                    (assoc :losses "1")
                                                    (dissoc :points)))})]
          (is (= false
                 (malli/validate schema response)))
          (is (= {:standings (conj
                              (vec (take 32 (repeat nil)))
                              {:losses ["should be an integer"]
                               :points ["missing required key"]})}
                 (malli-error/humanize (malli/explain schema response)))))))

    (testing "url"
      (is (= (str base-url "/standings/2023-11-09")
             (api/url (standings/->StandingsApiRequest "2023-11-09" schedule-response nil)))))))
