(ns nhl-score-api.fetchers.nhl-api-web.api.schedule
  (:require [malli.core :as malli]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]))

(def GameSchema
  (malli/schema
   [:map
    [:id :int]
    [:game-schedule-state [:enum "OK" "PPD"]]
    [:game-state [:enum "CRIT" "FINAL" "FUT" "LIVE" "OFF" "OVER" "PRE"]]
    [:game-type :int]]))

(def GameWeekSchema
  (malli/schema
   [:map
    [:date :string]
    [:games [:vector GameSchema]]]))

(def ResponseSchema
  (malli/schema
   [:map
    [:game-week [:vector GameWeekSchema]]
    [:regular-season-end-date :string]
    [:regular-season-start-date :string]]))

(defrecord ScheduleApiRequest [start-date-str end-date-str]
  api/ApiRequest
  (archive? [_ response] (->> response
                              (api/get-games-in-date-range start-date-str end-date-str)
                              (every? #(and (= "OFF" (:game-state %))
                                            (:three-min-recap %)))))
  (cache-key [_] (str "schedule-" start-date-str (when end-date-str (str "-" end-date-str))))
  (description [_] (str "schedule " {:date start-date-str}))
  (response-schema [_] ResponseSchema)
  (url [_] (str api/base-url "/schedule/" start-date-str)))
