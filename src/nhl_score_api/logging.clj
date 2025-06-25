(ns nhl-score-api.logging)

; Thread-local dynamic variable
(def ^:dynamic *request-id* nil)

(defn- format-message [level message]
  (str (if *request-id*
         (str "[request-id=" *request-id* "] ")
         "")
       level " " message))

(defn info
  "Logs a message to stdout. If *request-id* is set, prepends the request ID to the message.

   Example:
   (info \"Starting request\")  ;=> INFO Starting request
   (with-request-id \"abc123\"
     (info \"Starting request\"))  ;=> [request-id=abc123] INFO Starting request"
  [message]
  (println (format-message "INFO" message)))

(defn warn
  "Logs a warning message to stdout. If *request-id* is set, prepends the request ID to the message.

   Example:
   (warn \"Potential issue detected\")  ;=> WARN Potential issue detected
   (with-request-id \"abc123\"
     (warn \"Potential issue detected\"))  ;=> [request-id=abc123] WARN Potential issue detected"
  [message]
  (println (format-message "WARN" message)))

(defn error
  "Logs an error message to stderr. If *request-id* is set, prepends the request ID to the message.
   Similar to log but writes to stderr instead of stdout.

   Example:
   (error \"Database connection failed\")  ;=> [request-id=abc123] ERROR Database connection failed (to stderr)"
  [message]
  ; Temporarily redirect output to stderr
  (binding [*out* *err*]
    (println (format-message "ERROR" message))))

(defmacro with-request-id
  "Establishes a dynamic scope where *request-id* is bound to the given value.
   All log and log-error calls within the body will include the request ID in their output.
   The binding is thread-local and automatically cleaned up when the scope ends.

   Example:
   (with-request-id \"abc123\"
     (log \"Starting request\")
     (do-something)
     (log \"Request complete\"))
   ; Outputs:
   ; [request-id=abc123] Starting request
   ; [request-id=abc123] Request complete"
  [request-id & body]
  `(binding [*request-id* ~request-id]
     ~@body))
