(ns nhl-score-api.fetchers.nhl-api-web.api.landing-test
  (:require [clojure.test :refer [deftest is testing]]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.landing :as landing]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]))

(def base-url "https://api-web.nhle.com/v1")

(deftest landing-api-request-test
  (testing "archive?"
    (testing "Archives game in OFF state"
      (is (= true
             (api/archive? (landing/->LandingApiRequest "2023020209")
                           (resources/get-landing "2023020209")))))

    (testing "Does not archive game in CRIT state"
      (is (= false
             (api/archive? (landing/->LandingApiRequest "2023020206")
                           (resources/get-landing "2023020206"))))))

  (testing "cache-key"
    (is (= "landing-2023020209"
           (api/cache-key (landing/->LandingApiRequest "2023020209")))))

  (testing "description"
    (is (= "landing {:game-id \"2023020209\"}"
           (api/description (landing/->LandingApiRequest "2023020209")))))

  (testing "url"
    (is (= (str base-url "/gamecenter/2023020209/landing")
           (api/url (landing/->LandingApiRequest "2023020209"))))))
