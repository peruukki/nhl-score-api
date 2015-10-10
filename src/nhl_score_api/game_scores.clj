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

(defn- get-period-number
  "Parses the period number from the given DOM element, or nil if none found.
  Returns 4 for overtime period."
  [dom-element]
  (cond
    (is-regulation-period? dom-element)
    (read-string (first (:content dom-element)))

    (is-overtime-period? dom-element)
    4

    :default
    nil))

(defn- parse-time [time-str]
  (re-find #"\d\d:\d\d" time-str))

(defn- parse-goal-count [goal-count-str]
  (read-string (re-find #"\d+" goal-count-str)))

(defn- get-goal
  "Parses the goal information from the given DOM element, or nil if none found."
  [dom-element]
  (when (= "goalDetails" (:class (:attrs dom-element)))
    (let [content (:content dom-element)
          team (first (:content (first content)))
          time (parse-time (nth content 1))
          scorer (first (:content (nth content 2)))
          goal-count (parse-goal-count (first (:content (last content))))]
      {:team team :time time :scorer scorer :goal-count goal-count})))

(defn- add-period [goals period]
  (map #(assoc % :period period) goals))

(defn- parse-goals-rec
  "Parses given DOM elements and returns a collection of goals."
  [dom-elements current-period current-goals all-goals]
  (if (empty? dom-elements)
    (concat all-goals (add-period current-goals current-period))
    (let [dom-element (first dom-elements)
          period-number (get-period-number dom-element)
          goal (get-goal dom-element)]
      (cond
        period-number
        (parse-goals-rec (rest dom-elements) period-number [] (concat all-goals (add-period current-goals current-period)))

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

(defn- get-team-score [team goals]
  (count
    (filter #(= team (:team %)) goals)))

(defn- ended-in-overtime? [goals]
  (some #(> (:period %) 3) goals))

(defn- add-overtime-flag [goals scores]
  (if (ended-in-overtime? goals)
    (assoc scores :overtime true)
    scores))

(defn get-scores [teams goals]
  (->> teams
       (map #(vector % (get-team-score % goals)))
       (into {})
       (add-overtime-flag goals)))

(defn parse-game-details [dom-game]
  (let [goals (parse-goals dom-game)
        teams (parse-teams dom-game)
        scores (get-scores teams goals)]
    {:goals goals :scores scores :teams teams}))
