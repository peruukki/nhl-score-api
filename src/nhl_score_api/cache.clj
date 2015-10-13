(ns nhl-score-api.cache
  (:require [taoensso.carmine :as car :refer (wcar)]))

(def redis-disabled? (Boolean/valueOf (System/getenv "REDIS_DISABLED")))

(def host (System/getenv "REDISCLOUD_HOSTNAME"))
(def password (System/getenv "REDISCLOUD_PASSWORD"))
(def port (System/getenv "REDISCLOUD_PORT"))

(def server-connection {:pool {} :spec {:host host :port (when port (read-string port)) :password password}})
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
    (when value (println "Found key" key "value from cache"))
    value))

(defn set-value
  "Stores the value for the key in Redis. Returns the value."
  [key value ttl-seconds]
  (when (not redis-disabled?)
    (wcar* (car/set key value)
           (car/expire key ttl-seconds)))
  value)

(defn- validate-configuration []
  (when (nil? host) (throw (Exception. "REDISCLOUD_HOSTNAME env variable is not defined")))
  (when (nil? port) (throw (Exception. "REDISCLOUD_PORT env variable is not defined")))
  (when (not (number? (read-string port))) (throw (Exception. (str "REDISCLOUD_PORT env variable is not a number: " port)))))

(defn- ping-server []
  (validate-configuration)
  (println "Pinging Redis at" (str host ":" port))
  (let [response (wcar* (car/ping))]
    (println "Got response" response "from Redis")))
