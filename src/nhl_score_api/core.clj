(ns nhl-score-api.core
  (:require [nhl-score-api.fetchers.nhlstats.fetcher :as fetcher]
            [nhl-score-api.cache :as cache]
            [camel-snake-kebab.core :refer [->camelCaseString]]
            [org.httpkit.server :as server]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [new-reliquary.ring :refer [wrap-newrelic-transaction]])
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
    (get-cached-response request-path latest-scores-api-fn cache-get-fn cache-set-fn 60)

    nil))

(defn json-key-transformer [key]
  (if (keyword? key)
    (->camelCaseString key)
    key))

(defn- format-response [status body]
  {:status status
   :headers {"Access-Control-Allow-Origin" "*"
             "Access-Control-Allow-Headers" "Content-Type"
             "Expires" "0"
             "Content-Type" "application/json; charset=utf-8"}
   :body (json/write-str body :key-fn json-key-transformer)})

(defn request-handler [request]
  (println "Received request" request)
  (try
    (let [success-response (get-response (:uri request) fetcher/fetch-latest-scores cache/get-value cache/set-value)
          response (or success-response {})]
      (format-response (if success-response 200 404) response))
    (catch Exception e
      (println "Caught exception" e)
      (format-response 500 {:error "Server error"}))))

; Send New Relic transaction for each request
(def app (wrap-newrelic-transaction request-handler))
