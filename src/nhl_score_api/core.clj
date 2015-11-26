(ns nhl-score-api.core
  (:require [nhl-score-api.api :as api]
            [nhl-score-api.cache :as cache]
            [camel-snake-kebab.core :refer [->camelCaseString]]
            [org.httpkit.server :as server]
            [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import (java.util Properties))
  (:gen-class))

; From http://stackoverflow.com/a/33070806
(defn- get-version-from-pom [dep]
  (let [path (str "META-INF/maven/" (or (namespace dep) (name dep))
                  "/" (name dep) "/pom.properties")
        props (io/resource path)]
    (when props
      (with-open [stream (io/input-stream props)]
        (let [props (doto (Properties.) (.load stream))]
          (.getProperty props "version"))))))

(def version
  (or (System/getProperty "nhl-score-api.version")
      (get-version-from-pom 'nhl-score-api)))

(declare app)

(defn -main [& args]
  (let [ip "0.0.0.0"
        port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
    (println "Starting server version" version)
    (cache/connect)
    (println "Listening on" (str ip ":" port))
    (server/run-server app {:ip ip :port port})))

(defn- get-cached-response [request-path response-fn cache-get-fn cache-set-fn ttl-seconds]
  (or (cache-get-fn request-path)
      (cache-set-fn request-path (response-fn) ttl-seconds)))

(defn get-response [request-path latest-scores-api-fn cache-get-fn cache-set-fn]
  (case request-path
    "/"
    {:version version}

    "/api/scores/latest"
    (get-cached-response request-path latest-scores-api-fn cache-get-fn cache-set-fn 300)

    nil))

(defn json-key-transformer [key]
  (if (keyword? key)
    (->camelCaseString key)
    key))

(defn app [request]
  (println "Received request" request)
  (let [success-response (get-response (:uri request) api/fetch-latest-scores cache/get-value cache/set-value)
        response (or success-response {})]
    {:status (if success-response 200 404)
     :headers {"Access-Control-Allow-Origin" "*"
               "Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str response :key-fn json-key-transformer)}))
