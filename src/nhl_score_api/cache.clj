(ns nhl-score-api.cache
  (:require [taoensso.carmine :as car :refer (wcar)]))

(def redis-disabled? (Boolean/valueOf (System/getenv "REDIS_DISABLED")))

(def url (or (System/getenv "REDIS_URL") "redis://redis"))

(def server-connection {:pool {} :spec {:uri url}})
(defmacro wcar* [& body] `(car/wcar server-connection ~@body))

(declare ping-server)

(defn connect
  "Checks Redis server connectivity"
  []
  (if redis-disabled?
    (println "Redis disabled by REDIS_DISABLED environment variable")
    (ping-server)))

(defn get-value
  "Returns the value of the key from Redis, or nil if not found."
  [key]
  (let [value (if redis-disabled? nil (wcar* (car/get key)))]
    (if value
      (println "Key" key "value found from cache")
      (println "Key" key "value not in cache"))
    value))

(defn set-value
  "Stores the value for the key in Redis. Returns the value."
  [key value ttl-seconds]
  (when (not redis-disabled?)
    (wcar* (car/set key value)
           (car/expire key ttl-seconds)))
  value)

(defn- ping-server []
  (println "Pinging Redis at" url)
  (let [response (wcar* (car/ping))]
    (println "Got response" response "from Redis")))
