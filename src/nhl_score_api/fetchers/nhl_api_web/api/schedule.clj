(ns nhl-score-api.fetchers.nhl-api-web.api.schedule
  (:require [malli.core :as malli]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.schema :as schema]))

(def PeriodTypeSchema
  (malli/schema
   [:enum "OT" "REG" "SO"]))

(def TeamSchema
  (malli/schema
   [:map
    [:abbrev :string]
    [:id :int]
    [:place-name #'schema/Localized]
    [:score {:optional true} :int]]))

(def GameSchema
  (malli/schema
   [:map
    [:away-team #'TeamSchema]
    [:id :int]
    [:game-center-link {:optional true} :string]
    [:game-outcome {:optional true}
     [:map
      [:last-period-type #'PeriodTypeSchema]]]
    [:game-schedule-state [:enum "CNCL" "OK" "PPD"]]
    [:game-state [:enum "CRIT" "FINAL" "FUT" "LIVE" "OFF" "OVER" "PRE"]]
    [:game-type :int]
    [:home-team #'TeamSchema]
    [:period-descriptor
     [:map
      [:number {:optional true} :int]
      [:period-type {:optional true} #'PeriodTypeSchema]]]
    [:season :int]
    [:series-url {:optional true} :string]
    [:series-status {:optional true}
     [:map
      [:bottom-seed-wins :int]
      [:game-number-of-series :int]
      [:round :int]
      [:top-seed-wins :int]]]
    [:start-time-utc :string]
    [:three-min-recap {:optional true} :string]]))

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
  (archive-with-context? [this response _context] (api/archive? this response))
  (cache-key [_] (str "schedule-" start-date-str (when end-date-str (str "-" end-date-str))))
  (description [_] (str "schedule " {:date start-date-str}))
  (response-schema [_] ResponseSchema)
  (url [_] (str api/base-url "/schedule/" start-date-str)))
