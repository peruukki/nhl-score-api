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
    [:game-reports {:optional true}
     [:map
      [:rosters {:optional true} :string]]]
    [:team-game-stats {:optional true} [:vector TeamGameStatSchema]]]))

(defrecord RightRailApiRequest [game-id]
  api/ApiRequest
  (cache-key [_] (str "right-rail-" game-id))
  (description [_] (str "right-rail " {:game-id game-id}))
  (get-cache [this response] (api/get-cache-with-context this response nil))
  (get-cache-with-context [_ response _context] (when (:three-min-recap (:game-video response)) :archive))
  (response-schema [_] ResponseSchema)
  (transform [_ response] (api/api-response-to-json response))
  (url [_] (str api/base-url "/gamecenter/" game-id "/right-rail")))
