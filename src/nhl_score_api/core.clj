(ns nhl-score-api.core
  (:require [org.httpkit.server :as server]
            [clojure.data.json :as json]))

(declare app)

(defn -main [& args]
  (let [ip (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_IP" "0.0.0.0")
        port (Integer/parseInt (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_PORT" "8080"))]
    (println "Starting server, listening on" (str ip ":" port))
    (server/run-server app {:ip ip :port port})))

(defn app [req]
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/write-str [])})
