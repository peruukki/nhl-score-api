(ns nhl-score-api.fetchers.nhl-api-web.api.right-rail-test
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as malli]
            [malli.error :as malli-error]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.right-rail :as right-rail]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]))

(def base-url "https://api-web.nhle.com/v1")

(deftest right-rail-api-request-test
  (testing "archive?"
    (testing "Archives game with recap video"
      (is (= true
             (api/archive? (right-rail/->RightRailApiRequest "2023020209")
                           (resources/get-right-rail "2023020209")))))

    (testing "Does not archive game without recap video"
      (is (= false
             (api/archive? (right-rail/->RightRailApiRequest "2023020206")
                           (resources/get-right-rail "2023020206"))))))

  (testing "cache-key"
    (is (= "right-rail-2023020209"
           (api/cache-key (right-rail/->RightRailApiRequest "2023020209")))))

  (testing "description"
    (is (= "right-rail {:game-id \"2023020209\"}"
           (api/description (right-rail/->RightRailApiRequest "2023020209")))))

  (testing "response-schema"
    (testing "Matches valid response"
      (let [schema (api/response-schema (right-rail/->RightRailApiRequest "2023020209"))
            response (resources/get-right-rail "2023020209")]
        (is (= true
               (malli/validate schema response)))
        (is (= nil
               (malli-error/humanize (malli/explain schema response))))))

    (testing "Detects invalid response"
      (let [schema (api/response-schema (right-rail/->RightRailApiRequest "2023020209"))
            response (merge (resources/get-right-rail "2023020209")
                            {:team-game-stats [{:away-value 1 :category "unknown" :home-value 2}
                                               {:away-value 1 :category "powerPlay" :home-value 2}]})]
        (is (= false
               (malli/validate schema response)))
        (is (= {:team-game-stats
                [{:category ["should be either \"blockedShots\", \"giveaways\", \"hits\", \"pim\", \"sog\" or \"takeaways\""
                             "should be either \"faceoffWinningPctg\" or \"powerPlayPctg\""
                             "should be either \"faceoffWins\" or \"powerPlay\""]}]}
               (malli-error/humanize (malli/explain schema response)))))))

  (testing "url"
    (is (= (str base-url "/gamecenter/2023020209/right-rail")
           (api/url (right-rail/->RightRailApiRequest "2023020209"))))))
