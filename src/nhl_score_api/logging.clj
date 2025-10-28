(ns nhl-score-api.logging
  (:require [clojure.string :as str]))

; Thread-local dynamic variable
(def ^:dynamic *request-id* nil)

; ANSI color codes
(def ^:private colors
  {:blue "\u001b[34m"
   :green "\u001b[32m"
   :red "\u001b[31m"
   :reset "\u001b[0m"
   :yellow "\u001b[33m"})

(def ^:private level-colors
  {"DEBUG" :blue
   "ERROR" :red
   "INFO" :green
   "WARN" :yellow})

(defn- colorize [color text]
  (str (get colors color) text (get colors :reset)))

(defn- format-message [level message]
  (let [colored-level (if-let [color (get level-colors level)]
                        (colorize color level)
                        level)]
    (str (if *request-id*
           (str "[" (subs *request-id* 0 (min (count *request-id*) 8)) "] ")
           "")
         colored-level " " message)))

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

(defn- format-stack-trace
  "Converts an exception's stack trace to a formatted string."
  [exception]
  (let [sw (java.io.StringWriter.)
        pw (java.io.PrintWriter. sw)]
    (.printStackTrace exception pw)
    (.toString sw)))

(defn error
  "Logs an error message to stderr. If *request-id* is set, prepends the request ID to the message.
   Similar to log but writes to stderr instead of stdout.
   If the message is an Exception, prints the full stack trace.

   Example:
   (error \"Database connection failed\")  ;=> [request-id=abc123] ERROR Database connection failed (to stderr)
   (error (Exception. \"Something went wrong\"))  ;=> [request-id=abc123] ERROR Exception: Something went wrong + stack trace"
  [message]
  ; Write directly to stderr to ensure proper flushing
  (binding [*out* *err*]
    (if (instance? Exception message)
      (let [exception-msg (str "Caught exception " (.getClass message) ": " (.getMessage message))
            stack-trace (format-stack-trace message)]
        (println (format-message "ERROR" exception-msg))
        ; Print stack trace line by line to ensure each line is flushed separately
        (doseq [line (str/split-lines stack-trace)]
          (println (format-message "ERROR" line)))
        (flush))
      (do
        (println (format-message "ERROR" message))
        (flush)))))

(defn debug
  "Logs a debug message to stdout. If *request-id* is set, prepends the request ID to the message.

   Example:
   (debug \"Processing user input\")  ;=> DEBUG Processing user input
   (with-request-id \"abc123\"
     (debug \"Processing user input\"))  ;=> [request-id=abc123] DEBUG Processing user input"
  [message]
  (println (format-message "DEBUG" message)))

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
