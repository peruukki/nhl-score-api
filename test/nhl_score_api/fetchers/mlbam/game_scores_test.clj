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
             (map :scores games)) "Parsed scores"))))
