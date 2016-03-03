(ns nhl-score-api.fetchers.mlbam.latest-games-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.fetchers.mlbam.latest-games :refer :all]
            [nhl-score-api.fetchers.mlbam.resources :as resources]))

(deftest latest-games-filtering

  (testing "Only latest finished games are selected"
    (let [latest-games (filter-latest-finished-games resources/games-in-live-preview-and-final-states)]
      (is (= 1
             (count latest-games)) "Latest finished games count")
      (is (= ["Final"]
             (distinct (map #(:abstract-game-state (:status %)) latest-games))) "Game states"))))