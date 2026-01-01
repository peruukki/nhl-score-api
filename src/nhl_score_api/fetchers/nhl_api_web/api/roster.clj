(ns nhl-score-api.fetchers.nhl-api-web.api.roster
  (:require [malli.core :as malli]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.schema :as schema]))

(def PlayerSchema
  (malli/schema
   [:map
    [:first-name #'schema/Localized]
    [:id :int]
    [:last-name #'schema/Localized]
    [:position-code [:enum "C" "D" "G" "L" "R"]]
    [:sweater-number :int]]))

(def ResponseSchema
  (malli/schema
   [:map
    [:defensemen {:optional true} [:vector PlayerSchema]]
    [:forwards {:optional true} [:vector PlayerSchema]]
    [:goalies {:optional true} [:vector PlayerSchema]]]))

(defrecord RosterApiRequest [team-abbrev season]
  api/ApiRequest
  (cache-key [_] (str "roster-" team-abbrev "-" season))
  (description [_] (str "roster " {:team team-abbrev :season season}))
  (get-cache [this response] (api/get-cache-with-context this response nil))
  (get-cache-with-context [_ _response _context] :long-lived)
  (response-schema [_] ResponseSchema)
  (url [_] (str api/base-url "/roster/" team-abbrev "/" season)))
