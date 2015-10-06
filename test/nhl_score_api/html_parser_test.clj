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
             (count games)) "Parsed game count"))))
