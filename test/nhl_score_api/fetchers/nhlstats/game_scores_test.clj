(ns nhl-score-api.fetchers.nhlstats.game-scores-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.fetchers.nhlstats.game-scores :refer :all]
            [nhl-score-api.fetchers.nhlstats.transformer :refer [get-latest-games]]
            [nhl-score-api.fetchers.nhlstats.resources :as resources]
            [nhl-score-api.utils :refer [fmap-vals]]))

(deftest game-scores-parsing-scores

  (testing "Parsing scores with games finished in overtime and in shootout"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings)))]
      (is (= 9
             (count games)) "Parsed game count")
      (is (= [4 3 5 7 3 5 9 8 5]
             (map #(count (:goals %)) games)) "Parsed goal count")
      (is (= [{:away {:abbreviation "CAR" :id 12 :location-name "Carolina" :short-name "Carolina" :team-name "Hurricanes"}
               :home {:abbreviation "NJD" :id 1 :location-name "New Jersey" :short-name "New Jersey" :team-name "Devils"}}
              {:away {:abbreviation "CGY" :id 20 :location-name "Calgary" :short-name "Calgary" :team-name "Flames"}
               :home {:abbreviation "BOS" :id 6 :location-name "Boston" :short-name "Boston" :team-name "Bruins"}}
              {:away {:abbreviation "PIT" :id 5 :location-name "Pittsburgh" :short-name "Pittsburgh" :team-name "Penguins"}
               :home {:abbreviation "WSH" :id 15 :location-name "Washington" :short-name "Washington" :team-name "Capitals"}}
              {:away {:abbreviation "STL" :id 19 :location-name "St. Louis" :short-name "St Louis" :team-name "Blues"}
               :home {:abbreviation "OTT" :id 9 :location-name "Ottawa" :short-name "Ottawa" :team-name "Senators"}}
              {:away {:abbreviation "EDM" :id 22 :location-name "Edmonton" :short-name "Edmonton" :team-name "Oilers"}
               :home {:abbreviation "BUF" :id 7 :location-name "Buffalo" :short-name "Buffalo" :team-name "Sabres"}}
              {:away {:abbreviation "FLA" :id 13 :location-name "Florida" :short-name "Florida" :team-name "Panthers"}
               :home {:abbreviation "WPG" :id 52 :location-name "Winnipeg" :short-name "Winnipeg" :team-name "Jets"}}
              {:away {:abbreviation "COL" :id 21 :location-name "Colorado" :short-name "Colorado" :team-name "Avalanche"}
               :home {:abbreviation "MIN" :id 30 :location-name "Minnesota" :short-name "Minnesota" :team-name "Wild"}}
              {:away {:abbreviation "DAL" :id 25 :location-name "Dallas" :short-name "Dallas" :team-name "Stars"}
               :home {:abbreviation "NSH" :id 18 :location-name "Nashville" :short-name "Nashville" :team-name "Predators"}}
              {:away {:abbreviation "NYI" :id 2 :location-name "New York" :short-name "NY Islanders" :team-name "Islanders"}
               :home {:abbreviation "VAN" :id 23 :location-name "Vancouver" :short-name "Vancouver" :team-name "Canucks"}}]
             (map :teams games)) "Parsed teams")
      (is (= [{"CAR" 3 "NJD" 1} {"CGY" 1 "BOS" 2} {"PIT" 2 "WSH" 3} {"STL" 4 "OTT" 3 :shootout true}
              {"EDM" 2 "BUF" 1 :overtime true} {"FLA" 3 "WPG" 2} {"COL" 3 "MIN" 6} {"DAL" 3 "NSH" 5}
              {"NYI" 3 "VAN" 2}]
             (map :scores games)) "Parsed scores")
      (is (= [false]
             (distinct (map #(contains? % :playoff-series) games))))))

  (testing "Parsing scores with games finished, on-going and not yet started"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/games-in-live-preview-and-final-states)
                    (:records resources/standings)))]
      (is (= 7
             (count games)) "Parsed game count")
      (is (= 1
             (count (filter #(= (:state (:status %)) "FINAL") games))) "Parsed finished game count")
      (is (= 2
             (count (filter #(= (:state (:status %)) "LIVE") games))) "Parsed on-going game count")
      (is (= 3
             (count (filter #(= (:state (:status %)) "PREVIEW") games))) "Parsed not started game count")
      (is (= 1
             (count (filter #(= (:state (:status %)) "POSTPONED") games))) "Parsed postponed game count")
      (is (= [5 2 5 0 0 0 0]
             (map #(count (:goals %)) games)) "Parsed goal count")
      (is (= [{:away {:abbreviation "WSH" :id 15 :location-name "Washington" :short-name "Washington" :team-name "Capitals"}
               :home {:abbreviation "CHI" :id 16 :location-name "Chicago" :short-name "Chicago" :team-name "Blackhawks"}}
              {:away {:abbreviation "FLA" :id 13 :location-name "Florida" :short-name "Florida" :team-name "Panthers"}
               :home {:abbreviation "MIN" :id 30 :location-name "Minnesota" :short-name "Minnesota" :team-name "Wild"}}
              {:away {:abbreviation "STL" :id 19 :location-name "St. Louis" :short-name "St Louis" :team-name "Blues"}
               :home {:abbreviation "CAR" :id 12 :location-name "Carolina" :short-name "Carolina" :team-name "Hurricanes"}}
              {:away {:abbreviation "TBL" :id 14 :location-name "Tampa Bay" :short-name "Tampa Bay" :team-name "Lightning"}
               :home {:abbreviation "BOS" :id 6 :location-name "Boston" :short-name "Boston" :team-name "Bruins"}}
              {:away {:abbreviation "SJS" :id 28 :location-name "San Jose" :short-name "San Jose" :team-name "Sharks"}
               :home {:abbreviation "VAN" :id 23 :location-name "Vancouver" :short-name "Vancouver" :team-name "Canucks"}}
              {:away {:abbreviation "LAK" :id 26 :location-name "Los Angeles" :short-name "Los Angeles" :team-name "Kings"}
               :home {:abbreviation "ANA" :id 24 :location-name "Anaheim" :short-name "Anaheim" :team-name "Ducks"}}
              {:away {:abbreviation "NYI" :id 2 :location-name "New York" :short-name "NY Islanders" :team-name "Islanders"}
               :home {:abbreviation "EDM" :id 22 :location-name "Edmonton" :short-name "Edmonton" :team-name "Oilers"}}]
             (map :teams games)) "Parsed teams")
      (is (= [{"WSH" 2 "CHI" 3} {"FLA" 1 "MIN" 1} {"STL" 3 "CAR" 2}
              {"TBL" 0 "BOS" 0} {"SJS" 0 "VAN" 0} {"LAK" 0 "ANA" 0} {"NYI" 0 "EDM" 0}]
             (map :scores games)) "Parsed scores")
      (is (= [false]
             (distinct (map #(contains? % :playoff-series) games)))))))

(deftest game-scores-parsing-games

  (testing "Parsing game with goals in regulation and overtime"
    (let [game (nth
                 (:games
                   (parse-game-scores
                     (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
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
                     (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
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
                     (get-latest-games resources/playoff-games-finished-in-regulation-and-overtime)
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
                     (get-latest-games resources/playoff-games-finished-in-regulation-and-overtime)
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
                     (get-latest-games resources/playoff-games-finished-in-regulation-and-overtime)
                     (:records resources/standings)))
                 1)
          goals (:goals game)]
      (is (= [nil nil "PPG" "SHG" "PPG" nil nil]
             (map #(:strength %) goals))
          "Parsed goal strengths")
      (is (= [false false true true true false false]
             (map #(contains? % :strength) goals))
          "Even strength goals don't contain :strength field"))))

(deftest game-scores-parsing-game-statuses

  (testing "Parsing games' statuses"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/games-in-live-preview-and-final-states)
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
              {:state "POSTPONED"}]
             statuses) "Parsed game statuses"))))

(deftest game-scores-parsing-game-start-times

  (testing "Parsing games' start times"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/games-in-live-preview-and-final-states)
                    (:records resources/standings)))
          start-times (map #(:start-time %) games)]
      (is (= ["2016-02-28T17:30:00Z"
              "2016-02-28T20:00:00Z"
              "2016-02-28T20:00:00Z"
              "2016-02-28T23:30:00Z"
              "2016-02-29T00:00:00Z"
              "2016-02-29T02:00:00Z"
              "2016-02-29T02:30:00Z"]
             start-times) "Parsed game start times"))))

(deftest game-scores-parsing-team-records

  (testing "Parsing teams' pre-game regular season records"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings)))
          records (map #(:records (:pre-game-stats %)) games)]
      (is (= 9
             (count records)) "Parsed pre-game regular season records count")
      (is (= [{"CAR" {:wins 28 :losses 26 :ot 10} "NJD" {:wins 30 :losses 26 :ot 7}}
              {"CGY" {:wins 26 :losses 32 :ot 4} "BOS" {:wins 34 :losses 23 :ot 6}}
              {"PIT" {:wins 32 :losses 21 :ot 8} "WSH" {:wins 45 :losses 12 :ot 4}}
              {"STL" {:wins 36 :losses 20 :ot 9} "OTT" {:wins 30 :losses 27 :ot 6}}
              {"EDM" {:wins 23 :losses 34 :ot 7} "BUF" {:wins 25 :losses 31 :ot 7}}
              {"FLA" {:wins 35 :losses 19 :ot 8} "WPG" {:wins 26 :losses 31 :ot 4}}
              {"COL" {:wins 32 :losses 28 :ot 4} "MIN" {:wins 28 :losses 25 :ot 10}}
              {"DAL" {:wins 38 :losses 19 :ot 7} "NSH" {:wins 31 :losses 21 :ot 11}}
              {"NYI" {:wins 33 :losses 20 :ot 7} "VAN" {:wins 24 :losses 25 :ot 12}}]
             records) "Parsed pre-game regular season records")))

  (testing "Parsing teams' current regular season records"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings)))
          records (map #(:records (:current-stats %)) games)]
      (is (= 9
             (count records)) "Parsed current regular season records count")
      (is (= [{"CAR" {:wins 29 :losses 26 :ot 10} "NJD" {:wins 30 :losses 27 :ot 7}}
              {"CGY" {:wins 26 :losses 33 :ot 4} "BOS" {:wins 35 :losses 23 :ot 6}}
              {"PIT" {:wins 32 :losses 22 :ot 8} "WSH" {:wins 46 :losses 12 :ot 4}}
              {"STL" {:wins 37 :losses 20 :ot 9} "OTT" {:wins 30 :losses 27 :ot 7}}
              {"EDM" {:wins 24 :losses 34 :ot 7} "BUF" {:wins 25 :losses 31 :ot 8}}
              {"FLA" {:wins 36 :losses 19 :ot 8} "WPG" {:wins 26 :losses 32 :ot 4}}
              {"COL" {:wins 32 :losses 29 :ot 4} "MIN" {:wins 29 :losses 25 :ot 10}}
              {"DAL" {:wins 38 :losses 20 :ot 7} "NSH" {:wins 32 :losses 21 :ot 11}}
              {"NYI" {:wins 34 :losses 20 :ot 7} "VAN" {:wins 24 :losses 26 :ot 12}}]
             records) "Parsed current regular season records"))))

(deftest game-scores-parsing-team-streaks

  (testing "Parsing teams' current streaks"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings)))
          streaks (map #(:streaks (:current-stats %)) games)]
      (is (= 9
             (count streaks)) "Parsed streaks count")
      (is (= [{"CAR" {:type "WINS" :count 3} "NJD" {:type "LOSSES" :count 1}}
              {"CGY" {:type "LOSSES" :count 1} "BOS" {:type "WINS" :count 1}}
              {"PIT" {:type "WINS" :count 1} "WSH" {:type "OT" :count 1}}
              {"STL" {:type "WINS" :count 1} "OTT" {:type "LOSSES" :count 2}}
              {"EDM" {:type "LOSSES" :count 1} "BUF" {:type "WINS" :count 1}}
              {"FLA" {:type "WINS" :count 2} "WPG" {:type "WINS" :count 4}}
              {"COL" {:type "WINS" :count 1} "MIN" {:type "WINS" :count 1}}
              {"DAL" {:type "LOSSES" :count 3} "NSH" {:type "WINS" :count 3}}
              {"NYI" {:type "OT" :count 2} "VAN" {:type "WINS" :count 1}}]
             streaks) "Parsed streaks"))))

(deftest game-scores-parsing-team-ranks

  (testing "Parsing teams' division and league ranks for regular season games"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings)))
          pre-game-stats-standings (map #(:standings (:pre-game-stats %)) games)
          current-stats-standings (map #(:standings (:current-stats %)) games)
          ranks (map
                  #(fmap-vals (fn [team-stats] (select-keys team-stats [:division-rank :league-rank])) %)
                  current-stats-standings)]
      (is (= (repeat 9 nil) pre-game-stats-standings) "Pre-game standings should not exist")
      (is (= [{"CAR" {:division-rank "4" :league-rank "9"} "NJD" {:division-rank "8" :league-rank "26"}}
              {"CGY" {:division-rank "4" :league-rank "19"} "BOS" {:division-rank "1" :league-rank "1"}}
              {"PIT" {:division-rank "3" :league-rank "7"} "WSH" {:division-rank "1" :league-rank "5"}}
              {"STL" {:division-rank "1" :league-rank "2"} "OTT" {:division-rank "7" :league-rank "30"}}
              {"EDM" {:division-rank "2" :league-rank "12"} "BUF" {:division-rank "6" :league-rank "25"}}
              {"FLA" {:division-rank "4" :league-rank "15"} "WPG" {:division-rank "5" :league-rank "20"}}
              {"COL" {:division-rank "2" :league-rank "3"} "MIN" {:division-rank "6" :league-rank "21"}}
              {"DAL" {:division-rank "3" :league-rank "10"} "NSH" {:division-rank "4" :league-rank "16"}}
              {"NYI" {:division-rank "5" :league-rank "11"} "VAN" {:division-rank "3" :league-rank "17"}}]
             ranks) "Parsed current stats division and league ranks")))

  (testing "Parsing teams' division and league ranks for playoff games"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/playoff-games-finished-in-regulation-and-overtime)
                    (:records resources/standings)))
          pre-game-stats-standings (map #(:standings (:pre-game-stats %)) games)
          current-stats-standings (map #(:standings (:current-stats %)) games)
          ranks (map
                  #(fmap-vals (fn [team-stats] (select-keys team-stats [:division-rank :league-rank])) %)
                  current-stats-standings)]
      (is (= pre-game-stats-standings current-stats-standings) "Parsed standings, pre-game vs. current stats")
      (is (= [{"DET" {:division-rank "8" :league-rank "31"} "TBL" {:division-rank "2" :league-rank "4"}}
              {"NYR" {:division-rank "7" :league-rank "18"} "PIT" {:division-rank "3" :league-rank "7"}}
              {"CHI" {:division-rank "7" :league-rank "23"} "STL" {:division-rank "1" :league-rank "2"}}]
             ranks) "Parsed current stats division and league ranks"))))

; TODO: Enable this again when "normal" playoff spot logic is resumed
(deftest ^:skip game-scores-parsing-team-points-from-playoff-spot

  (testing "Parsing teams' points from playoff spot"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings-playoff-spots-per-division-5-3-4-4)))
          standings (map #(:standings (:current-stats %)) games)
          points-from-playoff-spot (map
                                     #(fmap-vals (fn [team-stats] (select-keys team-stats [:points-from-playoff-spot])) %)
                                     standings)]
      (is (= 9
             (count points-from-playoff-spot)) "Parsed points from playoff spot count")
      ; The test standings are:
      ;
      ; Eastern conference                       Western conference
      ;
      ; Metropolitan                             Central
      ; 1 WSH 79 pts                             1 STL 74 pts
      ; 2 PIT 76 pts                             2 DAL 73 pts
      ; 3 NYI 72 pts                             3 COL 72 pts
      ; 4 CBJ 71 pts, 1st wild card              4 NSH 65 pts, 2nd wild card
      ; 5 PHI 71 pts, 2nd wild card              ------------
      ; ------------                             5 WPG 63 pts
      ; 6 CAR 69 pts, 1st out of the playoffs    6 MIN 61 pts
      ; 7 NYR 64 pts                             7 CHI 60 pts
      ; 8 NJD 52 pts
      ;
      ; Atlantic                                 Pacific
      ; 1 BOS 84 pts                             1 VAN 69 pts
      ; 2 TBL 83 pts                             2 EDM 68 pts
      ; 3 TOR 70 pts                             3 VGK 68 pts
      ; ------------                             4 CGY 66 pts, 1st wild card
      ; 4 FLA 66 pts                             ------------
      ; 5 MTL 62 pts                             5 ARI 64 pts, 1st out of the playoffs
      ; 6 BUF 60 pts                             6 SJS 56 pts
      ; 7 OTT 49 pts                             7 ANA 53 pts
      ; 8 DET 32 pts                             8 LAK 47 pts
      (is (= [{"CAR" {:points-from-playoff-spot "-2"} "NJD" {:points-from-playoff-spot "-19"}}
              {"CGY" {:points-from-playoff-spot "+2"} "BOS" {:points-from-playoff-spot "+18"}}
              {"PIT" {:points-from-playoff-spot "+7"} "WSH" {:points-from-playoff-spot "+10"}}
              {"STL" {:points-from-playoff-spot "+10"} "OTT" {:points-from-playoff-spot "-21"}}
              {"EDM" {:points-from-playoff-spot "+4"} "BUF" {:points-from-playoff-spot "-10"}}
              {"FLA" {:points-from-playoff-spot "-4"} "WPG" {:points-from-playoff-spot "-2"}}
              {"COL" {:points-from-playoff-spot "+8"} "MIN" {:points-from-playoff-spot "-4"}}
              {"DAL" {:points-from-playoff-spot "+9"} "NSH" {:points-from-playoff-spot "+1"}}
              {"NYI" {:points-from-playoff-spot "+3"} "VAN" {:points-from-playoff-spot "+5"}}]
             points-from-playoff-spot) "Parsed points from playoff spot"))))

(deftest game-scores-parsing-team-playoff-records

  (testing "Parsing teams' pre-game playoff records when teams have no OT loss values in their records"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/playoff-games-live-finished-with-1st-games)
                    (:records resources/standings)))
          records (map #(:records (:pre-game-stats %)) games)]
      (is (= 5
             (count records)) "Parsed pre-game playoff records count")
      (is (= [{"NJD" {:wins 0 :losses 0} "TBL" {:wins 0 :losses 0}}
              {"TOR" {:wins 0 :losses 0} "BOS" {:wins 0 :losses 0}}
              {"CBJ" {:wins 0 :losses 0} "WSH" {:wins 0 :losses 0}}
              {"COL" {:wins 0 :losses 0} "NSH" {:wins 0 :losses 0}}
              {"SJS" {:wins 0 :losses 0} "ANA" {:wins 0 :losses 0}}]
             records) "Parsed pre-game playoff records")))

  (testing "Parsing teams' pre-game playoff records when teams have OT loss values in their records"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/playoff-games-with-ot-losses-in-records)
                    (:records resources/standings)))
          records (map #(:records (:pre-game-stats %)) games)]
      (is (= 5
             (count records)) "Parsed pre-game playoff records count")
      (is (= [{"CAR" {:wins 0 :losses 0 :ot 0} "NYR" {:wins 0 :losses 0 :ot 0}}
              {"CHI" {:wins 0 :losses 0 :ot 0} "EDM" {:wins 0 :losses 0 :ot 0}}
              {"FLA" {:wins 0 :losses 0 :ot 0} "NYI" {:wins 0 :losses 0 :ot 0}}
              {"MTL" {:wins 0 :losses 0 :ot 0} "PIT" {:wins 0 :losses 0 :ot 0}}
              {"CGY" {:wins 0 :losses 0 :ot 0} "WPG" {:wins 0 :losses 0 :ot 0}}]
             records) "Parsed pre-game playoff records")))

  (testing "Parsing teams' current playoff records"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/playoff-games-live-finished-with-1st-games)
                    (:records resources/standings)))
          records (map #(:records (:current-stats %)) games)]
      (is (= 5
             (count records)) "Parsed current playoff records count")
      (is (= [{"NJD" {:wins 0 :losses 1} "TBL" {:wins 1 :losses 0}}
              {"TOR" {:wins 0 :losses 1} "BOS" {:wins 1 :losses 0}}
              {"CBJ" {:wins 1 :losses 0} "WSH" {:wins 0 :losses 1}}
              {"COL" {:wins 0 :losses 0} "NSH" {:wins 0 :losses 0}}
              {"SJS" {:wins 0 :losses 0} "ANA" {:wins 0 :losses 0}}]
             records) "Parsed current playoff records"))))

(deftest game-scores-parsing-playoff-series

  (testing "Parsing pre-game playoff series wins from playoff games"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/playoff-games-finished-with-2nd-games)
                    (:records resources/standings)))
          playoff-series (map #(:playoff-series (:pre-game-stats %)) games)
          wins (map :wins playoff-series)]
      (is (= [{"DET" 0 "TBL" 1} {"NYI" 1 "FLA" 0} {"CHI" 0 "STL" 1} {"NSH" 0 "ANA" 0}]
             wins) "Parsed pre-game playoff series wins")))

  (testing "Parsing current playoff series wins from playoff games"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/playoff-games-finished-with-2nd-games)
                    (:records resources/standings)))
          playoff-series (map #(:playoff-series (:current-stats %)) games)
          wins (map :wins playoff-series)]
      (is (= [{"DET" 0 "TBL" 2} {"NYI" 1 "FLA" 1} {"CHI" 1 "STL" 1} {"NSH" 1 "ANA" 0}]
             wins) "Parsed current playoff series wins")))

  (testing "Parsing pre-game playoff series wins from first playoff games"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/playoff-games-live-finished-with-1st-games)
                    (:records resources/standings)))
          playoff-series (map #(:playoff-series (:pre-game-stats %)) games)
          wins (map :wins playoff-series)]
      (is (= [{"NJD" 0 "TBL" 0}                             ; finished & up to date
              {"TOR" 0 "BOS" 0}                             ; finished & up to date
              {"CBJ" 0 "WSH" 0}                             ; finished & current game missing from seriesRecord
              {"COL" 0 "NSH" 0}                             ; in-progress & up to date
              {"SJS" 0 "ANA" 0}]                            ; in-progress & current game included in seriesRecord
             wins) "Parsed pre-game playoff series wins")))

  (testing "Parsing current playoff series wins from first playoff games"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/playoff-games-live-finished-with-1st-games)
                    (:records resources/standings)))
          playoff-series (map #(:playoff-series (:current-stats %)) games)
          wins (map :wins playoff-series)]
      (is (= [{"NJD" 0 "TBL" 1}                             ; finished & up to date
              {"TOR" 0 "BOS" 1}                             ; finished & up to date
              {"CBJ" 1 "WSH" 0}                             ; finished & current game missing from seriesRecord
              {"COL" 0 "NSH" 0}                             ; in-progress & up to date
              {"SJS" 0 "ANA" 0}]                            ; in-progress & current game included in seriesRecord
             wins) "Parsed current playoff series wins")))

  (testing "Parsing playoff rounds from playoff games"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/playoff-games-live-finished-with-1st-games)
                    (:records resources/standings)))
          pre-game-stats-playoff-series (map #(:playoff-series (:pre-game-stats %)) games)
          current-stats-playoff-series (map #(:playoff-series (:current-stats %)) games)
          pre-game-stats-rounds (map :round pre-game-stats-playoff-series)
          current-stats-rounds (map :round current-stats-playoff-series)]
      (is (= [1 1 1 1 1]
             pre-game-stats-rounds) "Parsed pre-game stats playoff rounds")
      (is (= [1 1 1 1 1]
             current-stats-rounds) "Parsed current stats playoff rounds"))))

(deftest game-scores-pre-game-stats
  (testing "Not including teams' pre-game stats"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings)
                    false))]
      (is (= [false]
             (distinct (map #(contains? % :pre-game-stats) games)))
          "Pre-game stats are not included"))))

(deftest game-scores-validation

  (testing "Validating valid game with goals"
    (let [game (first (:games
                        (parse-game-scores
                          (get-latest-games resources/games-in-live-preview-and-final-states)
                          (:records resources/standings))))]
      (is (= (contains? game :errors) false) "No validation errors")))

  (testing "Validating valid game without goals"
    (let [game (nth (:games
                        (parse-game-scores
                          (get-latest-games resources/games-in-live-preview-and-final-states)
                          (:records resources/standings)))
                    3)]
      (is (= (contains? game :errors) false) "No validation errors")))

  (testing "Validating valid non-finished game with multiple shootout goals"
    (let [game (nth (:games
                      (parse-game-scores
                        (get-latest-games resources/games-for-validation-testing)
                        (:records resources/standings)))
                    4)]
      (is (= (contains? game :errors) false) "No validation errors")))

  (testing "Validating valid finished game with multiple shootout goals"
    (let [game (nth (:games
                      (parse-game-scores
                        (get-latest-games resources/games-for-validation-testing)
                        (:records resources/standings)))
                    3)]
      (is (= (contains? game :errors) false) "No validation errors")))

  (testing "Validating game missing all goals"
    (let [game (first (:games
                        (parse-game-scores
                          (get-latest-games resources/games-for-validation-testing)
                          (:records resources/standings))))]
      (is (= (contains? game :errors) true) "Contains validation errors")
      (is (= [{:error :MISSING-ALL-GOALS}]
             (:errors game)) "Errors contain 'missing all goals' error")))

  (testing "Validating game missing one goal"
    (let [game (second (:games
                        (parse-game-scores
                          (get-latest-games resources/games-for-validation-testing)
                          (:records resources/standings))))]
      (is (= (contains? game :errors) true) "Contains validation errors")
      (is (= [{:error :SCORE-AND-GOAL-COUNT-MISMATCH :details {:goal-count 4 :score-count 5}}]
             (:errors game)) "Errors contain expected 'score and goal count mismatch' error")))

  (testing "Validating game having one goal too many"
    (let [game (nth (:games
                        (parse-game-scores
                          (get-latest-games resources/games-for-validation-testing)
                          (:records resources/standings)))
                    2)]
      (is (= (contains? game :errors) true) "Contains validation errors")
      (is (= [{:error :SCORE-AND-GOAL-COUNT-MISMATCH :details {:goal-count 5 :score-count 3}}]
             (:errors game)) "Errors contain expected 'score and goal count mismatch' error"))))
