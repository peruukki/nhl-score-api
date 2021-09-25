(ns nhl-score-api.core-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.core :refer :all]
            [clojure.data.json :as json]))

(def cache-get-key (atom nil))
(def cache-set-key (atom nil))
(def cache-set-value (atom nil))
(def cache-set-ttl-seconds (atom nil))
(def latest-scores-fetched (atom false))
(def scores-in-date-range-fetched (atom false))

(def latest-scores {:teams [] :scores {} :goals []})
(def scores-in-date-range [{:teams [] :scores {} :goals []}])

(declare assert-status assert-json-content-type assert-cors-enabled assert-browser-caching-disabled assert-body)
(declare reset-cache-state! cache-get-fn cache-set-fn)
(declare latest-scores-api-fn)
(declare scores-in-date-range-api-fn)


(deftest api-routing
  (testing "Root path returns project version"
    (let [version (System/getProperty "nhl-score-api.version")
          response (app {:uri "/"})]
      (is (not (nil? version)) "Project version number is valid")
      (assert-status response 200)
      (assert-json-content-type response)
      (assert-cors-enabled response)
      (assert-body response {:version version} "Response contains version number")))

  (testing "Unknown path returns 404 Not Found"
    (let [response (app {:uri "/this-path-does-not-exist"})]
      (assert-status response 404)
      (assert-json-content-type response)
      (assert-body response {} "Response contains empty body"))))

(deftest json-key-transforming
  (testing "Response JSON keyword key is transformed to camel case"
    (is (= "goalCount"
           (json-key-transformer :goal-count)) "JSON key is in camel case"))

  (testing "Response JSON string key is not transformed"
    (is (= "goal-count"
           (json-key-transformer "goal-count")) "Key is not transformed")))

(deftest caching
  (testing "Root path request is not cached"
    (reset-cache-state!)
    (get-response "/" {} latest-scores-api-fn scores-in-date-range-api-fn cache-get-fn cache-set-fn)
    (is (= nil
           @cache-get-key) "Cache was not searched")
    (is (= nil
           @cache-set-key) "Nothing was stored in the cache"))

  (testing "Latest scores request is cached for 1 minute"
    (reset-cache-state!)
    (let [path "/api/scores/latest"]
      (get-response path {} latest-scores-api-fn scores-in-date-range-api-fn cache-get-fn cache-set-fn)
      (is (= path
             @cache-get-key) "Cache was searched for request path key")
      (is (= true
             @latest-scores-fetched) "Latest scores were fetched")
      (is (= path
             @cache-set-key) "Request path was stored in the cache as key")
      (is (= latest-scores
             @cache-set-value) "Response was stored in the cache as value")
      (is (= 60
             @cache-set-ttl-seconds) "Key time-to-live was set to 60 seconds")

      (reset! latest-scores-fetched false)
      (get-response path {} latest-scores-api-fn scores-in-date-range-api-fn cache-get-fn cache-set-fn)
      (is (= false
             @latest-scores-fetched) "Latest scores were not fetched")))

  (testing "Scores in date range request is cached for 1 minute"
    (reset-cache-state!)
    (let [path "/api/scores"
          params {:start-date "2021-09-25" :end-date "2021-09-26"}]
      (get-response path params latest-scores-api-fn scores-in-date-range-api-fn cache-get-fn cache-set-fn)
      (is (= path
             @cache-get-key) "Cache was searched for request path key")
      (is (= true
             @scores-in-date-range-fetched) "Scores were fetched")
      (is (= path
             @cache-set-key) "Request path was stored in the cache as key")
      (is (= scores-in-date-range
             @cache-set-value) "Response was stored in the cache as value")
      (is (= 60
             @cache-set-ttl-seconds) "Key time-to-live was set to 60 seconds")

      (reset! scores-in-date-range-fetched false)
      (get-response path params latest-scores-api-fn scores-in-date-range-api-fn cache-get-fn cache-set-fn)
      (is (= false
             @scores-in-date-range-fetched) "Scores were not fetched"))))

(deftest browser-caching
  (testing "Browser caching is disabled by response headers")
    (let [response (app {:uri "/"})]
      (assert-browser-caching-disabled response)))

(defn- assert-status [response expected-status]
  (is (= expected-status
         (:status response))
      (str "Status code is " expected-status)))

(defn- assert-json-content-type [response]
  (is (= "application/json; charset=utf-8"
         (get (:headers response) "Content-Type"))
      "Content type is JSON"))

(defn- assert-cors-enabled [response]
  (is (= "*"
         (get (:headers response) "Access-Control-Allow-Origin"))
      "Access-Control-Allow-Origin allows all sites")
  (is (= "Content-Type"
         (get (:headers response) "Access-Control-Allow-Headers"))
      "Access-Control-Allow-Headers allows Content-Type header"))

(defn- assert-browser-caching-disabled [response]
  (is (= "0"
         (get (:headers response) "Expires"))
      "Expires header disables browser caching"))

(defn- assert-body [response expected-body message]
  (is (= (json/write-str expected-body)
         (:body response))
      message))

(defn- reset-cache-state! []
  (reset! cache-get-key nil)
  (reset! cache-set-key nil)
  (reset! cache-set-value nil)
  (reset! cache-set-ttl-seconds nil)
  (reset! latest-scores-fetched false)
  (reset! scores-in-date-range-fetched false))

(defn- cache-get-fn [key]
  (reset! cache-get-key key)
  @cache-set-value)

(defn- cache-set-fn [key value ttl-seconds]
  (reset! cache-set-key key)
  (reset! cache-set-value value)
  (reset! cache-set-ttl-seconds ttl-seconds))

(defn- latest-scores-api-fn []
  (reset! latest-scores-fetched true)
  latest-scores)

(defn- scores-in-date-range-api-fn [start-date end-date]
  (reset! scores-in-date-range-fetched true)
  scores-in-date-range)
