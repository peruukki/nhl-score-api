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

(defn- get-final-games [games]
  (filter #(= "Final" (get-game-state %)) games))

(defn- get-date-and-final-games [date-and-games]
  {:date (get-date (:date date-and-games))
   :games (get-final-games (:games date-and-games))})

(defn filter-latest-finished-games [api-response]
  (->> (json/read-str api-response :key-fn ->kebab-case-keyword)
       :dates
       (map #(select-keys % [:date :games]))
       (map get-date-and-final-games)
       (filter #(seq (:games %)))
       last))
