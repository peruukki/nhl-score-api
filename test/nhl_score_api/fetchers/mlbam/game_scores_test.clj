(ns nhl-score-api.fetchers.mlbam.game-scores-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.fetchers.mlbam.game-scores :refer :all]
            [nhl-score-api.fetchers.mlbam.latest-games :refer [filter-latest-games]]
            [nhl-score-api.fetchers.mlbam.resources :as resources]))

(deftest game-score-json-parsing

  (testing "Parsing scores with games finished in overtime and in shootout"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)))]
      (is (= 9
             (count games)) "Parsed game count")
      (is (= [4 3 5 7 3 5 9 8 5]
             (map #(count (:goals %)) games)) "Parsed goal count")
      (is (= [{:away "CAR" :home "NJD"} {:away "CGY" :home "BOS"} {:away "PIT" :home "WSH"} {:away "STL" :home "OTT"}
              {:away "EDM" :home "BUF"} {:away "FLA" :home "WPG"} {:away "COL" :home "MIN"} {:away "DAL" :home "NSH"}
              {:away "NYI" :home "VAN"}]
             (map :teams games)) "Parsed team names")
      (is (= [{"CAR" 3 "NJD" 1} {"CGY" 1 "BOS" 2} {"PIT" 2 "WSH" 3} {"STL" 4 "OTT" 3 :shootout true}
              {"EDM" 2 "BUF" 1 :overtime true} {"FLA" 3 "WPG" 2} {"COL" 3 "MIN" 6} {"DAL" 3 "NSH" 5}
              {"NYI" 3 "VAN" 2}]
             (map :scores games)) "Parsed scores")
      (is (= [false]
             (distinct (map #(contains? % :playoff-series) games))))))

  (testing "Parsing scores with games finished, on-going and not yet started"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/games-in-live-preview-and-final-states)))]
      (is (= 7
             (count games)) "Parsed game count")
      (is (= 1
             (count (filter #(= (:state (:status %)) "FINAL") games))) "Parsed finished game count")
      (is (= 2
             (count (filter #(= (:state (:status %)) "LIVE") games))) "Parsed on-going game count")
      (is (= 4
             (count (filter #(= (:state (:status %)) "PREVIEW") games))) "Parsed not started game count")
      (is (= [5 2 5 0 0 0 0]
             (map #(count (:goals %)) games)) "Parsed goal count")
      (is (= [{:away "WSH" :home "CHI"} {:away "FLA" :home "MIN"} {:away "STL" :home "CAR"}
              {:away "TBL" :home "BOS"} {:away "SJS" :home "VAN"} {:away "LAK" :home "ANA"} {:away "NYI" :home "EDM"}]
             (map :teams games)) "Parsed team names")
      (is (= [{"WSH" 2 "CHI" 3} {"FLA" 1 "MIN" 1} {"STL" 3 "CAR" 2}
              {"TBL" 0 "BOS" 0} {"SJS" 0 "VAN" 0} {"LAK" 0 "ANA" 0} {"NYI" 0 "EDM" 0}]
             (map :scores games)) "Parsed scores")
      (is (= [false]
             (distinct (map #(contains? % :playoff-series) games))))))

  (testing "Parsing game with goals in regulation and overtime"
    (let [game (nth
                 (:games
                   (parse-game-scores
                     (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)))
                 4)
          goals (:goals game)]
      (is (= [{:team "EDM" :min 0 :sec 22 :scorer "Connor McDavid" :goal-count 11 :period "1"}
              {:team "BUF" :min 9 :sec 6 :scorer "Cal O'Reilly" :goal-count 1 :period "3"}
              {:team "EDM" :min 3 :sec 48 :scorer "Connor McDavid" :goal-count 12 :period "OT"}]
             goals) "Parsed goals")))

  (testing "Parsing game with goals in regulation and shootout"
    (let [game (nth
                 (:games
                   (parse-game-scores
                     (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)))
                 3)
          goals (map #(dissoc % :strength) (:goals game))]  ; 'strength' field has its own test
      (is (= [{:team "STL" :min 3 :sec 36 :scorer "Dmitrij Jaskin" :goal-count 4 :period "1"}
              {:team "STL" :min 12 :sec 53 :scorer "Jaden Schwartz" :goal-count 5 :period "1"}
              {:team "STL" :min 10 :sec 38 :scorer "Vladimir Tarasenko" :goal-count 30 :period "2"}
              {:team "OTT" :min 12 :sec 32 :scorer "Ryan Dzingel" :goal-count 2 :period "2"}
              {:team "OTT" :min 17 :sec 19 :scorer "Jean-Gabriel Pageau" :goal-count 15 :period "3"}
              {:team "OTT" :min 19 :sec 59 :scorer "Jean-Gabriel Pageau" :goal-count 16 :period "3"}
              {:team "STL" :scorer "Patrik Berglund" :period "SO"}]
             goals) "Parsed goals")))

  (testing "Parsing game with goal in playoff overtime"
    (let [game (nth
                 (:games
                   (parse-game-scores
                     (filter-latest-games resources/playoff-games-finished-in-regulation-and-overtime)))
                 2)]
      (is (= {"CHI" 0 "STL" 1 :overtime true}
             (:scores game)) "Parsed scores")
      (is (= [{:team "STL" :min 9 :sec 4 :scorer "David Backes" :goal-count 1 :period "4"}]
             (:goals game)) "Parsed goals")))

  (testing "Parsing empty net goal information"
    (let [game (nth
                 (:games
                   (parse-game-scores
                     (filter-latest-games resources/playoff-games-finished-in-regulation-and-overtime)))
                 1)
          goals (:goals game)]
      (is (= [false]
             (distinct (map #(contains? % :empty-net) (drop-last goals))))
          "All goals but the last one have no :empty-net field")
      (is (= true
             (:empty-net (last goals)))
          "Last goal has :empty-net field set to true")))

  (testing "Parsing goal strength (even / power play / short handed) information"
    (let [game (nth
                 (:games
                   (parse-game-scores
                     (filter-latest-games resources/playoff-games-finished-in-regulation-and-overtime)))
                 1)
          goals (:goals game)]
      (is (= [nil nil "PPG" "SHG" "PPG" nil nil]
             (map #(:strength %) goals))
          "Parsed goal strengths")
      (is (= [false false true true true false false]
             (map #(contains? % :strength) goals))
          "Even strength goals don't contain :strength field")))

  (testing "Parsing games' statuses"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/games-in-live-preview-and-final-states)))
          statuses (map #(:status %) games)]
      (is (= [{:state "FINAL"}
              {:state "LIVE"
               :progress {:current-period 3,
                          :current-period-ordinal "3rd",
                          :current-period-time-remaining {:pretty "08:58" :min 8 :sec 58}}}
              {:state "LIVE"
               :progress {:current-period 3,
                          :current-period-ordinal "3rd",
                          :current-period-time-remaining {:pretty "END" :min 0 :sec 0}}}
              {:state "PREVIEW"}
              {:state "PREVIEW"}
              {:state "PREVIEW"}
              {:state "PREVIEW"}]
             statuses) "Parsed game statuses")))

  (testing "Parsing games' start times"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/games-in-live-preview-and-final-states)))
          start-times (map #(:start-time %) games)]
      (is (= ["2016-02-28T17:30:00Z"
              "2016-02-28T20:00:00Z"
              "2016-02-28T20:00:00Z"
              "2016-02-28T23:30:00Z"
              "2016-02-29T00:00:00Z"
              "2016-02-29T02:00:00Z"
              "2016-02-29T02:30:00Z"]
             start-times) "Parsed game start times")))

  (testing "Parsing teams' regular season records"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)))
          records (map #(:records %) games)]
      (is (= 9
             (count records)) "Parsed regular season records count")
      (is (= [{"CAR" {:wins 28 :losses 26 :ot 10} "NJD" {:wins 30 :losses 26 :ot 7}}
              {"CGY" {:wins 26 :losses 32 :ot 4} "BOS" {:wins 34 :losses 23 :ot 6}}
              {"PIT" {:wins 32 :losses 21 :ot 8} "WSH" {:wins 45 :losses 12 :ot 4}}
              {"STL" {:wins 36 :losses 20 :ot 9} "OTT" {:wins 30 :losses 27 :ot 6}}
              {"EDM" {:wins 23 :losses 34 :ot 7} "BUF" {:wins 25 :losses 31 :ot 7}}
              {"FLA" {:wins 35 :losses 19 :ot 8} "WPG" {:wins 26 :losses 31 :ot 4}}
              {"COL" {:wins 32 :losses 28 :ot 4} "MIN" {:wins 28 :losses 25 :ot 10}}
              {"DAL" {:wins 38 :losses 19 :ot 7} "NSH" {:wins 31 :losses 21 :ot 11}}
              {"NYI" {:wins 33 :losses 20 :ot 7} "VAN" {:wins 24 :losses 25 :ot 12}}]
             records) "Parsed regular season records")))

  (testing "Parsing teams' playoff records"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/playoff-games-live-finished-with-1st-games)))
          records (map #(:records %) games)]
      (is (= 5
             (count records)) "Parsed regular season records count")
      (is (= [{"NJD" {:wins 0 :losses 0} "TBL" {:wins 0 :losses 0}}
              {"TOR" {:wins 0 :losses 0} "BOS" {:wins 0 :losses 0}}
              {"CBJ" {:wins 0 :losses 0} "WSH" {:wins 0 :losses 0}}
              {"COL" {:wins 0 :losses 0} "NSH" {:wins 0 :losses 0}}
              {"SJS" {:wins 0 :losses 0} "ANA" {:wins 0 :losses 0}}]
             records) "Parsed regular season records")))

  (testing "Parsing playoff series information from playoff games"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/playoff-games-finished-with-2nd-games)))
          playoff-series (map #(:playoff-series %) games)]
      (is (= [{:wins {"DET" 0 "TBL" 1}} {:wins {"NYI" 1 "FLA" 0}} {:wins {"CHI" 0 "STL" 1}} {:wins {"NSH" 0 "ANA" 0}}]
             playoff-series) "Parsed playoff series information")))

  (testing "Parsing playoff series information from first playoff games"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/playoff-games-live-finished-with-1st-games)))
          playoff-series (map #(:playoff-series %) games)]
      (is (= [{:wins {"NJD" 0 "TBL" 0}} {:wins {"TOR" 0 "BOS" 0}} {:wins {"CBJ" 0 "WSH" 0}}
              {:wins {"COL" 0 "NSH" 0}} {:wins {"SJS" 0 "ANA" 0}}]
             playoff-series) "Parsed playoff series information"))))
