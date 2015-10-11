(ns nhl-score-api.game-scores
  (:require [net.cgrand.enlive-html :as html]))

(declare parse-dom parse-games parse-game-details)

(defn parse-game-scores [dom]
  (filter #(seq (:goals %))
    (map parse-game-details (parse-games dom))))

(defn parse-dom [html-resource]
  (html/html-resource html-resource))

(defn parse-games [dom-page]
  (html/select dom-page [:.sbGame]))

(defn- is-regulation-period? [dom-element]
  (and (= :b (:tag dom-element))
       (= " Period:" (last (:content dom-element)))))

(defn- is-overtime-period? [dom-element]
  (and (= :b (:tag dom-element))
       (= "OT Period:" (first (:content dom-element)))))

(defn- is-shootout? [dom-element]
  (and (= :b (:tag dom-element))
       (= "SO:" (first (:content dom-element)))))

(defn- get-period
  "Parses the period string from the given DOM element, or nil if none found.
  Returns \"OT\" for (5 minute) overtime period and \"SO\" for shootout."
  [dom-element]
  (cond
    (is-regulation-period? dom-element)
    (first (:content dom-element))

    (is-overtime-period? dom-element)
    "OT"

    (is-shootout? dom-element)
    "SO"

    :default
    nil))

(defn- parse-time [time-str]
  (re-find #"\d\d:\d\d" time-str))

(defn- parse-goal-count [goal-count-str]
  (read-string (re-find #"\d+" goal-count-str)))

(defn- get-goal
  "Parses the goal information from the given DOM element, or nil if none found."
  [dom-element]
  (case (:class (:attrs dom-element))
    "goalDetails"
    (let [content (:content dom-element)
          team (first (:content (first content)))
          time (parse-time (nth content 1))
          scorer (first (:content (nth content 2)))
          goal-count (parse-goal-count (first (:content (last content))))]
      {:team team :time time :scorer scorer :goal-count goal-count})

    "shootoutInfo"
    (let [content (:content dom-element)
          team (first (:content (first content)))
          scorer (first (:content (nth content 2)))]
      {:team team :scorer scorer})

    nil))

(defn- add-period [goals period]
  (map #(assoc % :period period) goals))

(defn- parse-goals-rec
  "Parses given DOM elements and returns a collection of goals."
  [dom-elements current-period current-goals all-goals]
  (if (empty? dom-elements)
    (concat all-goals (add-period current-goals current-period))
    (let [dom-element (first dom-elements)
          period (get-period dom-element)
          goal (get-goal dom-element)]
      (cond
        period
        (parse-goals-rec (rest dom-elements) period [] (concat all-goals (add-period current-goals current-period)))

        goal
        (parse-goals-rec (rest dom-elements) current-period (conj current-goals goal) all-goals)

        :default
        (parse-goals-rec (rest dom-elements) current-period current-goals all-goals)))))

(defn parse-goals [dom-game]
  (let [scoring-infos (html/select dom-game [:.scoringInfo])
        goal-infos (:content (first scoring-infos))]
    (parse-goals-rec goal-infos nil [] [])))

(defn parse-teams [dom-game]
  (let [team-links (html/select dom-game [:table :a])
        team-links-with-content (remove #(nil? (:content %)) team-links)]
    (map #(:rel (:attrs %)) team-links-with-content)))

(defn- parse-goal-counts [dom-game teams]
  (let [team-scores (html/select dom-game [:table :td.total])
        goal-counts (map #(read-string (first (:content %))) team-scores)]
    (into {}
      (map #(vector %1 (nth goal-counts %2)) teams (iterate inc 0)))))

(defn- ended-in-overtime? [goals]
  (some #(= "OT" (:period %)) goals))

(defn- ended-in-shootout? [goals]
  (some #(= "SO" (:period %)) goals))

(defn- add-overtime-flag [goals scores]
  (if (ended-in-overtime? goals)
    (assoc scores :overtime true)
    scores))

(defn- add-shootout-flag [goals scores]
  (if (ended-in-shootout? goals)
    (assoc scores :shootout true)
    scores))

(defn add-score-flags [team-goal-counts goals]
  (->> team-goal-counts
       (add-overtime-flag goals)
       (add-shootout-flag goals)))

(defn parse-game-details [dom-game]
  (let [goals (parse-goals dom-game)
        teams (parse-teams dom-game)
        team-goal-counts (parse-goal-counts dom-game teams)
        scores (add-score-flags team-goal-counts goals)]
    {:goals goals :scores scores :teams teams}))
