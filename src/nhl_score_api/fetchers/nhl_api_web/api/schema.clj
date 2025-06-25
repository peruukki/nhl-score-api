(ns nhl-score-api.fetchers.nhl-api-web.api.schema
  (:require [malli.core :as malli]
            [malli.util :as malli-util]))

(def registry (merge (malli/default-schemas) (malli-util/schemas)))

(def GameState [:enum "CRIT" "FINAL" "FUT" "LIVE" "OFF" "OVER" "PRE"])

(def Localized [:map [:default :string]])
