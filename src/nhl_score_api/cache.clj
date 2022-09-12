(ns nhl-score-api.cache
  (:require [clojure.core.memoize :as memo]))

(defn get-cached-fn
  "Returns a function whose result is returned from cache, or if not found, executes the
    given function and stores its return value in the cache with the given time-to-live."
  [f fname ttl-millis]
  (memo/ttl #(do (println "Function" fname "result not in cache, args:" %&)
                 (apply f %&))
            :ttl/threshold ttl-millis))
