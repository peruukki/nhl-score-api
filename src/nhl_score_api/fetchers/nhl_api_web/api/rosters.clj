(ns nhl-score-api.fetchers.nhl-api-web.api.rosters
  (:require [clojure.string :as str]
            [malli.core :as malli]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]))

(def ResponseSchema
  (malli/schema
   [:and
    [:string {:min 1}]
    [:fn {:error/message "Response must contain <html> tag"}
     #(str/includes? % "<html>")]]))

(defrecord RostersApiRequest [rosters-url game-id]
  api/ApiRequest
  (cache-key [_] (str "rosters-" game-id))
  (description [_] (str "rosters " {:game-id game-id}))
  (get-cache [this response] (api/get-cache-with-context this response nil))
  (get-cache-with-context [_ _response _context] :archive)
  (response-schema [_] ResponseSchema)
  (transform [_ response] response)
  (url [_] rosters-url))
