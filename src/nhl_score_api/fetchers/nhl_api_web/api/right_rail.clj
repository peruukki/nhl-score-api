(ns nhl-score-api.fetchers.nhl-api-web.api.right-rail
  (:require [nhl-score-api.fetchers.nhl-api-web.api.index :as api]))

(defrecord RightRailApiRequest [game-id]
  api/ApiRequest
  (archive? [_ response] (boolean (:three-min-recap (:game-video response))))
  (cache-key [_] (str "right-rail-" game-id))
  (description [_] (str "right-rail " {:game-id game-id}))
  (url [_] (str api/base-url "/gamecenter/" game-id "/right-rail")))
