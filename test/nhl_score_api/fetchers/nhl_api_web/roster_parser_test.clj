(ns nhl-score-api.fetchers.nhl-api-web.roster-parser-test
  (:require [clojure.test :refer [deftest is testing]]
            [nhl-score-api.fetchers.nhl-api-web.roster-parser :as parser]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]))

(deftest parse-roster-html-test
  (testing "Parses complete roster HTML"
    (let [html-content (resources/get-roster-html "2023020207")
          result (parser/parse-roster-html html-content)]
      (is (map? result))
      (is (contains? result :away))
      (is (contains? result :home))
      (is (vector? (:away result)))
      (is (vector? (:home result)))
      (is (> (count (:away result)) 0))
      (is (> (count (:home result)) 0))))

  (testing "Extracts player information correctly"
    (let [html-content (resources/get-roster-html "2023020207")
          result (parser/parse-roster-html html-content)
          away-players (:away result)
          home-players (:home result)]
      ;; Check that players have required fields
      (doseq [player (concat away-players home-players)]
        (is (contains? player :number))
        (is (contains? player :position))
        (is (contains? player :name))
        (is (contains? player :starting-lineup))
        (is (integer? (:number player)))
        (is (string? (:position player)))
        (is (string? (:name player)))
        (is (boolean? (:starting-lineup player)))
        (is (contains? #{"G" "D" "C" "L" "R"} (:position player))))))

  (testing "Extracts starting lineup information"
    (let [html-content (resources/get-roster-html "2023020207")
          result (parser/parse-roster-html html-content)
          away-players (:away result)
          home-players (:home result)
          all-players (concat away-players home-players)
          starting-players (filter :starting-lineup all-players)]
      ;; Should have some starting players
      (is (> (count starting-players) 0))
      ;; Check that starting goalies are marked
      (let [starting-goalies (filter #(and (= "G" (:position %))
                                           (:starting-lineup %))
                                     all-players)]
        (is (> (count starting-goalies) 0))
        ;; Each team should have exactly one starting goalie
        (let [away-starting-goalies (filter #(and (= "G" (:position %))
                                                  (:starting-lineup %))
                                            away-players)
              home-starting-goalies (filter #(and (= "G" (:position %))
                                                  (:starting-lineup %))
                                            home-players)]
          (is (>= (count away-starting-goalies) 1))
          (is (>= (count home-starting-goalies) 1))))))

  (testing "Normalizes player names correctly"
    (let [html-content (resources/get-roster-html "2023020207")
          result (parser/parse-roster-html html-content)
          away-players (:away result)
          home-players (:home result)
          all-players (concat away-players home-players)]
      ;; Check that names are normalized (not all uppercase)
      (doseq [player all-players]
        (let [name (:name player)]
          (is (not (every? #(Character/isUpperCase %) name))
              (str "Name should not be all uppercase: " name))
          ;; Check that captain markers are removed
          (is (not (re-find #"\([CA]\)" name))
              (str "Name should not contain captain markers: " name))))))

  (testing "Normalizes abbreviations correctly"
    (let [html-content (resources/get-roster-html "2023020207")
          result (parser/parse-roster-html html-content)
          away-players (:away result)
          aj-greer (first (filter #(and (= 18 (:number %))
                                        (= "L" (:position %)))
                                  away-players))]
      ;; A.J. Greer should have abbreviation preserved
      (is (some? aj-greer))
      (is (= "A.J. Greer" (:name aj-greer))
          "Abbreviation 'A.J.' should be preserved in uppercase")))

  (testing "Handles specific players correctly"
    (let [html-content (resources/get-roster-html "2023020207")
          result (parser/parse-roster-html html-content)
          away-players (:away result)
          home-players (:home result)
          dan-vladar (first (filter #(and (= 80 (:number %))
                                          (= "G" (:position %)))
                                    away-players))
          joseph-woll (first (filter #(and (= 60 (:number %))
                                           (= "G" (:position %)))
                                     home-players))]
      ;; Check for specific known players
      (is (some? dan-vladar))
      (is (= "Dan Vladar" (:name dan-vladar)))
      (is (:starting-lineup dan-vladar) "Dan Vladar should be in starting lineup")
      (is (some? joseph-woll))
      (is (= "Joseph Woll" (:name joseph-woll)))
      (is (:starting-lineup joseph-woll) "Joseph Woll should be in starting lineup")))

  (testing "Handles empty or invalid HTML gracefully"
    (let [result (parser/parse-roster-html "")]
      (is (nil? result)))))
