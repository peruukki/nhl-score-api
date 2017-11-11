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
      (is (= ["Final" "Live"]
             (distinct (map #(:abstract-game-state (:status %)) latest-games))) "Distinct game states")))

  (testing "Finished games are returned before on-going ones"
    (let [latest-games (:games
                         (filter-latest-started-games resources/games-in-live-preview-and-final-states))]
      (is (= ["Final" "Live" "Live"]
             (map #(:abstract-game-state (:status %)) latest-games)) "Game states")))

  (testing "If latest games have started but none finished, previous night's games are returned"
    (let [latest-date-and-games (filter-latest-started-games resources/latest-games-in-live-and-preview-states)
          latest-games (:games latest-date-and-games)]
      (is (= "2017-11-09"
             (:raw (:date latest-date-and-games))) "Latest started games date")
      (is (= 7
             (count latest-games)) "Latest started games count")
      (is (= ["Final"]
             (distinct (map #(:abstract-game-state (:status %)) latest-games))) "Distinct game states")))

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
