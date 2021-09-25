(ns nhl-score-api.utils)

; From https://stackoverflow.com/a/1677927/305436
(defn fmap-vals
  "Applies function f to each value in map m and returns a new map."
  [f m]
  (into {} (for [[k v] m] [k (f v)])))

(defn fmap-keys
  "Applies function f to each key in map m and returns a new map."
  [f m]
  (into {} (for [[k v] m] [(f k) v])))
