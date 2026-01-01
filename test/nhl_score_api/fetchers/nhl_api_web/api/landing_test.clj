(ns nhl-score-api.fetchers.nhl-api-web.api.landing-test
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as malli]
            [malli.error :as malli-error]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.landing :as landing]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]))

(def base-url "https://api-web.nhle.com/v1")

(deftest landing-api-request-test
  (testing "get-cache"
    (testing "Returns :archive for game in OFF state"
      (is (= :archive
             (api/get-cache (landing/->LandingApiRequest "2023020209")
                            (resources/get-landing "2023020209")))))

    (testing "Returns nil for game in CRIT state"
      (is (= nil
             (api/get-cache (landing/->LandingApiRequest "2023020206")
                            (resources/get-landing "2023020206"))))))

  (testing "cache-key"
    (is (= "landing-2023020209"
           (api/cache-key (landing/->LandingApiRequest "2023020209")))))

  (testing "description"
    (is (= "landing {:game-id \"2023020209\"}"
           (api/description (landing/->LandingApiRequest "2023020209")))))

  (testing "response-schema"
    (testing "Matches complete response"
      (let [schema (api/response-schema (landing/->LandingApiRequest "2023020209"))
            response (resources/get-landing "2023020209")]
        (is (= true
               (malli/validate schema response)))
        (is (= nil
               (malli-error/humanize (malli/explain schema response))))))

    (testing "Matches minimal response"
      (let [schema (api/response-schema (landing/->LandingApiRequest "2023020209"))
            response (resources/get-landing "2023020209-modified-minimal")]
        (is (= true
               (malli/validate schema response)))
        (is (= nil
               (malli-error/humanize (malli/explain schema response))))))

    (testing "Detects invalid response"
      (let [schema (api/response-schema (landing/->LandingApiRequest "2023020209"))
            response (merge (resources/get-landing "2023020209")
                            {:game-state "unknown" :game-type "invalid"})]
        (is (= false
               (malli/validate schema response)))
        (is (= {:game-state ["should be either \"CRIT\", \"FINAL\", \"FUT\", \"LIVE\", \"OFF\", \"OVER\" or \"PRE\""],
                :game-type ["should be an integer"]}
               (malli-error/humanize (malli/explain schema response)))))))

  (testing "url"
    (is (= (str base-url "/gamecenter/2023020209/landing")
           (api/url (landing/->LandingApiRequest "2023020209"))))))
