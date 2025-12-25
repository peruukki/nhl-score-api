(ns nhl-score-api.fetchers.nhl-api-web.api.landing
  (:require [malli.core :as malli]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.schema :as schema]))

(def PeriodDescriptorSchema
  (malli/schema
   [:map
    [:number :int]
    [:period-type :string]]))

(def PlayerSchema
  (malli/schema
   [:map
    [:first-name #'schema/Localized]
    [:last-name #'schema/Localized]
    [:name #'schema/Localized]
    [:player-id :int]]))

(def AssistSchema
  (malli/schema
   [:merge
    PlayerSchema
    [:map
     [:assists-to-date :int]]]
   {:registry schema/registry}))

(def GoalSchema
  (malli/schema
   [:merge
    PlayerSchema
    [:map
     [:assists [:vector AssistSchema]]
     [:goal-modifier
      [:enum "awarded" "awarded-empty-net" "empty-net" "none" "own-goal" "own-goal-empty-net" "penalty-shot"]]
     [:goals-to-date {:optional true} :int]
     [:strength [:enum "ev" "pp" "sh"]]]]
   {:registry schema/registry}))

(def ResponseSchema
  (malli/schema
   [:map
    [:clock {:optional true}
     [:map
      [:in-intermission :boolean]
      [:seconds-remaining :int]
      [:time-remaining :string]]]
    [:game-state #'schema/GameState]
    [:game-type :int]
    [:period-descriptor {:optional true} #'PeriodDescriptorSchema]
    [:summary {:optional true}
     [:map
      [:scoring
       [:vector
        [:map
         [:goals [:vector GoalSchema]]
         [:period-descriptor #'PeriodDescriptorSchema]]]]]]]))

(defrecord LandingApiRequest [game-id]
  api/ApiRequest
  (archive? [this response] (api/archive-with-context? this response nil))
  (archive-with-context? [_ response _context] (= "OFF" (:game-state response)))
  (cache-key [_] (str "landing-" game-id))
  (description [_] (str "landing " {:game-id game-id}))
  (response-schema [_] ResponseSchema)
  (url [_] (str api/base-url "/gamecenter/" game-id "/landing")))
