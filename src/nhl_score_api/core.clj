(ns nhl-score-api.core
  (:require [nhl-score-api.fetchers.nhlstats.fetcher :as fetcher]
            [nhl-score-api.cache :as cache]
            [nhl-score-api.utils :refer [fmap-keys fmap-vals format-date]]
            [nhl-score-api.param-parser :as params]
            [nhl-score-api.param-validator :as validate]
            [camel-snake-kebab.core :refer [->camelCaseString ->kebab-case-keyword]]
            [org.httpkit.server :as server]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
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

(defn- get-cached-response [request-path request-params response-fn cache-get-fn cache-set-fn ttl-seconds]
  (let [params-key-part (if request-params
                          (str "?" (str/join "&" (for [[k v] request-params] (str (->camelCaseString k) "=" v))))
                          nil)
        cache-key (str request-path params-key-part)]
    (or (cache-get-fn cache-key)
        (cache-set-fn cache-key (response-fn) ttl-seconds))))

(defn get-response
  [request-path request-params fetch-latest-scores-api-fn fetch-scores-in-date-range-api-fn cache-get-fn cache-set-fn]
  (case request-path
    "/"
    {:status 200
     :body {:version version}}

    "/api/scores/latest"
    {:status 200
     :body (get-cached-response
             request-path
             nil
             fetch-latest-scores-api-fn
             cache-get-fn
             cache-set-fn
             60)}

    "/api/scores"
    (let [expected-params [{:field :start-date :type :date :required? true}
                           {:field :end-date :type :date}]
          parsed-params (params/parse-params expected-params request-params)]
      (if (not-empty (:errors parsed-params))
        {:status 400
         :body {:errors (:errors parsed-params)}}
        (let [start-date (:start-date (:values parsed-params))
              end-date (:end-date (:values parsed-params))
              validation-error (validate/validate-date-range start-date end-date 16)]
          (if validation-error
            {:status 400
             :body {:errors [validation-error]}}
            {:status 200
             :body (get-cached-response
                     request-path
                     (fmap-vals format-date (:values parsed-params))
                     #(fetch-scores-in-date-range-api-fn start-date end-date)
                     cache-get-fn
                     cache-set-fn
                     60)}))))

    {:status 404 :body {}}))

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
    (let [request-params
          (fmap-keys ->kebab-case-keyword (:params request))
          response
          (get-response
            (:uri request)
            request-params
            fetcher/fetch-latest-scores
            fetcher/fetch-scores-in-date-range
            cache/get-value
            cache/set-value)]
      (format-response (:status response) (:body response)))
    (catch Exception e
      (println "Caught exception" e)
      (format-response 500 {:error "Server error"}))))

; Send New Relic transaction for each request
(def app (wrap-newrelic-transaction (wrap-params request-handler)))
