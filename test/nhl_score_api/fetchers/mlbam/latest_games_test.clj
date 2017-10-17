(ns nhl-score-api.fetchers.mlbam.latest-games-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.fetchers.mlbam.latest-games :refer :all]
            [nhl-score-api.fetchers.mlbam.resources :as resources]))

(deftest latest-games-filtering

  (testing "Only latest finished games are selected"
    (let [latest-games (:games
                         (filter-latest-finished-games resources/games-in-live-preview-and-final-states))]
      (is (= 1
             (count latest-games)) "Latest finished games count")
      (is (= ["Final"]
             (distinct (map #(:abstract-game-state (:status %)) latest-games))) "Game states")))

  (testing "No finished games is handled gracefully"
    (let [latest-games (:games
                         (filter-latest-finished-games resources/games-in-preview-state))]
      (is (= 0
             (count latest-games)) "Latest finished games count")))

  (testing "Date of latest finished games is included"
    (let [date (:date
                 (filter-latest-finished-games resources/games-in-live-preview-and-final-states))]
      (is (= "2016-02-28"
             date)))))
