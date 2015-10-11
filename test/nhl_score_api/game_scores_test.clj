(ns nhl-score-api.game-scores-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.game-scores :refer :all]
            [nhl-score-api.resources :as resources]))

(deftest game-score-html-parsing

  (testing "Parsing page with finished games"
    (let [games (parse-game-scores resources/dom-with-game-missing-goals)]
      (is (= 8
             (count games)) "Parsed game count")
      (is (= [8 5 9 3 4 6 5 5]
             (map #(count (:goals %)) games)) "Parsed goal count")
      (is (= [["NSH" "CBJ"] ["TBL" "FLA"] ["MTL" "OTT"] ["DET" "TOR"]
              ["DAL" "CHI"] ["SJS" "ANA"] ["WPG" "CGY"] ["EDM" "VAN"]]
             (map :teams games)) "Parsed team names")
      (is (= [{"NSH" 1 "CBJ" 7} {"TBL" 3 "FLA" 2} {"MTL" 4 "OTT" 5} {"DET" 2 "TOR" 1}
              {"DAL" 0 "CHI" 4} {"SJS" 1 "ANA" 5} {"WPG" 3 "CGY" 2} {"EDM" 2 "VAN" 3 :overtime true}]
             (map :scores games)) "Parsed scores")))

  (testing "Parsing page with games that have not yet started"
    (let [games (parse-game-scores resources/dom-with-not-started-games)]
      (is (= 0
             (count games)) "Parsed game count")))

  (testing "Parsing page with no games"
    (let [games (parse-game-scores resources/dom-without-games)]
      (is (= 0
             (count games)) "Parsed game count")))

  (testing "Parsing game with goals in regulation and overtime"
    (let [goals (:goals (last (parse-game-scores resources/dom-with-game-missing-goals)))]
      (is (= [{:team "VAN" :time "03:57" :scorer "Bo Horvat" :goal-count 1 :period 1}
              {:team "EDM" :time "03:08" :scorer "Benoit Pouliot" :goal-count 1 :period 3}
              {:team "EDM" :time "17:19" :scorer "Ryan Nugent-Hopkins" :goal-count 1 :period 3}
              {:team "VAN" :time "18:04" :scorer "Jannik Hansen" :goal-count 1 :period 3}
              {:team "VAN" :time "00:29" :scorer "Daniel Sedin" :goal-count 1 :period 4}]
             goals) "Parsed goals")))

  (testing "Parsing game without goals"
    (let [dom (parse-dom resources/dom-with-game-missing-goals)
          game (nth (parse-games dom) 7)
          goals (parse-goals game)]
      (is (= []
             goals) "Parsed goals"))))
