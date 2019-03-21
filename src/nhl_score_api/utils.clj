(ns nhl-score-api.utils)

; From https://stackoverflow.com/a/1677927/305436
(defn fmap
  "Applies function f to each value in map m and returns a new map."
  [f m]
  (into {} (for [[k v] m] [k (f v)])))
