(ns nhl-score-api.fetchers.nhl-api-web.api.roster-test
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as malli]
            [malli.error :as malli-error]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.roster :as roster]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]))

(def base-url "https://api-web.nhle.com/v1")

(deftest roster-api-request-test
  (testing "cache-key"
    (is (= "roster-CGY-20232024"
           (api/cache-key (roster/->RosterApiRequest "CGY" "20232024"))))
    (is (= "roster-TOR-20232024"
           (api/cache-key (roster/->RosterApiRequest "TOR" "20232024")))))

  (testing "description"
    (is (= "roster {:team \"CGY\", :season \"20232024\"}"
           (api/description (roster/->RosterApiRequest "CGY" "20232024")))))

  (testing "url"
    (is (= (str base-url "/roster/CGY/20232024")
           (api/url (roster/->RosterApiRequest "CGY" "20232024"))))
    (is (= (str base-url "/roster/TOR/20232024")
           (api/url (roster/->RosterApiRequest "TOR" "20232024")))))

  (testing "response-schema"
    (testing "Matches complete response"
      (let [schema (api/response-schema (roster/->RosterApiRequest "CGY" "20232024"))
            response (resources/get-roster-api "CGY" "20232024")]
        (is (= true
               (malli/validate schema response))
            "Response validates against schema")
        (is (= nil
               (malli-error/humanize (malli/explain schema response)))
            "No validation errors")))

    (testing "Detects invalid response"
      (let [schema (api/response-schema (roster/->RosterApiRequest "CGY" "20232024"))
            response {:forwards "invalid"}]
        (is (= false
               (malli/validate schema response))
            "Invalid response fails validation")
        (is (not= nil
                  (malli-error/humanize (malli/explain schema response)))
            "Validation errors present"))))

  (testing "archive?"
    (is (= false
           (api/archive? (roster/->RosterApiRequest "CGY" "20232024")
                        (resources/get-roster-api "CGY" "20232024")))
        "Archive check returns false (simple implementation)")))
