(ns nhl-score-api.game-scores-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.game-scores :refer :all]
            [nhl-score-api.resources :as resources]))

(deftest game-score-html-parsing

  (testing "Parsing page with games finished in overtime and in shootout"
    (let [games (parse-game-scores resources/dom-with-ot-and-so-game)]
      (is (= 14
             (count games)) "Parsed game count")
      (is (= [5 6 11 8 7 8 7 2 5 5 9 5 3 2]
             (map #(count (:goals %)) games)) "Parsed goal count")
      (is (= [["TBL" "BUF"] ["MTL" "BOS"] ["OTT" "TOR"] ["PHI" "FLA"]
              ["CBJ" "NYR"] ["NJD" "WSH"] ["DET" "CAR"] ["EDM" "NSH"]
              ["STL" "MIN"] ["NYI" "CHI"] ["DAL" "COL"] ["CGY" "VAN"]
              ["PIT" "ARI"] ["ANA" "SJS"]]
             (map :teams games)) "Parsed team names")
      (is (= [{"TBL" 4 "BUF" 1} {"MTL" 4 "BOS" 2} {"OTT" 5 "TOR" 4 :shootout true} {"PHI" 1 "FLA" 7}
              {"CBJ" 2 "NYR" 5} {"NJD" 3 "WSH" 5} {"DET" 4 "CAR" 3} {"EDM" 0 "NSH" 2}
              {"STL" 2 "MIN" 3} {"NYI" 1 "CHI" 4} {"DAL" 3 "COL" 6} {"CGY" 3 "VAN" 2 :overtime true}
              {"PIT" 1 "ARI" 2} {"ANA" 0 "SJS" 2}]
             (map :scores games)) "Parsed scores")))

  (testing "Parsing page with one game missing goal information"
    (let [games (parse-game-scores resources/dom-with-game-missing-goals)]
      (is (= 8
             (count games)) "Parsed game count")
      (is (= [8 5 9 3 4 6 5 5]
             (map #(count (:goals %)) games)) "Parsed goal count")))

  (testing "Parsing page with games that have not yet started"
    (let [games (parse-game-scores resources/dom-with-not-started-games)]
      (is (= 0
             (count games)) "Parsed game count")))

  (testing "Parsing page with no games"
    (let [games (parse-game-scores resources/dom-without-games)]
      (is (= 0
             (count games)) "Parsed game count")))

  (testing "Parsing game with goals in regulation and overtime"
    (let [goals (:goals (nth (parse-game-scores resources/dom-with-ot-and-so-game) 11))]
      (is (= [{:team "CGY" :min 5 :sec 0 :scorer "Dougie Hamilton" :goal-count 1 :period "1"}
              {:team "VAN" :min 2 :sec 3 :scorer "Bo Horvat" :goal-count 1 :period "2"}
              {:team "VAN" :min 11 :sec 33 :scorer "Jared McCann" :goal-count 1 :period "2"}
              {:team "CGY" :min 8 :sec 49 :scorer "Sean Monahan" :goal-count 1 :period "3"}
              {:team "CGY" :min 3 :sec 3 :scorer "Johnny Gaudreau" :goal-count 1 :period "OT"}]
             goals) "Parsed goals")))

  (testing "Parsing game with goals in shootout"
    (let [goals (:goals (nth (parse-game-scores resources/dom-with-ot-and-so-game) 2))]
      (is (= [{:team "TOR" :scorer "Pierre-Alexandre Parenteau" :period "SO"}
              {:team "OTT" :scorer "Bobby Ryan" :period "SO"}
              {:team "OTT" :scorer "Mike Hoffman" :period "SO"}]
             (take-last 3 goals)) "Parsed shootout goals")))

  (testing "Parsing game without goals"
    (let [dom (parse-dom resources/dom-with-game-missing-goals)
          game (nth (parse-games dom) 7)
          goals (parse-goals game)]
      (is (= []
             goals) "Parsed goals"))))
