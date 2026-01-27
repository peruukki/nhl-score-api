(ns nhl-score-api.fetchers.nhl-api-web.api.index
  (:require [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clojure.data.json :as json]))

(def base-url "https://api-web.nhle.com/v1")

(defn api-response-to-json [api-response]
  (json/read-str api-response :key-fn ->kebab-case-keyword))

(defn get-games-in-date-range [start-date-str end-date-str schedule-response]
  (->> schedule-response
       :game-week
       (filter (if end-date-str
                 #(and (<= (compare start-date-str (:date %)) 0)
                       (<= (compare (:date %) end-date-str) 0))
                 #(= 0 (compare start-date-str (:date %)))))
       (map :games)
       flatten))

(defprotocol ApiRequest
  (cache-key [_])
  (description [_])
  (get-cache [_ response])
  (get-cache-with-context [_ response context])
  (response-schema [_])
  (transform [_ response])
  (url [_]))
