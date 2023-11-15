(ns nhl-score-api.fetchers.nhl-api-web.transformer-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [nhl-score-api.fetchers.nhl-api-web.transformer :refer :all]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]))

(deftest get-latest-games-test

  (testing "All games from latest day that has started games are returned"
    (let [latest-games (:games
                         (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout))]
      (is (= 6
             (count latest-games)) "Latest started games count")
      (is (= ["OFF" "FINAL" "OVER" "LIVE" "FUT" "PRE"]
             (distinct (map :game-state latest-games))) "Distinct game states")))

  (testing "Games are returned in order: finished -> in progress -> not started"
    (let [latest-games (:games
                         (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout))]
      (is (= ["Final" "Final" "Final" "Live" "Preview" "Preview"]
             (map get-game-state latest-games)) "Game states")))

  (testing "No started games is handled gracefully"
    (let [latest-games (:games
                         (get-latest-games resources/games-in-preview-state))]
      (is (= 0
             (count latest-games)) "Latest started games count")))

  (testing "Date of latest started games is included"
    (let [date (:date
                 (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout))]
      (is (= "2023-11-10"
             (:raw date)))
      (is (= "Fri Nov 10"
             (:pretty date))))))

(deftest get-games-in-date-range-test

  (testing "Only games within given range are returned"
    (let [dates-and-games (get-games-in-date-range resources/games-finished-in-regulation-overtime-and-shootout
                                                   (time/date-time 2023 11 9)
                                                   (time/date-time 2023 11 10))]
      (is (= ["2023-11-09" "2023-11-10"]
             (map #(:raw (:date %)) dates-and-games)) "Dates")
      (is (= [10 6]
             (map #(count (:games %)) dates-and-games)) "Game counts"))))
