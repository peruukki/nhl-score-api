(ns nhl-score-api.fetchers.nhl-api-web.transformer
  (:require [clj-time.core :as time]
            [clj-time.format :as format]))

(defn- prettify-date [date]
  (format/unparse (format/formatter "E MMM d") (format/parse date)))

(defn- get-date [date]
  {:raw date
   :pretty (prettify-date date)})

(defn get-game-state [game]
  (case (:game-state game)
    "OFF" "Final"
    "FINAL" "Final"
    "OVER" "Final"
    "LIVE" "Live"
    "Preview"))

(declare started-game?)

(defn- has-started-games? [date-and-games]
  (let [games (:games date-and-games)]
    (some started-game? games)))

; TODO Figure out game types
(defn- pr-game? [game]
  (= "PR" (:game-type game)))

(defn- remove-pr-games [date-and-games]
  (let [accepted-games (remove pr-game? (:games date-and-games))]
    (assoc date-and-games :games accepted-games)))

(defn- format-date [date-and-games]
  (assoc date-and-games :date (get-date (:date date-and-games))))

(defn- sort-games-by-state [games]
  (assoc games :games (sort-by get-game-state (:games games))))

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
       (map remove-pr-games)
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
        parsed-date (format/parse (format/formatters :year-month-day) date-str)]
    (time/within? range parsed-date)))

(defn get-games-in-date-range [api-response start-date end-date]
  (let [dates-and-games (get-games api-response)]
    (filter #(within-date-range? (:raw (:date %)) start-date end-date) dates-and-games)))
