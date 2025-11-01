(ns nhl-score-api.param-validator
  (:require [clj-time.core :as time]))

(defn validate-date-range [start-date end-date max-days]
  (cond
    (time/before? start-date (time/date-time 1917 1 1))
    "Start date is too soon, earliest possible is 1917-01-01"

    (nil? end-date)
    nil

    (time/before? end-date start-date)
    "End date is before start date"

    (> (time/in-days (time/interval start-date end-date)) (- max-days 1))
    (str "Date range exceeds maximum limit of " max-days " days")

    :else
    nil))
