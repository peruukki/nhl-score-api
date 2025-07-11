(ns nhl-score-api.fetchers.nhl-api-web.transformer
  (:require [clj-time.core :as time]
            [clj-time.format :as format]
            [nhl-score-api.utils :refer [parse-date]]))

(defn- prettify-date [date]
  (format/unparse (format/formatter "E MMM d") (format/parse date)))

(defn- get-date [date]
  {:raw date
   :pretty (prettify-date date)})

(defn get-game-state [game]
  (let [game-schedule-state (:game-schedule-state game)]
    (if (= game-schedule-state "PPD")
      "Postponed"
      (case (:game-state game)
        "OFF" "Final"
        "FINAL" "Final"
        "OVER" "Final"
        "CRIT" "Live"
        "LIVE" "Live"
        "Preview"))))

(declare started-game?)

(defn regular-season-game? [schedule-game]
  (= 2 (:game-type schedule-game)))

(defn playoff-game? [schedule-game]
  (= 3 (:game-type schedule-game)))

(defn non-playoff-game? [schedule-game]
  (not (playoff-game? schedule-game)))

(defn- non-league-game? [schedule-game]
  (not (or (regular-season-game? schedule-game)
           (playoff-game? schedule-game))))

(defn- has-started-games? [date-and-games]
  (let [games (:games date-and-games)]
    (some started-game? games)))

(defn- remove-non-league-games [date-and-games]
  (let [accepted-games (remove non-league-game? (:games date-and-games))]
    (assoc date-and-games :games accepted-games)))

(defn- format-date [date-and-games]
  (assoc date-and-games :date (get-date (:date date-and-games))))

(defn- game-comparator [game]
  (case (get-game-state game)
    "Final" 0
    "Live" 1
    "Preview" 2
    "Postponed" 3))

(defn- sort-games-by-state [games]
  (assoc games :games (sort-by game-comparator (:games games))))

(defn finished-game? [game]
  (= "Final" (get-game-state game)))

(defn live-game? [game]
  (= "Live" (get-game-state game)))

(defn started-game? [game]
  (or (finished-game? game)
      (live-game? game)))

(defn- transform-games [api-response]
  (->> api-response
       :game-week
       (map #(select-keys % [:date :games]))
       (map remove-non-league-games)
       (map format-date)
       (map sort-games-by-state)))

(defn get-games [api-response]
  (transform-games api-response))

(defn get-latest-games [api-response]
  (let [dates-and-games (get-games api-response)]
    (last
     (filter has-started-games? dates-and-games))))

(defn- within-date-range? [date-str start-date end-date]
  (let [range (time/interval start-date (time/plus end-date (time/seconds 1)))
        parsed-date (parse-date date-str)]
    (time/within? range parsed-date)))

(defn get-games-in-date-range [api-response start-date end-date]
  (let [dates-and-games (get-games api-response)]
    (filter #(within-date-range? (:raw (:date %)) start-date end-date) dates-and-games)))
