(ns nhl-score-api.fetchers.mlbam.game-scores-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.fetchers.mlbam.game-scores :refer :all]
            [nhl-score-api.fetchers.mlbam.latest-games :refer [filter-latest-finished-games]]
            [nhl-score-api.fetchers.mlbam.resources :as resources]))

(deftest game-score-json-parsing

  (testing "Parsing scores with games finished in overtime and in shootout"
    (let [games (parse-game-scores (filter-latest-finished-games resources/games-finished-in-regulation-overtime-and-shootout))]
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
             (map :scores games)) "Parsed scores")))

  (testing "Parsing game with goals in regulation and overtime"
    (let [game (nth (parse-game-scores (filter-latest-finished-games resources/games-finished-in-regulation-overtime-and-shootout)) 4)
          goals (:goals game)]
      (is (= [{:team "EDM" :min 0 :sec 22 :scorer "Connor McDavid" :goal-count 11 :period "1"}
              {:team "BUF" :min 9 :sec 6 :scorer "Cal O'Reilly" :goal-count 1 :period "3"}
              {:team "EDM" :min 3 :sec 48 :scorer "Connor McDavid" :goal-count 12 :period "OT"}]
             goals) "Parsed goals")))

  (testing "Parsing game with goals in regulation and shootout"
    (let [game (nth (parse-game-scores (filter-latest-finished-games resources/games-finished-in-regulation-overtime-and-shootout)) 3)
          goals (:goals game)]
      (is (= [{:team "STL" :min 3 :sec 36 :scorer "Dmitrij Jaskin" :goal-count 4 :period "1"}
              {:team "STL" :min 12 :sec 53 :scorer "Jaden Schwartz" :goal-count 5 :period "1"}
              {:team "STL" :min 10 :sec 38 :scorer "Vladimir Tarasenko" :goal-count 30 :period "2"}
              {:team "OTT" :min 12 :sec 32 :scorer "Ryan Dzingel" :goal-count 2 :period "2"}
              {:team "OTT" :min 17 :sec 19 :scorer "Jean-Gabriel Pageau" :goal-count 15 :period "3"}
              {:team "OTT" :min 19 :sec 59 :scorer "Jean-Gabriel Pageau" :goal-count 16 :period "3"}
              {:team "STL" :scorer "Patrik Berglund" :period "SO"}]
             goals) "Parsed goals")))

  (testing "Parsing game with goal in playoff overtime"
    (let [game (nth (parse-game-scores (filter-latest-finished-games resources/playoff-games-finished-in-regulation-and-overtime)) 2)]
      (is (= {"CHI" 0 "STL" 1 :overtime true}
             (:scores game)) "Parsed scores")
      (is (= [{:team "STL" :min 9 :sec 4 :scorer "David Backes" :goal-count 1 :period "4"}]
             (:goals game)) "Parsed goals"))))
