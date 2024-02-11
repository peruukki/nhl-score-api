(ns nhl-score-api.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [nhl-score-api.core :refer [app get-response json-key-transformer]]
            [clojure.data.json :as json]))

(def latest-scores-fetched (atom false))
(def scores-in-date-range-fetched (atom false))

(def latest-scores {:teams {} :scores {} :goals []})
(def scores-in-date-range [{:teams {} :scores {} :goals []}])

(declare assert-status assert-json-content-type assert-cors-enabled assert-browser-caching-disabled assert-body)
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

(deftest latest-scores-route
  (testing "Returns success response"
    (let [path "/api/scores/latest"
          response (get-response path {} latest-scores-api-fn scores-in-date-range-api-fn)]
      (is (= 200 (:status response)) "Response status is 200"))))

(deftest scores-in-date-range-route
  (testing "Returns success response with :start-date parameter"
    (let [path "/api/scores"
          response (get-response path {:start-date "2021-10-03"} latest-scores-api-fn scores-in-date-range-api-fn)]
      (is (= 200 (:status response)) "Response status is 200")))

  (testing "Returns success response with :start-date and :end-date parameters"
    (let [path "/api/scores"
          response (get-response path {:start-date "2021-10-03" :end-date "2021-10-04"} latest-scores-api-fn scores-in-date-range-api-fn)]
      (is (= 200 (:status response)) "Response status is 200")))

  (testing "Returns failure response without parameters"
    (let [path "/api/scores"
          response (get-response path {} latest-scores-api-fn scores-in-date-range-api-fn)]
      (is (= 400 (:status response)) "Response status is 400")
      (is (= {:errors ["Missing required parameter startDate"]} (:body response)) "Response body contains errors")))

  (testing "Returns failure response with invalid date range parameters"
    (let [path "/api/scores"
          response (get-response path {:start-date "2021-10-01" :end-date "2021-10-17"} latest-scores-api-fn scores-in-date-range-api-fn)]
      (is (= 400 (:status response)) "Response status is 400")
      (is (= {:errors ["Date range exceeds maximum limit of 7 days"]} (:body response)) "Response body contains errors"))))

(deftest json-key-transforming
  (testing "Response JSON keyword key is transformed to camel case"
    (is (= "goalCount"
           (json-key-transformer :goal-count)) "JSON key is in camel case"))

  (testing "Response JSON string key is not transformed"
    (is (= "goal-count"
           (json-key-transformer "goal-count")) "Key is not transformed")))

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

(defn- latest-scores-api-fn []
  (reset! latest-scores-fetched true)
  latest-scores)

#_{:clj-kondo/ignore [:unused-binding]}
(defn- scores-in-date-range-api-fn [start-date end-date]
  (reset! scores-in-date-range-fetched true)
  scores-in-date-range)
