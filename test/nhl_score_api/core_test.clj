(ns nhl-score-api.core-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.core :refer :all]
            [clojure.data.json :as json]))

(declare assert-status assert-json-content-type assert-body)

(deftest api-routing
  (testing "Root path returns project version"
    (let [version (System/getProperty "nhl-score-api.version")
          response (app {:uri "/"})]
      (is (not (nil? version)) "Project version number is valid")
      (assert-status response 200)
      (assert-json-content-type response)
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

(defn- assert-status [response expected-status]
  (is (= expected-status
         (:status response))
      (str "Status code is " expected-status)))

(defn- assert-json-content-type [response]
  (is (= "application/json; charset=utf-8"
         (get (:headers response) "Content-Type"))
      "Content type is JSON"))

(defn- assert-body [response expected-body message]
  (is (= (json/write-str expected-body)
         (:body response))
      message))
