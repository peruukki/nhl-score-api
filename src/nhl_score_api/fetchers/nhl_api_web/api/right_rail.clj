(ns nhl-score-api.fetchers.nhl-api-web.api.right-rail
  (:require [malli.core :as malli]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.schema :as schema]))

(def TeamGameStatSchema
  (malli/schema
   [:union
    [:map
     [:away-value :int]
     [:category [:enum "blockedShots" "giveaways" "hits" "pim" "sog" "takeaways"]]
     [:home-value :int]]
    [:map
     [:away-value :double]
     [:category [:enum "faceoffWinningPctg" "powerPlayPctg"]]
     [:home-value :double]]
    [:map
     [:away-value :string]
     [:category [:enum "faceoffWins" "powerPlay"]]
     [:home-value :string]]]
   {:registry schema/registry}))

(def ResponseSchema
  (malli/schema
   [:map
    [:team-game-stats {:optional true} [:vector TeamGameStatSchema]]]))

(defrecord RightRailApiRequest [game-id]
  api/ApiRequest
  (archive? [this response] (api/archive-with-context? this response nil))
  (archive-with-context? [_ response _context] (boolean (:three-min-recap (:game-video response))))
  (cache-key [_] (str "right-rail-" game-id))
  (description [_] (str "right-rail " {:game-id game-id}))
  (response-schema [_] ResponseSchema)
  (url [_] (str api/base-url "/gamecenter/" game-id "/right-rail")))
