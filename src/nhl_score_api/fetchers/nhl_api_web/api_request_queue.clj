(ns nhl-score-api.fetchers.nhl-api-web.api-request-queue
  (:require [clj-http.client :as http]
            [clojure.core.async :as async]
            [nhl-score-api.logging :as logger]))

; Outgoing API request throttling: limit to max 3 concurrent requests
(def ^:private max-concurrent-api-requests 3)
(def ^:private pending-requests (atom {}))
(def ^:private pending-requests-lock (Object.)) ; Map of URL -> promise for deduplication
(def ^:private request-queue (async/chan 100))

(defn- api-request-worker [id]
  (async/go-loop []
    (when-let [[url options request-description result-promise request-id] (async/<! request-queue)]
      (try
        (logger/with-request-id request-id
          (logger/info (str "[" id "] Fetching " request-description))
          (let [start-time (System/currentTimeMillis)
                response (http/get url options)]
            (logger/info (str "[" id "] Fetched " request-description
                              " (took " (- (System/currentTimeMillis) start-time) " ms)"))
            (deliver result-promise response)))
        (catch Exception e
          (deliver result-promise e))
        (finally
          ; Remove from pending requests when done
          (locking pending-requests-lock
            (swap! pending-requests dissoc url))))
      (recur))))

; Start worker pool
(doseq [id (range max-concurrent-api-requests)]
  (api-request-worker (inc id)))

(defn fetch
  "API request with throttling: limits to max 3 concurrent requests.
   Requests are queued and processed by worker pool.
   Duplicate requests for the same URL share the same pending request."
  [url options request-description]
  (let [result-promise
        (locking pending-requests-lock
          (if-let [existing-promise (get @pending-requests url)]
            ; There's already a pending request for this URL, wait for it
            (do
              (logger/debug (str "Reusing pending request for " request-description))
              existing-promise)
            ; No pending request, create a new one
            (let [new-promise (promise)
                  request-id logger/*request-id*]
              ; Track the pending request
              (swap! pending-requests assoc url new-promise)
              ; Queue the actual HTTP request
              (async/>!! request-queue [url options request-description new-promise request-id])
              new-promise)))
        result @result-promise]
    ; Block until result is available and handle exceptions
    (if (instance? Exception result)
      (throw result)
      result)))
