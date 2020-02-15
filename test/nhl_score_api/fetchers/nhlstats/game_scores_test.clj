(ns nhl-score-api.fetchers.nhlstats.game-scores-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.fetchers.nhlstats.game-scores :refer :all]
            [nhl-score-api.fetchers.nhlstats.latest-games :refer [filter-latest-games]]
            [nhl-score-api.fetchers.nhlstats.resources :as resources]
            [nhl-score-api.utils :refer [fmap]]))

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
      (is (= [{:away {:abbreviation "CAR" :id 12} :home {:abbreviation "NJD" :id 1}}
              {:away {:abbreviation "CGY" :id 20} :home {:abbreviation "BOS" :id 6}}
              {:away {:abbreviation "PIT" :id 5} :home {:abbreviation "WSH" :id 15}}
              {:away {:abbreviation "STL" :id 19} :home {:abbreviation "OTT" :id 9}}
              {:away {:abbreviation "EDM" :id 22} :home {:abbreviation "BUF" :id 7}}
              {:away {:abbreviation "FLA" :id 13} :home {:abbreviation "WPG" :id 52}}
              {:away {:abbreviation "COL" :id 21} :home {:abbreviation "MIN" :id 30}}
              {:away {:abbreviation "DAL" :id 25} :home {:abbreviation "NSH" :id 18}}
              {:away {:abbreviation "NYI" :id 2} :home {:abbreviation "VAN" :id 23}}]
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
      (is (= [{:away {:abbreviation "WSH" :id 15} :home {:abbreviation "CHI" :id 16}}
              {:away {:abbreviation "FLA" :id 13} :home {:abbreviation "MIN" :id 30}}
              {:away {:abbreviation "STL" :id 19} :home {:abbreviation "CAR" :id 12}}
              {:away {:abbreviation "TBL" :id 14} :home {:abbreviation "BOS" :id 6}}
              {:away {:abbreviation "SJS" :id 28} :home {:abbreviation "VAN" :id 23}}
              {:away {:abbreviation "LAK" :id 26} :home {:abbreviation "ANA" :id 24}}
              {:away {:abbreviation "NYI" :id 2} :home {:abbreviation "EDM" :id 22}}]
             (map :teams games)) "Parsed teams")
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

  (testing "Parsing teams' pre-game regular season records"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
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
                    (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
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
             records) "Parsed current regular season records")))

  (testing "Parsing teams' current streaks"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings)))
          streaks (map #(:streaks (:current-stats %)) games)]
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

  (testing "Parsing teams' league ranks"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings)))
          standings (map #(:standings (:current-stats %)) games)
          league-ranks (map
                         #(fmap (fn [team-stats] (select-keys team-stats [:league-rank])) %)
                         standings)]
      (is (= 9
             (count league-ranks)) "Parsed league ranks count")
      (is (= [{"CAR" {:league-rank "13"} "NJD" {:league-rank "28"}}
              {"CGY" {:league-rank "2"} "BOS" {:league-rank "4"}}
              {"PIT" {:league-rank "12"} "WSH" {:league-rank "9"}}
              {"STL" {:league-rank "14"} "OTT" {:league-rank "31"}}
              {"EDM" {:league-rank "27"} "BUF" {:league-rank "17"}}
              {"FLA" {:league-rank "23"} "WPG" {:league-rank "5"}}
              {"COL" {:league-rank "19"} "MIN" {:league-rank "18"}}
              {"DAL" {:league-rank "16"} "NSH" {:league-rank "7"}}
              {"NYI" {:league-rank "6"} "VAN" {:league-rank "25"}}]
             league-ranks) "Parsed league ranks")))

  (testing "Parsing teams' points from playoff spot"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/games-finished-in-regulation-overtime-and-shootout)
                    (:records resources/standings)))
          standings (map #(:standings (:current-stats %)) games)
          points-from-playoff-spot (map
                                     #(fmap (fn [team-stats] (select-keys team-stats [:points-from-playoff-spot])) %)
                                     standings)]
      (is (= 9
             (count points-from-playoff-spot)) "Parsed points from playoff spot count")
      (is (= [{"CAR" {:points-from-playoff-spot "-1"} "NJD" {:points-from-playoff-spot "-15"}}
              {"CGY" {:points-from-playoff-spot "+20"} "BOS" {:points-from-playoff-spot "+10"}}
              {"PIT" {:points-from-playoff-spot "+1"} "WSH" {:points-from-playoff-spot "+5"}}
              {"STL" {:points-from-playoff-spot "+6"} "OTT" {:points-from-playoff-spot "-22"}}
              {"EDM" {:points-from-playoff-spot "-8"} "BUF" {:points-from-playoff-spot "-7"}}
              {"FLA" {:points-from-playoff-spot "-11"} "WPG" {:points-from-playoff-spot "+15"}}
              {"COL" {:points-from-playoff-spot "-1"} "MIN" {:points-from-playoff-spot "+1"}}
              {"DAL" {:points-from-playoff-spot "+2"} "NSH" {:points-from-playoff-spot "+14"}}
              {"NYI" {:points-from-playoff-spot "+7"} "VAN" {:points-from-playoff-spot "-4"}}]
             points-from-playoff-spot) "Parsed points from playoff spot")))

  (testing "Parsing teams' pre-game playoff records"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/playoff-games-live-finished-with-1st-games)
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

  (testing "Parsing teams' current playoff records"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/playoff-games-live-finished-with-1st-games)
                    (:records resources/standings)))
          records (map #(:records (:current-stats %)) games)]
      (is (= 5
             (count records)) "Parsed current playoff records count")
      (is (= [{"NJD" {:wins 0 :losses 1} "TBL" {:wins 1 :losses 0}}
              {"TOR" {:wins 0 :losses 1} "BOS" {:wins 1 :losses 0}}
              {"CBJ" {:wins 1 :losses 0} "WSH" {:wins 0 :losses 1}}
              {"COL" {:wins 0 :losses 1} "NSH" {:wins 1 :losses 0}}
              {"SJS" {:wins 0 :losses 0} "ANA" {:wins 0 :losses 0}}]
             records) "Parsed current playoff records")))

  (testing "Parsing pre-game playoff series information from playoff games"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/playoff-games-finished-with-2nd-games)
                    (:records resources/standings)))
          playoff-series (map #(:playoff-series (:pre-game-stats %)) games)]
      (is (= [{:wins {"DET" 0 "TBL" 1}} {:wins {"NYI" 1 "FLA" 0}} {:wins {"CHI" 0 "STL" 1}} {:wins {"NSH" 0 "ANA" 0}}]
             playoff-series) "Parsed pre-game playoff series information")))

  (testing "Parsing current playoff series information from playoff games"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/playoff-games-finished-with-2nd-games)
                    (:records resources/standings)))
          playoff-series (map #(:playoff-series (:current-stats %)) games)]
      (is (= [{:wins {"DET" 0 "TBL" 2}} {:wins {"NYI" 1 "FLA" 1}} {:wins {"CHI" 1 "STL" 1}} {:wins {"NSH" 1 "ANA" 0}}]
             playoff-series) "Parsed current playoff series information")))

  (testing "Parsing pre-game playoff series information from first playoff games"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/playoff-games-live-finished-with-1st-games)
                    (:records resources/standings)))
          playoff-series (map #(:playoff-series (:pre-game-stats %)) games)]
      (is (= [{:wins {"NJD" 0 "TBL" 0}} {:wins {"TOR" 0 "BOS" 0}} {:wins {"CBJ" 0 "WSH" 0}}
              {:wins {"COL" 0 "NSH" 0}} {:wins {"SJS" 0 "ANA" 0}}]
             playoff-series) "Parsed pre-game playoff series information")))

  (testing "Parsing current playoff series information from first playoff games"
    (let [games (:games
                  (parse-game-scores
                    (filter-latest-games resources/playoff-games-live-finished-with-1st-games)
                    (:records resources/standings)))
          playoff-series (map #(:playoff-series (:current-stats %)) games)]
      (is (= [{:wins {"NJD" 0 "TBL" 1}} {:wins {"TOR" 0 "BOS" 1}} {:wins {"CBJ" 1 "WSH" 0}}
              {:wins {"COL" 0 "NSH" 1}} {:wins {"SJS" 0 "ANA" 0}}]
             playoff-series) "Parsed current playoff series information"))))
