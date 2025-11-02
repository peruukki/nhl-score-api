(ns nhl-score-api.fetchers.nhl-api-web.transformer-test
  (:require [clj-time.core :as time]
            [clojure.test :refer [deftest is testing]]
            [nhl-score-api.fetchers.nhl-api-web.resources :as resources]
            [nhl-score-api.fetchers.nhl-api-web.transformer :refer [get-game-state
                                                                    get-games-in-date-range
                                                                    get-latest-games]]))

(deftest get-latest-games-test

  (testing "All games from latest day that has started games are returned"
    (let [latest-games (:games
                        (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout))]
      (is (= 9
             (count latest-games)) "Latest started games count")
      (is (= ["OFF" "FINAL" "OVER" "CRIT" "LIVE" "FUT" "PRE"]
             (distinct (map :game-state latest-games))) "Distinct game states")))

  (testing "Games are returned in order: finished -> in progress -> not started -> postponed"
    (let [latest-games (:games
                        (get-latest-games resources/games-finished-in-regulation-overtime-and-shootout))]
      (is (= ["Final" "Final" "Final" "Live" "Live" "Live" "Preview" "Preview" "Postponed"]
             (map get-game-state latest-games)) "Game states")))

  (testing "No started games is handled gracefully"
    (let [latest-games (:games
                        (get-latest-games resources/games-in-preview-state))]
      (is (= 0
             (count latest-games)) "Latest started games count")))

  (testing "Non-league games are not returned"
    (let [latest-games (:games
                        (get-latest-games resources/non-league-games))]
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
      (is (= [10 9]
             (map #(count (:games %)) dates-and-games)) "Game counts"))))
