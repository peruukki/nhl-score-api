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
      (is (> (count starting-players) 0))
      (let [starting-goalies (filter #(and (= "G" (:position %))
                                           (:starting-lineup %))
                                     all-players)]
        (is (> (count starting-goalies) 0))
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
      (doseq [player all-players]
        (let [name (:name player)]
          (is (not (every? #(Character/isUpperCase %) name))
              (str "Name should not be all uppercase: " name))
          (is (not (re-find #"\([CA]\)" name))
              (str "Name should not contain captain markers: " name))))))

  (testing "Normalizes abbreviations correctly"
    (let [html-content (resources/get-roster-html "2023020207")
          result (parser/parse-roster-html html-content)
          away-players (:away result)
          aj-greer (first (filter #(and (= 18 (:number %))
                                        (= "L" (:position %)))
                                  away-players))]
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
      (is (some? dan-vladar))
      (is (= "Dan Vladar" (:name dan-vladar)))
      (is (:starting-lineup dan-vladar) "Dan Vladar should be in starting lineup")
      (is (some? joseph-woll))
      (is (= "Joseph Woll" (:name joseph-woll)))
      (is (:starting-lineup joseph-woll) "Joseph Woll should be in starting lineup")))

  (testing "Handles empty or invalid HTML gracefully"
    (let [result (parser/parse-roster-html "")]
      (is (nil? result)))))

(deftest enrich-roster-with-api-data-test
  (testing "Enriches roster with player IDs from API data"
    (let [html-content (resources/get-roster-html "2023020207")
          html-roster (parser/parse-roster-html html-content)
          api-roster-away (resources/get-roster-api "CGY" "20232024")
          api-roster-home (resources/get-roster-api "TOR" "20232024")
          enriched (parser/enrich-roster-with-api-data html-roster
                                                       api-roster-away
                                                       api-roster-home)]
      (is (map? enriched))
      (is (contains? enriched :away))
      (is (contains? enriched :home))
      (is (vector? (:away enriched)))
      (is (vector? (:home enriched)))))

  (testing "Matches players by jersey number and position"
    (let [html-content (resources/get-roster-html "2023020207")
          html-roster (parser/parse-roster-html html-content)
          api-roster-away (resources/get-roster-api "CGY" "20232024")
          api-roster-home (resources/get-roster-api "TOR" "20232024")
          enriched (parser/enrich-roster-with-api-data html-roster
                                                       api-roster-away
                                                       api-roster-home)
          dan-vladar (first (filter #(and (= 80 (:number %))
                                          (= "G" (:position %)))
                                    (:away enriched)))
          joseph-woll (first (filter #(and (= 60 (:number %))
                                           (= "G" (:position %)))
                                     (:home enriched)))]
      (is (some? dan-vladar))
      (is (contains? dan-vladar :player-id))
      (is (= 8478435 (:player-id dan-vladar)))
      (is (= "Dan Vladar" (:name dan-vladar)))
      (is (:starting-lineup dan-vladar) "Starting lineup should be preserved")
      (is (some? joseph-woll))
      (is (contains? joseph-woll :player-id))
      (is (= 8479361 (:player-id joseph-woll)))
      (is (= "Joseph Woll" (:name joseph-woll)))
      (is (:starting-lineup joseph-woll) "Starting lineup should be preserved")))

  (testing "Preserves starting lineup information when enriching"
    (let [html-content (resources/get-roster-html "2023020207")
          html-roster (parser/parse-roster-html html-content)
          api-roster-away (resources/get-roster-api "CGY" "20232024")
          api-roster-home (resources/get-roster-api "TOR" "20232024")
          enriched (parser/enrich-roster-with-api-data html-roster
                                                       api-roster-away
                                                       api-roster-home)
          all-players (concat (:away enriched) (:home enriched))]
      (doseq [player all-players]
        (is (contains? player :starting-lineup))
        (is (boolean? (:starting-lineup player))))
      (let [starting-players (filter :starting-lineup all-players)]
        (is (> (count starting-players) 0))
        (let [starting-goalies (filter #(and (= "G" (:position %))
                                             (:starting-lineup %))
                                       all-players)]
          (is (> (count starting-goalies) 0))
          (doseq [goalie starting-goalies]
            (is (contains? goalie :player-id) "Starting goalies should have player IDs"))))))

  (testing "Handles unmatched players gracefully"
    (let [html-content (resources/get-roster-html "2023020207")
          html-roster (parser/parse-roster-html html-content)
          ;; Use empty API rosters to simulate unmatched players
          empty-api-roster {:forwards [] :defensemen [] :goalies []}
          enriched (parser/enrich-roster-with-api-data html-roster
                                                       empty-api-roster
                                                       empty-api-roster)
          all-players (concat (:away enriched) (:home enriched))]
      (is (> (count all-players) 0))
      (doseq [player all-players]
        (is (not (contains? player :player-id))
            "Unmatched players should not have player-id"))
      (doseq [player all-players]
        (is (contains? player :number))
        (is (contains? player :position))
        (is (contains? player :name))
        (is (contains? player :starting-lineup)))))

  (testing "Unmatched players have normalized names"
    (let [html-content (resources/get-roster-html "2023020207")
          html-roster (parser/parse-roster-html html-content)
          ;; Use empty API rosters so no players match
          empty-api-roster {:forwards [] :defensemen [] :goalies []}
          enriched (parser/enrich-roster-with-api-data html-roster
                                                       empty-api-roster
                                                       empty-api-roster)
          ;; Get a specific unmatched player (e.g., A.J. Greer from away team)
          aj-greer (first (filter #(and (= 18 (:number %))
                                        (= "L" (:position %)))
                                  (:away enriched)))]
      (is (some? aj-greer))
      (is (not (contains? aj-greer :player-id))
          "Unmatched player should not have player-id")
      (is (= "A.J. Greer" (:name aj-greer))
          "Unmatched player name should be normalized (title case, not all uppercase)")
      (is (not (re-find #"\([CA]\)" (:name aj-greer)))
          "Unmatched player name should not contain captain markers"))))

(testing "Matches players by name when jersey number doesn't match"
  (let [html-player {:number 99 :position "G" :name "Test Goalie" :starting-lineup true}
        api-players [{:id 12345
                      :sweater-number 1
                      :position-code "G"
                      :first-name {:default "Test"}
                      :last-name {:default "Goalie"}}]
        html-roster {:away [html-player] :home []}
        enriched (parser/enrich-roster-with-api-data html-roster
                                                     {:forwards [] :defensemen [] :goalies api-players}
                                                     {:forwards [] :defensemen [] :goalies []})
        matched-player (first (:away enriched))]
    (is (some? matched-player))
    (is (contains? matched-player :player-id))
    (is (= 12345 (:player-id matched-player)))
    (is (:starting-lineup matched-player) "Starting lineup should be preserved")))
