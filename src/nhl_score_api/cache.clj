(ns nhl-score-api.cache
  (:require [clojure.core.cache :as cache]
            [clojure.core.cache.wrapped :as cache.wrapped]
            [clojure.string :as str]))

(def caches
  {:archive (atom (-> {}
                      (cache/lru-cache-factory :threshold 64)
                      (cache/ttl-cache-factory :ttl (* 24 60 60 1000))))
   :short-lived (atom (-> {}
                          (cache/lru-cache-factory :threshold 32)
                          (cache/ttl-cache-factory :ttl (* 60 1000))))})

(defn get-cached
  "Returns the value with cache-key from archive cache or short-lived cache if it exists.
   Otherwise stores the value returned by value-fn in short-lived cache.

   Returns the value with :from-cache? metadata indicating the value source."
  [cache-key value-fn]
  (let [from-cache? (atom true)
        archive-value (cache.wrapped/lookup (:archive caches) cache-key)
        value (or
               archive-value
               (cache.wrapped/lookup-or-miss
                (:short-lived caches)
                cache-key
                (fn [_]
                  (let [value (value-fn)]
                    (println "Caching" cache-key "value in" :short-lived)
                    (swap! from-cache? not)
                    value))))]
    (with-meta value {:from-cache? @from-cache?})))

(defn archive
  "Stores value in archive cache and returns the value."
  [cache-key value]
  (println "Caching" cache-key "value in" :archive)
  (cache.wrapped/miss (:archive caches) cache-key value)
  value)

(defn evict-from-short-lived!
  "Evicts keys from the short-lived cache."
  [cache-keys]
  (doseq [cache-key cache-keys] (cache.wrapped/evict (:short-lived caches) cache-key)))

(defn log-cache-sizes!
  "Logs all cache sizes and returns passed value."
  [value]
  (println "Cache sizes:" (str/join ", "
                                    (map (fn [[id cache]] (str id " " (count @cache))) caches)))
  value)
