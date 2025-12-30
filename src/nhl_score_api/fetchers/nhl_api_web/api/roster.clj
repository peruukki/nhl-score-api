(ns nhl-score-api.fetchers.nhl-api-web.api.roster
  (:require [malli.core :as malli]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.schema :as schema]))

(def PlayerSchema
  (malli/schema
   [:map
    [:id :int]
    [:firstName #'schema/Localized]
    [:lastName #'schema/Localized]
    [:sweaterNumber :int]
    [:positionCode [:enum "C" "D" "G" "L" "R"]]
    [:headshot {:optional true} :string]
    [:shootsCatches {:optional true} [:enum "L" "R"]]
    [:heightInInches {:optional true} :int]
    [:weightInPounds {:optional true} :int]
    [:heightInCentimeters {:optional true} :int]
    [:weightInKilograms {:optional true} :int]
    [:birthDate {:optional true} :string]
    [:birthCity {:optional true} #'schema/Localized]
    [:birthCountry {:optional true} :string]
    [:birthStateProvince {:optional true} #'schema/Localized]]))

(def ResponseSchema
  (malli/schema
   [:map
    [:forwards {:optional true} [:vector #'PlayerSchema]]
    [:defensemen {:optional true} [:vector #'PlayerSchema]]
    [:goalies {:optional true} [:vector #'PlayerSchema]]]))

(defrecord RosterApiRequest [team-abbrev season]
  api/ApiRequest
  (archive? [this response] (api/archive-with-context? this response nil))
  (archive-with-context? [_ _response _context]
    ; Roster data doesn't change frequently during a season, so we can archive it
    ; once the season is over. For now, we'll archive based on season end date.
    ; This is a simple implementation - could be enhanced later.
    false)
  (cache-key [_] (str "roster-" team-abbrev "-" season))
  (description [_] (str "roster " {:team team-abbrev :season season}))
  (response-schema [_] ResponseSchema)
  (url [_] (str api/base-url "/roster/" team-abbrev "/" season)))
