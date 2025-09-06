(ns nhl-score-api.fetchers.nhl-api-web.data-test
  (:require [clojure.test :refer [deftest is testing]]
            [nhl-score-api.fetchers.nhl-api-web.data :refer [get-team-names]]))

(deftest get-team-name-test

  (testing "Returns team data for team without previous names"
    (is (= {:short-name "Boston" :team-name "Bruins"}
           (get-team-names "BOS" 20232024))
        "Team without previous names"))

  (testing "Returns team data for team with previous names"
    (is (= {:short-name "Utah" :team-name "Hockey Club"}
           (get-team-names "UTA" 20242025))
        "Last season before name change")
    (is (= {:short-name "Utah" :team-name "Mammoth"}
           (get-team-names "UTA" 20252026))
        "First season after name change"))

  (testing "Returns empty data for unknown team abbreviation"
    (is (= {:short-name "" :team-name ""}
           (get-team-names "ABC" 20232024))
        "Unknown team abbreviation")))
