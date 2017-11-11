(ns nhl-score-api.fetchers.mlbam.latest-games
  (:require [clojure.data.json :as json]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clj-time.format :as format]))

(defn- prettify-date [date]
  (format/unparse (format/formatter "E MMM d") (format/parse date)))

(defn- get-date [date]
  {:raw date
   :pretty (prettify-date date)})

(defn- get-game-state [game]
  (:abstract-game-state (:status game)))

(defn- has-finished-games? [date-and-games]
  (let [games (:games date-and-games)]
    (some #(= "Final" (get-game-state %)) games)))

(defn- get-latest-date-and-games-with-finished-games [dates-and-games]
  (last
    (filter has-finished-games? dates-and-games)))

(defn- format-date [date-and-games]
  (assoc date-and-games :date (get-date (:date date-and-games))))

(defn- sort-games-by-state [games]
  (assoc games :games (sort-by get-game-state (:games games))))

(defn filter-latest-games [api-response]
  (->> (json/read-str api-response :key-fn ->kebab-case-keyword)
       :dates
       (map #(select-keys % [:date :games]))
       get-latest-date-and-games-with-finished-games
       format-date
       sort-games-by-state))
