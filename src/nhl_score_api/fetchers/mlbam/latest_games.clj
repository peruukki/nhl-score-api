(ns nhl-score-api.fetchers.mlbam.latest-games
  (:require [clojure.data.json :as json]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]))

(defn- get-game-state [game]
  (:abstract-game-state (:status game)))

(defn- get-final-games [games]
  (filter #(= "Final" (get-game-state %)) games))

(defn filter-latest-finished-games [api-response]
  (->> (json/read-str api-response :key-fn ->kebab-case-keyword)
       :dates
       (map #(:games %))
       (map #(get-final-games %))
       (filter seq)
       last))
