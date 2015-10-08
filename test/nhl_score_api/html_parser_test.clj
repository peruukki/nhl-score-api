(ns nhl-score-api.html-parser-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.html-parser :refer :all])
  (:import (java.io File)))

(def resource-path "test/nhl_score_api/resources/")

(def html-with-games (new File (str resource-path "scores-2015-10-03.htm")))
(def html-without-games (new File (str resource-path "scores-2015-10-06.htm")))

(deftest game-score-html-parsing

  (testing "Parsing page with games"
    (let [games (parse-scores html-with-games)]
      (is (= 9
             (count games)) "Parsed game count")
      (is (= [8 5 9 3 4 6 5 0 5]
             (map count games)) "Parsed goal count")))

  (testing "Parsing page with no games"
    (let [games (parse-scores html-without-games)]
      (is (= 0
             (count games)) "Parsed game count")))

  (testing "Parsing game with goals in regulation and overtime"
    (let [goals (last (parse-scores html-with-games))]
      (is (= [{:team "VAN" :time "03:57" :scorer "Bo Horvat" :goal-count 1 :period 1}
              {:team "EDM" :time "03:08" :scorer "Benoit Pouliot" :goal-count 1 :period 3}
              {:team "EDM" :time "17:19" :scorer "Ryan Nugent-Hopkins" :goal-count 1 :period 3}
              {:team "VAN" :time "18:04" :scorer "Jannik Hansen" :goal-count 1 :period 3}
              {:team "VAN" :time "00:29" :scorer "Daniel Sedin" :goal-count 1 :period 4}]
             goals) "Parsed goals")))

  (testing "Parsing game without goals"
    (let [goals (nth (parse-scores html-with-games) 7)]
      (is (= []
             goals) "Parsed goals"))))
