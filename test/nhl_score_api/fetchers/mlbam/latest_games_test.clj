(ns nhl-score-api.fetchers.mlbam.latest-games-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.fetchers.mlbam.latest-games :refer :all]
            [nhl-score-api.fetchers.mlbam.resources :as resources]))

(deftest latest-games-filtering

  (testing "Only latest started games are selected"
    (let [latest-games (:games
                         (filter-latest-started-games resources/games-in-live-preview-and-final-states))]
      (is (= 3
             (count latest-games)) "Latest started games count")
      (is (= ["Live" "Final"]
             (distinct (map #(:abstract-game-state (:status %)) latest-games))) "Game states")))

  (testing "No started games is handled gracefully"
    (let [latest-games (:games
                         (filter-latest-started-games resources/games-in-preview-state))]
      (is (= 0
             (count latest-games)) "Latest started games count")))

  (testing "Date of latest started games is included"
    (let [date (:date
                 (filter-latest-started-games resources/games-in-live-preview-and-final-states))]
      (is (= "2016-02-28"
             (:raw date)))
      (is (= "Sun Feb 28"
             (:pretty date))))))
