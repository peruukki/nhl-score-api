(ns nhl-score-api.utils
  (:require [clj-time.format :as format]))

; From https://stackoverflow.com/a/1677927/305436
(defn fmap-vals
  "Applies function f to each value in map m and returns a new map."
  [f m]
  (into {} (for [[k v] m] [k (f v)])))

(defn fmap-keys
  "Applies function f to each key in map m and returns a new map."
  [f m]
  (into {} (for [[k v] m] [(f k) v])))

(defn format-date
  "Formats given date-time to YYMMDD format"
  [date]
  (format/unparse (format/formatters :year-month-day) date))
