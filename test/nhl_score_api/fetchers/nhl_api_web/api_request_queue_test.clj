(ns nhl-score-api.fetchers.nhl-api-web.api-request-queue-test
  (:require [clj-http.fake :refer [with-global-fake-routes]]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [nhl-score-api.fetchers.nhl-api-web.api-request-queue :as api-request-queue]))

(defn setup-queue [f]
  (api-request-queue/init-queue!)
  (f)
  (api-request-queue/stop-queue!))

(use-fixtures :each setup-queue)

(deftest fetch-throws-when-not-initialized
  (testing "fetch throws exception when queue not initialized"
    (api-request-queue/stop-queue!)
    (is (thrown? IllegalStateException
                 (api-request-queue/fetch "http://example.com/api" {} "test request"))
        "Throws IllegalStateException when queue not initialized")))

(deftest basic-request-succeeds
  (testing "Single request completes successfully"
    (with-global-fake-routes
      {"http://example.com/api"
       (fn [_request]
         {:status 200
          :headers {"Content-Type" "application/json"}
          :body "{\"message\": \"success\"}"})}
      (let [response (api-request-queue/fetch "http://example.com/api" {} "test request")]
        (is (= 200 (:status response)) "Response status is 200")
        (is (= "{\"message\": \"success\"}" (:body response)) "Response body is correct")))))

(deftest concurrency-limit-enforced
  (testing "Only max-concurrent requests execute simultaneously"
    (api-request-queue/stop-queue!)
    (api-request-queue/init-queue! 2)
    (let [response-controls (atom {})
          request-started-promises (atom {})
          urls ["http://example.com/api1"
                "http://example.com/api2"
                "http://example.com/api3"
                "http://example.com/api4"]]
      (with-global-fake-routes
        (into {}
              (map (fn [url]
                     [url (fn [_request]
                            (deliver (get @request-started-promises url) :started)
                            @(get @response-controls url))]))
              urls)
        (let [; Set up response controls and start promises first
              _ (doseq [url urls]
                  (swap! response-controls assoc url (promise))
                  (swap! request-started-promises assoc url (promise)))
              ; Start all 4 requests simultaneously (force evaluation with doall)
              futures (doall (map (fn [url]
                                    (future (api-request-queue/fetch url {} (str "request " url))))
                                  urls))
              ; Wait for exactly 2 requests to start (max-concurrent=2)
              ; Wait for any 2 promises to be delivered by waiting on futures
              all-promises (vals @request-started-promises)
              wait-futures (map #(future (deref % 2000 :timeout)) all-promises)
              _ (let [completed-count (atom 0)]
                  (doseq [wait-future wait-futures]
                    (when (< @completed-count 2)
                      (let [result @wait-future]
                        (when (not= :timeout result)
                          (swap! completed-count inc)))))
                  (when (< @completed-count 2)
                    (throw (Exception. (str "Only " @completed-count " requests started within timeout, expected 2")))))
              ; Check that only 2 have started
              started-urls (keys (filter (fn [[_ p]] (realized? p)) @request-started-promises))]
          (is (<= (count started-urls) 2) (str "Only " (count started-urls) " requests started, should be <= 2"))
          ; Deliver responses for the requests that started
          (doseq [url started-urls]
            (deliver (get @response-controls url) {:status 200 :body "response"}))
          ; Wait for those to complete (with timeout)
          (doseq [[url future-result] (map vector urls futures)]
            (when (contains? (set started-urls) url)
              (let [result (deref future-result 2000 :timeout)]
                (if (= :timeout result)
                  (throw (Exception. (str "Future for " url " timed out waiting for response")))
                  (is (= 200 (:status result)) (str "Request for " url " completed successfully"))))))
          ; Wait for remaining 2 requests to start
          (doseq [[url promise] @request-started-promises]
            (when-not (realized? promise)
              (let [result (deref promise 2000 :timeout)]
                (when (= :timeout result)
                  (throw (Exception. (str "Timeout waiting for request " url " to start")))))))
          ; Verify all 4 have started
          (let [all-started-urls (keys (filter (fn [[_ p]] (realized? p)) @request-started-promises))]
            (is (= 4 (count all-started-urls)) "All 4 requests have started"))
          ; Deliver responses for all remaining URLs (those that haven't been delivered yet)
          (doseq [url urls]
            (when-not (realized? (get @response-controls url))
              (deliver (get @response-controls url) {:status 200 :body "response"})))
          ; Wait for all to complete (with timeout)
          (doseq [[idx future-result] (map-indexed vector futures)]
            (let [result (deref future-result 2000 :timeout)]
              (if (= :timeout result)
                (throw (Exception. (str "Future " idx " timed out waiting for response")))
                (is (= 200 (:status result)) "All requests completed successfully")))))))))

(deftest request-deduplication
  (testing "Multiple simultaneous requests for same URL share the same HTTP call"
    (let [http-call-count (atom 0)
          response-control (promise)
          http-call-started (promise)
          url "http://example.com/api"]
      (with-global-fake-routes
        {url (fn [_request]
               (swap! http-call-count inc)
               (deliver http-call-started :started)
               @response-control)}
        ; Start 3 simultaneous requests for the same URL
        (let [futures (doall (map (fn [i]
                                    (future (api-request-queue/fetch url {} (str "request " i))))
                                  (range 3)))]
          ; Wait for HTTP call to start (with timeout to avoid hanging)
          (let [result (deref http-call-started 2000 :timeout)]
            (when (= :timeout result)
              (throw (Exception. "Timeout waiting for HTTP call to start"))))
          ; Only 1 HTTP call should have been made
          (is (= 1 @http-call-count) "Only one HTTP call was made")
          ; Deliver response
          (deliver response-control {:status 200 :body "shared response"})
          ; All 3 callers should get the same result
          (doseq [future-result futures]
            (let [response @future-result]
              (is (= 200 (:status response)) "Response status is 200")
              (is (= "shared response" (:body response)) "All callers get same response")))
          ; Still only 1 HTTP call
          (is (= 1 @http-call-count) "Still only one HTTP call was made"))))))

(deftest error-handling
  (testing "HTTP errors are properly propagated"
    (with-global-fake-routes
      {"http://example.com/api"
       (fn [_request]
         {:status 404
          :headers {}
          :body "Not Found"})}
      (let [ex (try
                 (api-request-queue/fetch "http://example.com/api" {} "test request")
                 nil
                 (catch Exception e e))]
        (is (some? ex) "Exception is thrown for error status")
        (is (= 404 (get-in (ex-data ex) [:status])) "Exception contains status 404")))))

(deftest exception-handling
  (testing "Exceptions thrown by http/get are caught and rethrown to caller"
    (with-global-fake-routes
      {"http://example.com/api"
       (fn [_request]
         (throw (Exception. "Network error")))}
      (is (thrown? Exception
                   (api-request-queue/fetch "http://example.com/api" {} "test request"))
          "Exception is thrown to caller"))))

(deftest pending-requests-cleanup
  (testing "Pending requests are removed from map after completion"
    (let [response-control (promise)
          url "http://example.com/api"]
      (with-global-fake-routes
        {url (fn [_request]
               @response-control)}
        ; Start a request
        (let [future-result (future (api-request-queue/fetch url {} "test request"))]
          ; Deliver response
          (deliver response-control {:status 200 :body "response"})
          ; Wait for completion (this also ensures cleanup has happened)
          @future-result
          ; Start another request for same URL - should make new HTTP call
          (let [http-call-count (atom 0)
                response-control2 (promise)]
            (with-global-fake-routes
              {url (fn [_request]
                     (swap! http-call-count inc)
                     @response-control2)}
              (let [future-result2 (future (api-request-queue/fetch url {} "test request 2"))]
                (deliver response-control2 {:status 200 :body "response2"})
                @future-result2
                ; Should have made a new HTTP call (not reused)
                (is (= 1 @http-call-count) "New HTTP call was made after cleanup")))))))))
