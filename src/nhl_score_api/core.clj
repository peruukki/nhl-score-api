(ns nhl-score-api.core
  (:require [nhl-score-api.api :as api]
            [camel-snake-kebab.core :refer [->camelCaseString]]
            [org.httpkit.server :as server]
            [clojure.data.json :as json]))

(def version (System/getProperty "nhl-score-api.version"))

(declare app)

(defn -main [& args]
  (let [ip (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_IP" "0.0.0.0")
        port (Integer/parseInt (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_PORT" "8080"))]
    (println "Starting server version" (str version ",") "listening on" (str ip ":" port))
    (server/run-server app {:ip ip :port port})))

(defn- get-response [request-path]
  (case request-path
    "/"
    {:version version}

    "/api/scores/latest"
    (api/fetch-latest-scores)

    nil))

(defn app [request]
  (println "Received request" request)
  (let [success-response (get-response (:uri request))
        response (or success-response {})]
    {:status (if success-response 200 404)
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str response :key-fn ->camelCaseString)}))
