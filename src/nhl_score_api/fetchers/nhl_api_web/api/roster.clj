(ns nhl-score-api.fetchers.nhl-api-web.api.roster
  (:require [malli.core :as malli]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.schema :as schema]))

(def PlayerSchema
  (malli/schema
   [:map
    [:id :int]
    [:first-name #'schema/Localized]
    [:last-name #'schema/Localized]
    [:sweater-number :int]
    [:position-code [:enum "C" "D" "G" "L" "R"]]
    [:headshot {:optional true} :string]
    [:shoots-catches {:optional true} [:enum "L" "R"]]
    [:height-in-inches {:optional true} :int]
    [:weight-in-pounds {:optional true} :int]
    [:height-in-centimeters {:optional true} :int]
    [:weight-in-kilograms {:optional true} :int]
    [:birth-date {:optional true} :string]
    [:birth-city {:optional true} #'schema/Localized]
    [:birth-country {:optional true} :string]
    [:birth-state-province {:optional true} #'schema/Localized]]))

(def ResponseSchema
  (malli/schema
   [:map
    [:forwards {:optional true} [:vector PlayerSchema]]
    [:defensemen {:optional true} [:vector PlayerSchema]]
    [:goalies {:optional true} [:vector PlayerSchema]]]))

(defrecord RosterApiRequest [team-abbrev season]
  api/ApiRequest
  (archive? [this response] (api/archive-with-context? this response nil))
  (archive-with-context? [_ _response _context]
    false)
  (cache-key [_] (str "roster-" team-abbrev "-" season))
  (description [_] (str "roster " {:team team-abbrev :season season}))
  (response-schema [_] ResponseSchema)
  (url [_] (str api/base-url "/roster/" team-abbrev "/" season)))
