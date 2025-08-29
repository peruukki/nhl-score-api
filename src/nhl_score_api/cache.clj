(ns nhl-score-api.cache
  (:require [clojure.core.cache :as cache]
            [clojure.core.cache.wrapped :as cache.wrapped]
            [clojure.string :as str]
            [nhl-score-api.logging :as logger]
            [nhl-score-api.utils :as utils]))

(def caches
  {:archive (atom (-> {}
                      (cache/lru-cache-factory :threshold 64)
                      (cache/ttl-cache-factory :ttl (* 24 60 60 1000))))
   :short-lived (atom (-> {}
                          (cache/lru-cache-factory :threshold 32)
                          (cache/ttl-cache-factory :ttl (* 60 1000))))})

(def cache-sizes (atom {:archive 0 :short-lived 0}))

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
                    (logger/debug (str "Caching " cache-key " value in " :short-lived))
                    (swap! from-cache? not)
                    value))))]
    (with-meta value {:from-cache? @from-cache?})))

(defn archive
  "Stores value in archive cache and returns the value."
  [cache-key value]
  (logger/debug (str "Caching " cache-key " value in " :archive))
  (cache.wrapped/miss (:archive caches) cache-key value)
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
