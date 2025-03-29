(ns nhl-score-api.param-validator
  (:require [clj-time.core :as time]))

(defn validate-date-range [start-date end-date max-days]
  (cond
    (nil? end-date)
    nil

    (time/before? end-date start-date)
    "End date is before start date"

    (> (time/in-days (time/interval start-date end-date)) (- max-days 1))
    (str "Date range exceeds maximum limit of " max-days " days")

    :else
    nil))
