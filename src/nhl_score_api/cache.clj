(ns nhl-score-api.cache
  (:require [clojure.core.cache :as cache]
            [clojure.core.cache.wrapped :as cache.wrapped]
            [clojure.string :as str]
            [nhl-score-api.logging :as logger]
            [nhl-score-api.utils :as utils]))

;; Cache Architecture
;;
;; This module provides a three-tier caching system for API responses:
;;
;; 1. :archive (24-hour TTL, LRU threshold: 64)
;;    - Used by existing API request types for long-term storage
;;    - Values are stored explicitly via cache/store when API request types
;;      return :archive from get-cache-with-context
;;    - Suitable for data that changes infrequently (e.g., finished games)
;;
;; 2. :long-lived (4-hour TTL, LRU threshold: 32)
;;    - Used by new API request types for medium-term storage
;;    - Values are stored explicitly via cache/store when API request types
;;      return :long-lived from get-cache-with-context
;;    - Provides a balance between freshness and cache efficiency
;;
;; 3. :short-lived (1-minute TTL, LRU threshold: 32)
;;    - Used automatically for all API responses
;;    - Values are stored automatically by get-cached when not found in other caches
;;    - Provides rapid access for recently fetched data
;;
;; Lookup order in get-cached: archive → long-lived → short-lived → fetch
;; This ensures the most durable cache is checked first, while short-lived cache
;; provides quick access to recent data for all request types.

(def caches
  {:archive (atom (-> {}
                      (cache/lru-cache-factory :threshold 64)
                      (cache/ttl-cache-factory :ttl (* 24 60 60 1000))))
   :long-lived (atom (-> {}
                         (cache/lru-cache-factory :threshold 32)
                         (cache/ttl-cache-factory :ttl (* 4 60 60 1000))))
   :short-lived (atom (-> {}
                          (cache/lru-cache-factory :threshold 32)
                          (cache/ttl-cache-factory :ttl (* 60 1000))))})

(def cache-sizes (atom {:archive 0 :long-lived 0 :short-lived 0}))

(defn get-cached
  "Returns the value with cache-key from archive cache, long-lived cache, or short-lived cache if it exists.
   Otherwise stores the value returned by value-fn in short-lived cache.

   Returns the value with :from-cache? metadata indicating the value source."
  [cache-key value-fn]
  (let [from-cache? (atom true)
        archive-value (cache.wrapped/lookup (:archive caches) cache-key)
        long-lived-value (cache.wrapped/lookup (:long-lived caches) cache-key)
        value (or
               archive-value
               long-lived-value
               (cache.wrapped/lookup-or-miss
                (:short-lived caches)
                cache-key
                (fn [_]
                  (let [value (value-fn)]
                    (logger/debug (str "Caching " cache-key " value in " :short-lived))
                    (swap! from-cache? not)
                    value))))]
    (with-meta value {:from-cache? @from-cache?})))

(defn store
  "Stores value in the specified cache and returns the value.
   cache-name should be :archive or :long-lived.
   Note: short-lived cache is populated automatically by get-cached and should not be used here."
  [cache-name cache-key value]
  (logger/debug (str "Caching " cache-key " value in " cache-name))
  (cache.wrapped/miss (get caches cache-name) cache-key value)
  value)

(defn evict-from-short-lived!
  "Evicts keys from the short-lived cache."
  [cache-keys]
  (doseq [cache-key cache-keys] (cache.wrapped/evict (:short-lived caches) cache-key)))

(defn log-cache-sizes!
  "Logs all cache sizes if they have changed since the last call and returns the passed value."
  [value]
  (swap! cache-sizes
         (fn [last-cache-sizes]
           (let [current-cache-sizes (utils/fmap-vals #(count @%) caches)]
             (when (not= last-cache-sizes current-cache-sizes)
               (logger/debug (str "Cache sizes: "
                                  (str/join ", "
                                            (map (fn [[id size]] (str id " " size)) current-cache-sizes)))))
             current-cache-sizes)))
  value)
