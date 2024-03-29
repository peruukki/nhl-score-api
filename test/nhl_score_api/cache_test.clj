(ns nhl-score-api.cache-test
  (:require [clojure.core.cache.wrapped :as cache.wrapped]
            [clojure.test :refer [deftest is use-fixtures]]
            [nhl-score-api.cache :as cache]))

(def value-fn-called (atom false))

(defn- seed-caches! [f]
  (cache.wrapped/seed
   (:archive cache/caches) {"archive-key" {:value "archive-value"}
                            "common-key" {:value "common-value-from-archive"}})
  (cache.wrapped/seed
   (:short-lived cache/caches) {"short-lived-key" {:value "short-lived-value"}
                                "another-short-lived-key" {:value "another-short-lived-value"}
                                "common-key" {:value "common-value-from-short-lived"}})
  (f))

(defn- reset-spies! [f]
  (reset! value-fn-called false)
  (f))

(defn- value-fn []
  (reset! value-fn-called true)
  {:value "value-fn-value"})

(use-fixtures :each seed-caches! reset-spies!)

(deftest get-cached-value-found-from-archive-cache
  (let [result (cache/get-cached "archive-key" value-fn)]
    (is (= {:value "archive-value"} result) "result is from archive cache")
    (is (= {:from-cache? true} (meta result)) "result is from cache")
    (is (= false @value-fn-called) "value-fn was not called")))

(deftest get-cached-value-found-from-short-lived-cache
  (let [result (cache/get-cached "short-lived-key" value-fn)]
    (is (= {:value "short-lived-value"} result) "result is from short-lived cache")
    (is (= {:from-cache? true} (meta result)) "result is from cache")
    (is (= false @value-fn-called) "value-fn was not called")))

(deftest get-cached-value-found-from-both-caches
  (let [result (cache/get-cached "common-key" value-fn)]
    (is (= {:value "common-value-from-archive"} result) "result is from archive cache")
    (is (= {:from-cache? true} (meta result)) "result is from cache")
    (is (= false @value-fn-called) "value-fn was not called")))

(deftest get-cached-value-not-found-from-cache
  (let [result (cache/get-cached "not-found-key" value-fn)]
    (is (= {:value "value-fn-value"} result) "result is from fetch-fn")
    (is (= {:from-cache? false} (meta result)) "result is not from cache")
    (is (= true @value-fn-called) "value-fn was called")))

(deftest archive-storing-value
  (is (= nil
         (cache.wrapped/lookup (:archive cache/caches) "key")) "value is not in archive")
  (is (= {:value "value"}
         (cache/archive "key" {:value "value"})) "archived value is returned")
  (is (= {:value "value"}
         (cache.wrapped/lookup (:archive cache/caches) "key")) "value is in archive"))

(deftest evict-from-short-lived!-removing-keys
  (is (= #{"archive-key" "common-key"}
         (set (keys @(:archive cache/caches)))) "archive has 2 keys")
  (is (= #{"short-lived-key" "another-short-lived-key" "common-key"}
         (set (keys @(:short-lived cache/caches)))) "short-lived has 3 keys")
  (cache/evict-from-short-lived! ["short-lived-key" "common-key" "non-existing-key"])
  (is (= #{"archive-key" "common-key"}
         (set (keys @(:archive cache/caches)))) "archive still has 2 keys")
  (is (= #{"another-short-lived-key"}
         (set (keys @(:short-lived cache/caches)))) "short-lived now has 1 key"))
