(ns nhl-score-api.fetchers.nhl-api-web.roster-parser-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]
            [nhl-score-api.fetchers.nhl-api-web.roster-parser :refer [parse-roster-html]]))

(deftest parse-roster-html-test
  (testing "Parsing roster HTML extracts dressed players for both teams"
    (let [html (resources/get-roster-html 2023020207)
          rosters (parse-roster-html html)
          away-dressed (:dressed-players (:away rosters))
          home-dressed (:dressed-players (:home rosters))]
      (is (seq away-dressed) "Away team has dressed players")
      (is (seq home-dressed) "Home team has dressed players")
      (is (>= (count away-dressed) 20) "Away team has multiple dressed players")
      (is (>= (count home-dressed) 20) "Home team has multiple dressed players")))

  (testing "Parsing roster HTML extracts scratched players for both teams"
    (let [html (resources/get-roster-html 2023020207)
          rosters (parse-roster-html html)
          away-scratched (:scratched-players (:away rosters))
          home-scratched (:scratched-players (:home rosters))]
      (is (seq away-scratched) "Away team has scratched players")
      (is (seq home-scratched) "Home team has scratched players")))

  (testing "Player information is correctly extracted"
    (let [html (resources/get-roster-html 2023020207)
          rosters (parse-roster-html html)
          away-dressed (:dressed-players (:away rosters))
          first-player (first away-dressed)]
      (is (contains? first-player :name) "Player has name")
      (is (contains? first-player :number) "Player has number")
      (is (contains? first-player :position) "Player has position")
      (is (string? (:name first-player)) "Name is a string")
      (is (integer? (:number first-player)) "Number is an integer")
      (is (string? (:position first-player)) "Position is a string")))

  (testing "Player names are normalized from UPPERCASE to title case"
    (let [html (resources/get-roster-html 2023020207)
          rosters (parse-roster-html html)
          away-dressed (:dressed-players (:away rosters))
          player-names (map :name away-dressed)]
      (is (some #(= % "Connor Zary") player-names) "Name normalized correctly")
      (is (some #(= % "Dan Vladar") player-names) "Name normalized correctly")
      (is (not-any? #(re-find #"^[A-Z\s]+$" %) player-names) "No all-uppercase names")))

  (testing "Captain and alternate markers are removed from names"
    (let [html (resources/get-roster-html 2023020207)
          rosters (parse-roster-html html)
          away-dressed (:dressed-players (:away rosters))
          home-dressed (:dressed-players (:home rosters))
          all-players (concat away-dressed home-dressed)
          player-names (map :name all-players)]
      (is (not-any? #(str/includes? % "(C)") player-names) "No captain markers in names")
      (is (not-any? #(str/includes? % "(A)") player-names) "No alternate markers in names")))

  (testing "Starting lineup players are correctly identified"
    (let [html (resources/get-roster-html 2023020207)
          rosters (parse-roster-html html)
          away-dressed (:dressed-players (:away rosters))
          home-dressed (:dressed-players (:home rosters))
          all-dressed (concat away-dressed home-dressed)
          starting-players (filter :starting-lineup all-dressed)
          starting-away-players (filter :starting-lineup away-dressed)
          starting-home-players (filter :starting-lineup home-dressed)]
      (is (seq starting-players) "Some players are in starting lineup")
      (is (every? #(= (:starting-lineup %) true) starting-players) "All starting players have :starting-lineup true")
      (is (= (count starting-away-players) 6) "Away team has exactly 6 players in starting lineup")
      (is (= (count starting-home-players) 6) "Home team has exactly 6 players in starting lineup")))

  (testing "Starting goalies are correctly marked"
    (let [html (resources/get-roster-html 2023020207)
          rosters (parse-roster-html html)
          away-dressed (:dressed-players (:away rosters))
          home-dressed (:dressed-players (:home rosters))
          away-goalies (filter #(= (:position %) "G") away-dressed)
          home-goalies (filter #(= (:position %) "G") home-dressed)
          starting-away-goalies (filter :starting-lineup away-goalies)
          starting-home-goalies (filter :starting-lineup home-goalies)
          starting-away-goalie (first starting-away-goalies)
          starting-home-goalie (first starting-home-goalies)]
      (is starting-away-goalie "Away team has a starting goalie")
      (is starting-home-goalie "Home team has a starting goalie")
      (is (= (:starting-lineup starting-away-goalie) true) "Starting away goalie has :starting-lineup true")
      (is (= (:starting-lineup starting-home-goalie) true) "Starting home goalie has :starting-lineup true")
      (is (= (count starting-away-goalies) 1) "Away team has exactly one starting goalie")
      (is (= (count starting-home-goalies) 1) "Home team has exactly one starting goalie")))

  (testing "Non-starting players do not have starting-lineup field"
    (let [html (resources/get-roster-html 2023020207)
          rosters (parse-roster-html html)
          away-dressed (:dressed-players (:away rosters))
          home-dressed (:dressed-players (:home rosters))
          all-dressed (concat away-dressed home-dressed)
          non-starting-players (remove :starting-lineup all-dressed)]
      (is (seq non-starting-players) "Some players are not in starting lineup")
      (is (every? #(not (contains? % :starting-lineup)) non-starting-players) "Non-starting players omit :starting-lineup field")))

  (testing "Scratched players do not have starting-lineup field"
    (let [html (resources/get-roster-html 2023020207)
          rosters (parse-roster-html html)
          away-scratched (:scratched-players (:away rosters))
          home-scratched (:scratched-players (:home rosters))
          all-scratched (concat away-scratched home-scratched)]
      (is (every? #(not (contains? % :starting-lineup)) all-scratched) "Scratched players do not have :starting-lineup field")))

  (testing "Specific players are correctly parsed"
    (let [html (resources/get-roster-html 2023020207)
          rosters (parse-roster-html html)
          away-dressed (:dressed-players (:away rosters))
          home-dressed (:dressed-players (:home rosters))
          dan-vladar (first (filter #(= (:name %) "Dan Vladar") away-dressed))
          william-nylander (first (filter #(= (:name %) "William Nylander") home-dressed))]
      (is dan-vladar "Dan Vladar is found")
      (is (= (:number dan-vladar) 80) "Dan Vladar has correct number")
      (is (= (:position dan-vladar) "G") "Dan Vladar has correct position")
      (is (:starting-lineup dan-vladar) "Dan Vladar is in starting lineup")
      (is william-nylander "William Nylander is found")
      (is (= (:number william-nylander) 88) "William Nylander has correct number")
      (is (= (:position william-nylander) "R") "William Nylander has correct position")
      (is (:starting-lineup william-nylander) "William Nylander is in starting lineup")))

  (testing "Scratched players are correctly parsed"
    (let [html (resources/get-roster-html 2023020207)
          rosters (parse-roster-html html)
          away-scratched (:scratched-players (:away rosters))
          home-scratched (:scratched-players (:home rosters))
          dennis-gilbert (first (filter #(= (:name %) "Dennis Gilbert") away-scratched))
          john-klingberg (first (filter #(= (:name %) "John Klingberg") home-scratched))]
      (is dennis-gilbert "Dennis Gilbert is found in scratched players")
      (is (= (:number dennis-gilbert) 48) "Dennis Gilbert has correct number")
      (is (= (:position dennis-gilbert) "D") "Dennis Gilbert has correct position")
      (is john-klingberg "John Klingberg is found in scratched players")
      (is (= (:number john-klingberg) 3) "John Klingberg has correct number")
      (is (= (:position john-klingberg) "D") "John Klingberg has correct position"))))
