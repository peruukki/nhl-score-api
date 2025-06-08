(ns nhl-score-api.logging)

; Thread-local dynamic variable
(def ^:dynamic *request-id* nil)

(defn- format-message [message]
  (if *request-id*
    (str "[request-id=" *request-id* "] " message)
    message))

(defn info
  "Logs a message to stdout. If *request-id* is set, prepends the request ID to the message.

   Example:
   (log \"Starting request\")  ;=> Starting request
   (with-request-id \"abc123\"
     (log \"Starting request\"))  ;=> [request-id=abc123] Starting request"
  [message]
  (println (format-message message)))

(defn error
  "Logs an error message to stderr. If *request-id* is set, prepends the request ID to the message.
   Similar to log but writes to stderr instead of stdout.

   Example:
   (log-error \"Database connection failed\")  ;=> [request-id=abc123] Database connection failed (to stderr)"
  [message]
  ; Temporarily redirect output to stderr
  (binding [*out* *err*]
    (println (format-message message))))

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
