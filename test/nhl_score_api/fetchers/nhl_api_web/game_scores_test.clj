(ns nhl-score-api.fetchers.nhl-api-web.game-scores-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.fetchers.nhl-api-web.game-scores :refer :all]
            [nhl-score-api.fetchers.nhl-api-web.transformer :refer [get-latest-games]]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]
            [nhl-score-api.utils :refer [fmap-vals]]))

(def default-games resources/games-finished-in-regulation-overtime-and-shootout)
(def default-landings (resources/get-landings [2023020195 2023020205 2023020206 2023020207 2023020208 2023020209]))
(def default-standings (:standings resources/standings))

(deftest game-scores-parsing-scores

  (testing "Parsing scores with games finished in OT and SO, some on-going and some not yet started"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games default-games)
                    default-standings
                    default-landings))]
      (is (= 6
             (count games)) "Parsed game count")
      (is (= 3
             (count (filter #(= (:state (:status %)) "FINAL") games))) "Parsed finished game count")
      (is (= 1
             (count (filter #(= (:state (:status %)) "LIVE") games))) "Parsed on-going game count")
      (is (= 2
             (count (filter #(= (:state (:status %)) "PREVIEW") games))) "Parsed not started game count")
      (is (= 0
             (count (filter #(= (:state (:status %)) "POSTPONED") games))) "Parsed postponed game count")
      (is (= [5 9 5 9 0 0]
             (map #(count (:goals %)) games)) "Parsed goal count")
      (is (= [{:away {:abbreviation "MTL" :id 8 :location-name "Montréal" :short-name "Montréal" :team-name "Canadiens"}
               :home {:abbreviation "DET" :id 17 :location-name "Detroit" :short-name "Detroit" :team-name "Red Wings"}}
              {:away {:abbreviation "CGY" :id 20 :location-name "Calgary" :short-name "Calgary" :team-name "Flames"}
               :home {:abbreviation "TOR" :id 10 :location-name "Toronto" :short-name "Toronto" :team-name "Maple Leafs"}}
              {:away {:abbreviation "SJS" :id 28 :location-name "San Jose" :short-name "San Jose" :team-name "Sharks"}
               :home {:abbreviation "VGK" :id 54 :location-name "Vegas" :short-name "Vegas" :team-name "Golden Knights"}}
              {:away {:abbreviation "PHI" :id 4 :location-name "Philadelphia" :short-name "Philadelphia" :team-name "Flyers"}
               :home {:abbreviation "ANA" :id 24 :location-name "Anaheim" :short-name "Anaheim" :team-name "Ducks"}}
              {:away {:abbreviation "DAL" :id 25 :location-name "Dallas" :short-name "Dallas" :team-name "Stars"}
               :home {:abbreviation "WPG" :id 52 :location-name "Winnipeg" :short-name "Winnipeg" :team-name "Jets"}}
              {:away {:abbreviation "BUF" :id 7 :location-name "Buffalo" :short-name "Buffalo" :team-name "Sabres"}
               :home {:abbreviation "PIT" :id 5 :location-name "Pittsburgh" :short-name "Pittsburgh" :team-name "Penguins"}}]
             (map :teams games)) "Parsed teams")
      (is (= [{"MTL" 3 "DET" 2 :overtime true}
              {"CGY" 4 "TOR" 5 :shootout true}
              {"SJS" 0 "VGK" 5}
              {"PHI" 6 "ANA" 3}
              {"DAL" 0 "WPG" 0}
              {"BUF" 0 "PIT" 0}]
             (map :scores games)) "Parsed scores")
      (is (= [false]
             (distinct (map #(contains? % :playoff-series) games)))))))

(deftest game-scores-parsing-games

  (testing "Parsing game with goals in regulation and overtime"
    (let [game (first
                 (:games
                   (parse-game-scores
                     (get-latest-games default-games)
                     default-standings
                     (resources/get-landings [2023020195]))))
          goals (map #(dissoc % :strength) (:goals game))]  ; 'strength' field has its own test
      (is (= [{:team    "MTL" :min 7 :sec 2 :period "1"
               :scorer  {:player "Mike Matheson" :player-id 8476875 :season-total 3}
               :assists [{:player "Alex Newhook" :player-id 8481618 :season-total 4}]}
              {:team    "DET" :min 16 :sec 44 :period "2"
               :scorer  {:player "Christian Fischer" :player-id 8478432 :season-total 1}
               :assists [{:player "Michael Rasmussen" :player-id 8479992 :season-total 3}
                         {:player "Ben Chiarot" :player-id 8475279 :season-total 4}]}
              {:team    "MTL" :min 0 :sec 26 :period "3"
               :scorer  {:player "Nick Suzuki" :player-id 8480018 :season-total 5}
               :assists [{:player "Mike Matheson" :player-id 8476875 :season-total 6}
                         {:player "Sean Monahan" :player-id 8477497 :season-total 6}]}
              {:team    "DET" :min 6 :sec 6 :period "3"
               :scorer  {:player "J.T. Compher" :player-id 8477456 :season-total 3}
               :assists []}
              {:team    "MTL" :min 4 :sec 16 :period "OT"
               :scorer  {:player "Cole Caufield" :player-id 8481540 :season-total 5}
               :assists [{:player "Mike Matheson" :player-id 8476875 :season-total 7}
                         {:player "Nick Suzuki" :player-id 8480018 :season-total 7}]}]
             goals) "Parsed goals")))

  (testing "Parsing game with goals in regulation and shootout"
    (let [game (nth
                 (:games
                   (parse-game-scores
                     (get-latest-games default-games)
                     default-standings
                     (resources/get-landings [2023020207])))
                 1)
          goals (map #(dissoc % :strength) (:goals game))]  ; 'strength' field has its own test
      (is (= [{:team    "TOR" :min 3 :sec 1 :period "1"
               :scorer  {:player "William Nylander" :player-id 8477939 :season-total 8}
               :assists []}
              {:team    "CGY" :min 7 :sec 2 :period "1"
               :scorer  {:player "Connor Zary" :player-id 8482074 :season-total 2}
               :assists [{:player "Nazem Kadri" :player-id 8475172 :season-total 6}]}
              {:team    "TOR" :min 8 :sec 18 :period "1"
               :scorer  {:player "Calle Jarnkrok" :player-id 8475714 :season-total 4}
               :assists [{:player "Max Domi" :player-id 8477503 :season-total 7}
                         {:player "Nicholas Robertson" :player-id 8481582 :season-total 2}]}
              {:team    "TOR" :min 1 :sec 6 :period "2"
               :scorer  {:player "William Nylander" :player-id 8477939 :season-total 9}
               :assists []}
              {:team    "TOR" :min 4 :sec 45 :period "2"
               :scorer  {:player "John Tavares" :player-id 8475166 :season-total 6}
               :assists [{:player "William Nylander" :player-id 8477939 :season-total 12}
                         {:player "Morgan Rielly" :player-id 8476853 :season-total 9}]}
              {:team    "CGY" :min 5 :sec 33 :period "2"
               :scorer  {:player "Nikita Zadorov" :player-id 8477507 :season-total 1}
               :assists [{:player "Andrew Mangiapane" :player-id 8478233 :season-total 4}
                         {:player "Jonathan Huberdeau" :player-id 8476456 :season-total 5}]}
              {:team    "CGY" :min 12 :sec 22 :period "2"
               :scorer  {:player "A.J. Greer" :player-id 8478421 :season-total 2}
               :assists [{:player "MacKenzie Weegar" :player-id 8477346 :season-total 4}
                         {:player "Rasmus Andersson" :player-id 8478397 :season-total 3}]}
              {:team    "CGY" :min 3 :sec 22 :period "3"
               :scorer  {:player "Martin Pospisil" :player-id 8481028 :season-total 2}
               :assists [{:player "Mikael Backlund" :player-id 8474150 :season-total 4}
                         {:player "Blake Coleman" :player-id 8476399 :season-total 2}]}
              {:team "TOR" :period "SO" :scorer {:player "Max Domi" :player-id 8477503}}]
             goals) "Parsed goals")))

  ; TODO later
  (comment
    (testing "Parsing game with goal in playoff overtime"
      (let [game (nth
                   (:games
                     (parse-game-scores
                       (get-latest-games resources/playoff-games-finished-in-regulation-and-overtime)
                       default-standings))
                   2)]
        (is (= {"CHI" 0 "STL" 1 :overtime true}
               (:scores game)) "Parsed scores")
        (is (= [{:team    "STL" :min 9 :sec 4 :period "4"
                 :scorer  {:player "David Backes" :player-id 8470655 :season-total 1}
                 :assists [{:player "Jay Bouwmeester" :player-id 8470151 :season-total 1}
                           {:player "Alex Pietrangelo" :player-id 8474565 :season-total 1}]}]
               (:goals game)) "Parsed goals")))

    (testing "Parsing game without goals"
      (let [game (nth
                   (:games
                     (parse-game-scores
                       (get-latest-games resources/games-for-validation-testing)
                       default-standings))
                   0)]
        (is (= []
               (:goals game)) "Parsed goals"))))

  (testing "Parsing empty net goal information"
    (let [game (nth
                 (:games
                   (parse-game-scores
                     (get-latest-games default-games)
                     default-standings
                     (resources/get-landings [2023020208])))
                 3)
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
                     (get-latest-games default-games)
                     default-standings
                     (resources/get-landings [2023020209])))
                 2)
          goals (:goals game)]
      (is (= [nil nil "SHG" nil "PPG"]
             (map #(:strength %) goals))
          "Parsed goal strengths")
      (is (= [false false true false true]
             (map #(contains? % :strength) goals))
          "Even strength goals don't contain :strength field"))))

(deftest game-scores-parsing-game-statuses

  (testing "Parsing games' statuses"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games default-games)
                    default-standings
                    default-landings))
          statuses (map #(:status %) games)]
      (is (= [{:state "FINAL"}
              {:state "FINAL"}
              {:state "FINAL"}
              {:state    "LIVE"
               :progress {:current-period                3,
                          :current-period-ordinal        "3rd",
                          :current-period-time-remaining {:pretty "01:23" :min 1 :sec 23}}}
              {:state "PREVIEW"}
              {:state "PREVIEW"}]
             statuses) "Parsed game statuses"))))

(deftest game-scores-parsing-game-start-times

  (testing "Parsing games' start times"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games default-games)
                    default-standings))
          start-times (map #(:start-time %) games)]
      (is (= ["2023-11-10T00:00:00Z"
              "2023-11-11T00:00:00Z"
              "2023-11-11T03:00:00Z"
              "2023-11-11T03:00:00Z"
              "2023-11-11T20:00:00Z"
              "2023-11-12T00:30:00Z"]
             start-times) "Parsed game start times"))))

(deftest game-scores-parsing-game-stats

  (testing "Parsing game stats"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games default-games)
                    default-standings
                    default-landings))
          game-stats (map #(:game-stats %) games)]
      (is (= 6
             (count game-stats)) "Parsed game stats count")
      (is (= [{:blocked                 {"MTL" 14 "DET" 12}
               :face-off-win-percentage {"MTL" "53.4" "DET" "46.6"}
               :giveaways               {"MTL" 1 "DET" 10}
               :hits                    {"MTL" 18 "DET" 17}
               :pim                     {"MTL" 10 "DET" 22}
               :power-play              {"MTL" {:goals 2 :opportunities 5 :percentage "40.0"}
                                         "DET" {:goals 0 :opportunities 4 :percentage "0.0"}}
               :shots                   {"MTL" 27 "DET" 29}
               :takeaways               {"MTL" 4 "DET" 2}}
              {:blocked                 {"CGY" 19 "TOR" 15}
               :face-off-win-percentage {"CGY" "40.7" "TOR" "59.3"}
               :giveaways               {"CGY" 7 "TOR" 11}
               :hits                    {"CGY" 21 "TOR" 25}
               :pim                     {"CGY" 4 "TOR" 6}
               :power-play              {"CGY" {:goals 0 :opportunities 2 :percentage "0.0"}
                                         "TOR" {:goals 1 :opportunities 1 :percentage "100.0"}}
               :shots                   {"CGY" 28 "TOR" 36}
               :takeaways               {"CGY" 13 "TOR" 10}}
              {:blocked                 {"SJS" 16 "VGK" 15}
               :face-off-win-percentage {"SJS" "55.9" "VGK" "44.1"}
               :giveaways               {"SJS" 4 "VGK" 8}
               :hits                    {"SJS" 19 "VGK" 14}
               :pim                     {"SJS" 2 "VGK" 10}
               :power-play              {"SJS" {:goals 0 :opportunities 4 :percentage "0.0"}
                                         "VGK" {:goals 0 :opportunities 0 :percentage "0.0"}}
               :shots                   {"SJS" 20 "VGK" 39}
               :takeaways               {"SJS" 4 "VGK" 7}}
              {:blocked                 {"PHI" 21 "ANA" 9}
               :face-off-win-percentage {"PHI" "49.2" "ANA" "50.8"}
               :giveaways               {"PHI" 7 "ANA" 12}
               :hits                    {"PHI" 10 "ANA" 14}
               :pim                     {"PHI" 12 "ANA" 6}
               :power-play              {"PHI" {:goals 0 :opportunities 3 :percentage "0.0"}
                                         "ANA" {:goals 2 :opportunities 6 :percentage "33.3"}}
               :shots                   {"PHI" 36 "ANA" 38}
               :takeaways               {"PHI" 7 "ANA" 3}}
              nil nil]
             game-stats) "Parsed game stats"))))

(deftest game-scores-parsing-team-records

  (testing "Parsing teams' pre-game regular season records"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games default-games)
                    default-standings
                    default-landings))
          records (map #(:records (:pre-game-stats %)) games)]
      (is (= 6
             (count records)) "Parsed pre-game regular season records count")
      (is (= [{"DET" {:wins 7 :losses 5 :ot 1} "MTL" {:wins 5 :losses 5 :ot 2}}
              {"CGY" {:wins 4 :losses 7 :ot 0} "TOR" {:wins 5 :losses 5 :ot 2}}
              {"SJS" {:wins 2 :losses 9 :ot 1} "VGK" {:wins 10 :losses 2 :ot 1}}
              {"ANA" {:wins 7 :losses 5 :ot 0} "PHI" {:wins 5 :losses 7 :ot 1}}
              {"DAL" {:wins 8 :losses 3 :ot 1} "WPG" {:wins 7 :losses 4 :ot 2}}
              {"BUF" {:wins 6 :losses 6 :ot 1} "PIT" {:wins 6 :losses 6 :ot 0}}]
             records) "Parsed pre-game regular season records")))

  (testing "Parsing teams' current regular season records"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games default-games)
                    default-standings
                    default-landings))
          records (map #(:records (:current-stats %)) games)]
      (is (= 6
             (count records)) "Parsed current regular season records count")
      (is (= [{"DET" {:wins 7 :losses 5 :ot 2} "MTL" {:wins 6 :losses 5 :ot 2}}
              {"CGY" {:wins 4 :losses 7 :ot 1} "TOR" {:wins 6 :losses 5 :ot 2}}
              {"SJS" {:wins 2 :losses 10 :ot 1} "VGK" {:wins 11 :losses 2 :ot 1}}
              {"ANA" {:wins 7 :losses 5 :ot 0} "PHI" {:wins 5 :losses 7 :ot 1}}
              {"DAL" {:wins 8 :losses 3 :ot 1} "WPG" {:wins 7 :losses 4 :ot 2}}
              {"BUF" {:wins 6 :losses 6 :ot 1} "PIT" {:wins 6 :losses 6 :ot 0}}]
             records) "Parsed current regular season records"))))

(deftest game-scores-parsing-team-streaks

  (testing "Parsing teams' current streaks"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games default-games)
                    default-standings))
          streaks (map #(:streaks (:current-stats %)) games)]
      (is (= 6
             (count streaks)) "Parsed streaks count")
      (is (= [{"DET" {:type "OT" :count 1} "MTL" {:type "WINS" :count 1}}
              {"CGY" {:type "WINS" :count 2} "TOR" {:type "LOSSES" :count 1}}
              {"SJS" {:type "WINS" :count 2} "VGK" {:type "LOSSES" :count 2}}
              {"ANA" {:type "LOSSES" :count 1} "PHI" {:type "LOSSES" :count 2}}
              {"DAL" {:type "WINS" :count 1} "WPG" {:type "WINS" :count 3}}
              {"BUF" {:type "OT" :count 1} "PIT" {:type "WINS" :count 3}}]
             streaks) "Parsed streaks"))))

(deftest game-scores-parsing-team-ranks

  (testing "Parsing teams' division and league ranks for regular season games"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games default-games)
                    default-standings))
          pre-game-stats-standings (map #(:standings (:pre-game-stats %)) games)
          current-stats-standings (map #(:standings (:current-stats %)) games)
          ranks (map
                  #(fmap-vals (fn [team-stats] (select-keys team-stats [:division-rank :league-rank])) %)
                  current-stats-standings)]
      (is (= (repeat 6 nil) pre-game-stats-standings) "Pre-game standings should not exist")
      (is (= [{"DET" {:division-rank "2" :league-rank "10"} "MTL" {:division-rank "6" :league-rank "16"}}
              {"CGY" {:division-rank "6" :league-rank "30"} "TOR" {:division-rank "5" :league-rank "15"}}
              {"SJS" {:division-rank "8" :league-rank "32"} "VGK" {:division-rank "1" :league-rank "2"}}
              {"ANA" {:division-rank "4" :league-rank "14"} "PHI" {:division-rank "7" :league-rank "25"}}
              {"DAL" {:division-rank "1" :league-rank "6"} "WPG" {:division-rank "3" :league-rank "8"}}
              {"BUF" {:division-rank "7" :league-rank "19"} "PIT" {:division-rank "6" :league-rank "23"}}]
             ranks) "Parsed current stats division and league ranks")))

  ; TODO: Later
  (comment
    (testing "Parsing teams' division and league ranks for playoff games"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/playoff-games-finished-in-regulation-and-overtime)
                    (:standings resources/standings)))
          pre-game-stats-standings (map #(:standings (:pre-game-stats %)) games)
          current-stats-standings (map #(:standings (:current-stats %)) games)
          ranks (map
                  #(fmap-vals (fn [team-stats] (select-keys team-stats [:division-rank :league-rank])) %)
                  current-stats-standings)]
      (is (= pre-game-stats-standings current-stats-standings) "Parsed standings, pre-game vs. current stats")
      (is (= [{"DET" {:division-rank "8" :league-rank "31"} "TBL" {:division-rank "2" :league-rank "4"}}
              {"NYR" {:division-rank "7" :league-rank "18"} "PIT" {:division-rank "3" :league-rank "7"}}
              {"CHI" {:division-rank "7" :league-rank "23"} "STL" {:division-rank "1" :league-rank "2"}}]
             ranks) "Parsed current stats division and league ranks")))))

(deftest game-scores-parsing-team-points-from-playoff-spot

  (testing "Parsing teams' points from playoff spot"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games default-games)
                    default-standings))
          standings (map #(:standings (:current-stats %)) games)
          points-from-playoff-spot (map
                                     #(fmap-vals (fn [team-stats] (select-keys team-stats [:points-from-playoff-spot])) %)
                                     standings)]
      (is (= 6
             (count points-from-playoff-spot)) "Parsed points from playoff spot count")
      ; The test standings are:
      ;
      ; Eastern conference                       Western conference
      ;
      ; Metropolitan                             Central
      ; 1 NYR 21 pts                             1 DAL 17 pts
      ; 2 CAR 16 pts                             2 COL 16 pts
      ; 3 NJD 15 pts                             3 WPG 16 pts
      ; ------------                             4 STL 13 pts, 2nd wild card
      ; 4 NYI 13 pts                             ------------
      ; 5 WSH 12 pts                             5 ARI 13 pts, 1st out of the playoffs
      ; 6 PIT 12 pts                             6 MIN 12 pts
      ; 7 PHI 11 pts                             7 CHI 10 pts
      ; 8 CBJ 11 pts                             8 NSH 10 pts
      ;
      ; Atlantic                                 Pacific
      ; 1 BOS 23 pts                             1 VGK 23 pts
      ; 2 DET 16 pts                             2 VAN 21 pts
      ; 3 TBL 16 pts                             3 LAK 19 pts
      ; 4 FLA 15 pts, 1st wild card              4 ANA 14 pts, 1st wild card
      ; 5 TOR 14 pts, 2nd wild card              ------------
      ; ------------                             5 SEA 13 pts
      ; 6 MTL 14 pts, 1st out of the playoffs    6 CGY  9 pts
      ; 7 BUF 13 pts                             7 EDM  5 pts
      ; 8 OTT 10 pts                             8 SJS  5 pts
      (is (= [{"DET" {:points-from-playoff-spot "+2"} "MTL" {:points-from-playoff-spot "0"}}
              {"CGY" {:points-from-playoff-spot "-4"} "TOR" {:points-from-playoff-spot "0"}}
              {"SJS" {:points-from-playoff-spot "-8"} "VGK" {:points-from-playoff-spot "+10"}}
              {"ANA" {:points-from-playoff-spot "+1"} "PHI" {:points-from-playoff-spot "-3"}}
              {"DAL" {:points-from-playoff-spot "+4"} "WPG" {:points-from-playoff-spot "+3"}}
              {"BUF" {:points-from-playoff-spot "-1"} "PIT" {:points-from-playoff-spot "-2"}}]
             points-from-playoff-spot) "Parsed points from playoff spot"))))

; TODO: Later
(comment
(deftest game-scores-parsing-team-playoff-records

  (testing "Parsing teams' pre-game playoff records when teams have no OT loss values in their records"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/playoff-games-live-finished-with-1st-games)
                    (:standings resources/standings)))
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
                    (:standings resources/standings)))
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
                    (:standings resources/standings)))
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
                    (:standings resources/standings)))
          playoff-series (map #(:playoff-series (:pre-game-stats %)) games)
          wins (map :wins playoff-series)]
      (is (= [{"DET" 0 "TBL" 1} {"NYI" 1 "FLA" 0} {"CHI" 0 "STL" 1} {"NSH" 0 "ANA" 0}]
             wins) "Parsed pre-game playoff series wins")))

  (testing "Parsing current playoff series wins from playoff games"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/playoff-games-finished-with-2nd-games)
                    (:standings resources/standings)))
          playoff-series (map #(:playoff-series (:current-stats %)) games)
          wins (map :wins playoff-series)]
      (is (= [{"DET" 0 "TBL" 2} {"NYI" 1 "FLA" 1} {"CHI" 1 "STL" 1} {"NSH" 1 "ANA" 0}]
             wins) "Parsed current playoff series wins")))

  (testing "Parsing pre-game playoff series wins from first playoff games"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games resources/playoff-games-live-finished-with-1st-games)
                    (:standings resources/standings)))
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
                    (:standings resources/standings)))
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
                    (:standings resources/standings)))
          pre-game-stats-playoff-series (map #(:playoff-series (:pre-game-stats %)) games)
          current-stats-playoff-series (map #(:playoff-series (:current-stats %)) games)
          pre-game-stats-rounds (map :round pre-game-stats-playoff-series)
          current-stats-rounds (map :round current-stats-playoff-series)]
      (is (= [1 1 1 1 1]
             pre-game-stats-rounds) "Parsed pre-game stats playoff rounds")
      (is (= [1 1 1 1 1]
             current-stats-rounds) "Parsed current stats playoff rounds")))))

(deftest game-scores-pre-game-stats
  (testing "Not including teams' pre-game stats"
    (let [games (:games
                  (parse-game-scores
                    (get-latest-games default-games)
                    (:standings resources/standings)
                    nil
                    false))]
      (is (= [false]
             (distinct (map #(contains? % :pre-game-stats) games)))
          "Pre-game stats are not included"))))

; TODO: Later
(comment
(deftest game-scores-validation

  (testing "Validating valid game with goals"
    (let [game (first (:games
                        (parse-game-scores
                          (get-latest-games default-games)
                          (:standings resources/standings))))]
      (is (= (contains? game :errors) false) "No validation errors")))

  (testing "Validating valid game without goals"
    (let [game (nth (:games
                      (parse-game-scores
                        (get-latest-games default-games)
                        (:standings resources/standings)))
                    3)]
      (is (= (contains? game :errors) false) "No validation errors")))

  (testing "Validating valid non-finished game with multiple shootout goals"
    (let [game (nth (:games
                      (parse-game-scores
                        (get-latest-games resources/games-for-validation-testing)
                        (:standings resources/standings)))
                    4)]
      (is (= (contains? game :errors) false) "No validation errors")))

  (testing "Validating valid finished game with multiple shootout goals"
    (let [game (nth (:games
                      (parse-game-scores
                        (get-latest-games resources/games-for-validation-testing)
                        (:standings resources/standings)))
                    3)]
      (is (= (contains? game :errors) false) "No validation errors")))

  (testing "Validating game missing all goals"
    (let [game (first (:games
                        (parse-game-scores
                          (get-latest-games resources/games-for-validation-testing)
                          (:standings resources/standings))))]
      (is (= (contains? game :errors) true) "Contains validation errors")
      (is (= [{:error :MISSING-ALL-GOALS}]
             (:errors game)) "Errors contain 'missing all goals' error")))

  (testing "Validating game missing one goal"
    (let [game (second (:games
                         (parse-game-scores
                           (get-latest-games resources/games-for-validation-testing)
                           (:standings resources/standings))))]
      (is (= (contains? game :errors) true) "Contains validation errors")
      (is (= [{:error :SCORE-AND-GOAL-COUNT-MISMATCH :details {:goal-count 4 :score-count 5}}]
             (:errors game)) "Errors contain expected 'score and goal count mismatch' error")))

  (testing "Validating game having one goal too many"
    (let [game (nth (:games
                      (parse-game-scores
                        (get-latest-games resources/games-for-validation-testing)
                        (:standings resources/standings)))
                    2)]
      (is (= (contains? game :errors) true) "Contains validation errors")
      (is (= [{:error :SCORE-AND-GOAL-COUNT-MISMATCH :details {:goal-count 5 :score-count 3}}]
             (:errors game)) "Errors contain expected 'score and goal count mismatch' error")))))