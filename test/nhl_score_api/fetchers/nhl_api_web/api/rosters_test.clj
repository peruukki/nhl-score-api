(ns nhl-score-api.fetchers.nhl-api-web.api.rosters-test
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as malli]
            [malli.error :as malli-error]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.rosters :as rosters]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]))

(deftest roster-api-request-test
  (testing "cache-key"
    (is (= "rosters-2023020207"
           (api/cache-key (rosters/->RostersApiRequest "https://www.nhl.com/scores/htmlreports/20232024/RO020207.HTM" 2023020207)))))

  (testing "description"
    (is (= "rosters {:game-id 2023020207}"
           (api/description (rosters/->RostersApiRequest "https://www.nhl.com/scores/htmlreports/20232024/RO020207.HTM" 2023020207)))))

  (testing "url"
    (let [roster-url "https://www.nhl.com/scores/htmlreports/20232024/RO020207.HTM"]
      (is (= roster-url
             (api/url (rosters/->RostersApiRequest roster-url 2023020207))))))

  (testing "get-cache"
    (testing "Returns :archive"
      (is (= :archive
             (api/get-cache (rosters/->RostersApiRequest "https://www.nhl.com/scores/htmlreports/20232024/RO020207.HTM" 2023020207)
                            "any-response")))))

  (testing "get-cache-with-context"
    (testing "Returns :archive"
      (is (= :archive
             (api/get-cache-with-context (rosters/->RostersApiRequest "https://www.nhl.com/scores/htmlreports/20232024/RO020207.HTM" 2023020207)
                                         "any-response"
                                         "any-context")))))

  (testing "response-schema"
    (testing "Matches valid HTML response"
      (let [schema (api/response-schema (rosters/->RostersApiRequest "https://www.nhl.com/scores/htmlreports/20232024/RO020207.HTM" 2023020207))
            html-response (resources/get-roster-html 2023020207)]
        (is (= true
               (malli/validate schema html-response)))
        (is (= nil
               (malli-error/humanize (malli/explain schema html-response))))))

    (testing "Detects invalid response without <html> tag"
      (let [schema (api/response-schema (rosters/->RostersApiRequest "https://www.nhl.com/scores/htmlreports/20232024/RO020207.HTM" 2023020207))
            invalid-response "This is not HTML"]
        (is (= false
               (malli/validate schema invalid-response)))
        (is (some? (malli-error/humanize (malli/explain schema invalid-response))))))

    (testing "Detects empty response"
      (let [schema (api/response-schema (rosters/->RostersApiRequest "https://www.nhl.com/scores/htmlreports/20232024/RO020207.HTM" 2023020207))
            empty-response ""]
        (is (= false
               (malli/validate schema empty-response)))
        (is (some? (malli-error/humanize (malli/explain schema empty-response)))))))

  (testing "transform"
    (testing "Returns response as-is for HTML"
      (let [roster-request (rosters/->RostersApiRequest "https://www.nhl.com/scores/htmlreports/20232024/RO020207.HTM" 2023020207)
            html-response (resources/get-roster-html 2023020207)]
        (is (= html-response
               (api/transform roster-request html-response))
            "Transform returns HTML response unchanged")))))
