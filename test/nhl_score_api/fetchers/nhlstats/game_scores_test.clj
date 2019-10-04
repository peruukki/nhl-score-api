(ns nhl-score-api.fetchers.nhlstats.game-scores-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.fetchers.nhlstats.game-scores :refer :all]
            [nhl-score-api.fetchers.nhlstats.latest-games :refer [filter-latest-games]]
            [nhl-score-api.fetchers.nhlstats.resources :as resources]))

(deftest game-score-json-parsing

  (testing "Parsing scores with games finished in overtime and in shootout"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings)))]
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
                    (filter-latest-games resources/games-in-live-preview-and-final-states)
                    (:records resources/standings)))]
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
                     (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                     (:records resources/standings)))
                 4)
          goals (:goals game)]
      (is (= [{:team "EDM" :min 0 :sec 22 :period "1"
               :scorer {:player "Connor McDavid" :season-total 11}
               :assists [{:player "Jordan Eberle" :season-total 19}]}
              {:team "BUF" :min 9 :sec 6 :period "3"
               :scorer {:player "Cal O'Reilly" :season-total 1}
               :assists [{:player "Sam Reinhart" :season-total 11}
                         {:player "Mark Pysyk" :season-total 5}]}
              {:team "EDM" :min 3 :sec 48 :period "OT"
               :scorer {:player "Connor McDavid" :season-total 12}
               :assists []}]
             goals) "Parsed goals")))

  (testing "Parsing game with goals in regulation and shootout"
    (let [game (nth
                 (:games
                   (parse-game-scores
                     (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                     (:records resources/standings)))
                 3)
          goals (map #(dissoc % :strength) (:goals game))]  ; 'strength' field has its own test
      (is (= [{:team "STL" :min 3 :sec 36 :period "1"
               :scorer {:player "Dmitrij Jaskin" :season-total 4}
               :assists [{:player "Jaden Schwartz" :season-total 7}
                         {:player "Alex Pietrangelo" :season-total 22}]}
              {:team "STL" :min 12 :sec 53 :period "1"
               :scorer {:player "Jaden Schwartz" :season-total 5}
               :assists [{:player "David Backes" :season-total 19}
                         {:player "Kevin Shattenkirk" :season-total 22}]}
              {:team "STL" :min 10 :sec 38 :period "2"
               :scorer {:player "Vladimir Tarasenko" :season-total 30}
               :assists [{:player "Kevin Shattenkirk" :season-total 23}
                         {:player "Jaden Schwartz" :season-total 8}]}
              {:team "OTT" :min 12 :sec 32 :period "2"
               :scorer {:player "Ryan Dzingel" :season-total 2}
               :assists [{:player "Dion Phaneuf" :season-total 27}
                         {:player "Mika Zibanejad" :season-total 26}]}
              {:team "OTT" :min 17 :sec 19 :period "3"
               :scorer {:player "Jean-Gabriel Pageau" :season-total 15}
               :assists [{:player "Mark Stone" :season-total 30}
                         {:player "Erik Karlsson" :season-total 57}]}
              {:team "OTT" :min 19 :sec 59 :period "3"
               :scorer {:player "Jean-Gabriel Pageau" :season-total 16}
               :assists [{:player "Bobby Ryan" :season-total 27}
                         {:player "Zack Smith" :season-total 7}]}
              {:team "STL" :period "SO" :scorer {:player "Patrik Berglund"}}]
             goals) "Parsed goals")))

  (testing "Parsing game with goal in playoff overtime"
    (let [game (nth
                 (:games
                   (parse-game-scores
                     (filter-latest-games resources/playoff-games-finished-in-regulation-and-overtime)
                     (:records resources/standings)))
                 2)]
      (is (= {"CHI" 0 "STL" 1 :overtime true}
             (:scores game)) "Parsed scores")
      (is (= [{:team "STL" :min 9 :sec 4 :period "4"
               :scorer {:player "David Backes" :season-total 1}
               :assists [{:player "Jay Bouwmeester" :season-total 1}
                         {:player "Alex Pietrangelo" :season-total 1}]}]
             (:goals game)) "Parsed goals")))

  (testing "Parsing empty net goal information"
    (let [game (nth
                 (:games
                   (parse-game-scores
                     (filter-latest-games resources/playoff-games-finished-in-regulation-and-overtime)
                     (:records resources/standings)))
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
                     (filter-latest-games resources/playoff-games-finished-in-regulation-and-overtime)
                     (:records resources/standings)))
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
                    (filter-latest-games resources/games-in-live-preview-and-final-states)
                    (:records resources/standings)))
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
                    (filter-latest-games resources/games-in-live-preview-and-final-states)
                    (:records resources/standings)))
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
                    (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings)))
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

  (testing "Parsing teams' current streaks"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings)))
          streaks (map #(:streaks %) games)]
      (is (= 9
             (count streaks)) "Parsed streaks count")
      (is (= [{"CAR" {:type "WINS" :count 1} "NJD" {:type "WINS" :count 1}}
              {"CGY" {:type "WINS" :count 4} "BOS" {:type "WINS" :count 7}}
              {"PIT" {:type "LOSSES" :count 1} "WSH" {:type "WINS" :count 2}}
              {"STL" {:type "LOSSES" :count 1} "OTT" {:type "LOSSES" :count 3}}
              {"EDM" {:type "WINS" :count 1} "BUF" {:type "OT" :count 1}}
              {"FLA" {:type "LOSSES" :count 1} "WPG" {:type "WINS" :count 1}}
              {"COL" {:type "WINS" :count 3} "MIN" {:type "WINS" :count 2}}
              {"DAL" {:type "WINS" :count 1} "NSH" nil}
              {"NYI" {:type "OT" :count 1} "VAN" {:type "OT" :count 1}}]
             streaks) "Parsed streaks")))

  (testing "Parsing teams' standings"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings)))
          standings (map #(:standings %) games)]
      (is (= 9
             (count standings)) "Parsed standings count")
      (is (= [{"CAR" {:points-from-playoff-spot "-1"} "NJD" {:points-from-playoff-spot "-15"}}
              {"CGY" {:points-from-playoff-spot "+20"} "BOS" {:points-from-playoff-spot "+10"}}
              {"PIT" {:points-from-playoff-spot "+1"} "WSH" {:points-from-playoff-spot "+5"}}
              {"STL" {:points-from-playoff-spot "+6"} "OTT" {:points-from-playoff-spot "-22"}}
              {"EDM" {:points-from-playoff-spot "-8"} "BUF" {:points-from-playoff-spot "-7"}}
              {"FLA" {:points-from-playoff-spot "-11"} "WPG" {:points-from-playoff-spot "+15"}}
              {"COL" {:points-from-playoff-spot "-1"} "MIN" {:points-from-playoff-spot "+1"}}
              {"DAL" {:points-from-playoff-spot "+2"} "NSH" {:points-from-playoff-spot "+14"}}
              {"NYI" {:points-from-playoff-spot "+7"} "VAN" {:points-from-playoff-spot "-4"}}]
             standings) "Parsed standings")))

  (testing "Parsing teams' playoff records"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/playoff-games-live-finished-with-1st-games)
                    (:records resources/standings)))
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
                    (filter-latest-games resources/playoff-games-finished-with-2nd-games)
                    (:records resources/standings)))
          playoff-series (map #(:playoff-series %) games)]
      (is (= [{:wins {"DET" 0 "TBL" 1}} {:wins {"NYI" 1 "FLA" 0}} {:wins {"CHI" 0 "STL" 1}} {:wins {"NSH" 0 "ANA" 0}}]
             playoff-series) "Parsed playoff series information")))

  (testing "Parsing playoff series information from first playoff games"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/playoff-games-live-finished-with-1st-games)
                    (:records resources/standings)))
          playoff-series (map #(:playoff-series %) games)]
      (is (= [{:wins {"NJD" 0 "TBL" 0}} {:wins {"TOR" 0 "BOS" 0}} {:wins {"CBJ" 0 "WSH" 0}}
              {:wins {"COL" 0 "NSH" 0}} {:wins {"SJS" 0 "ANA" 0}}]
             playoff-series) "Parsed playoff series information"))))
