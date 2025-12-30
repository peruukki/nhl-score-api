(ns nhl-score-api.param-parser
  (:require [camel-snake-kebab.core :refer [->camelCaseString]]
            [clojure.string :as str]
            [nhl-score-api.utils :refer [parse-date]]))

(defn- parse-fn-date [name field value]
  (try
    {:success {field (parse-date value)}}
    (catch Exception e {:error (str "Invalid parameter " name ": " (.getMessage e))})))

(defn- parse-fn-string [name field value]
  {:success {field value}})

(def parse-fns {:date parse-fn-date
                :string parse-fn-string})

(defn- parse-param [field value type required?]
  (let [name (->camelCaseString field)
        parse-fn (type parse-fns)]
    (cond (and required? (nil? value))
          {:error (str "Missing required parameter " name)}
          (nil? value)
          {:success {field value}}
          :else
          (parse-fn name field value))))

(defn parse-params [params-to-parse request-params]
  (let [parse-results
        (map #(parse-param (:field %) ((:field %) request-params) (:type %) (:required? %))
             params-to-parse)
        parsed-values (filter some? (map #(:success %) parse-results))
        parsing-errors (filter some? (map #(:error %) parse-results))]
    {:values (apply merge parsed-values)
     :errors parsing-errors}))

(defn parse-include-param [include-value]
  "Parses the 'include' query parameter, splitting by comma and trimming whitespace.
   Returns a set of inclusion names (e.g., #{\"rosters\" \"otherThing\"}).
   Returns empty set if parameter is nil or empty (including whitespace-only strings)."
  (if (or (nil? include-value) (empty? (str/trim include-value)))
    #{}
    (->> (str/split include-value #",")
         (map str/trim)
         (filter (complement empty?))
         set)))
