(ns nhl-score-api.fetchers.nhl-api-web.api.right-rail-test
  (:require [clojure.test :refer [deftest is testing]]
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

  (testing "url"
    (is (= (str base-url "/gamecenter/2023020209/right-rail")
           (api/url (right-rail/->RightRailApiRequest "2023020209"))))))
