(ns nhl-score-api.fetchers.nhl-api-web.api.landing
  (:require [nhl-score-api.fetchers.nhl-api-web.api.index :as api]))

(defrecord LandingApiRequest [game-id]
  api/ApiRequest
  (archive? [_ response] (= "OFF" (:game-state response)))
  (cache-key [_] (str "landing-" game-id))
  (description [_] (str "landing " {:game-id game-id}))
  (url [_] (str api/base-url "/gamecenter/" game-id "/landing")))
