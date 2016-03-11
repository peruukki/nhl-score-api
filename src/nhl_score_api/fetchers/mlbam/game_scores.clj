(ns nhl-score-api.fetchers.mlbam.game-scores)

(defn- parse-goals [api-game]
  (:scoring-plays api-game))

(defn- ended-in-overtime? [goals]
  (some #(= "OT" (:ordinal-num (:about %))) goals))

(defn- ended-in-shootout? [goals]
  (some #(= "SO" (:ordinal-num (:about %))) goals))

(defn- add-overtime-flag [goals scores]
  (if (ended-in-overtime? goals)
    (assoc scores :overtime true)
    scores))

(defn- add-shootout-flag [goals scores]
  (if (ended-in-shootout? goals)
    (assoc scores :shootout true)
    scores))

(defn- add-score-flags [team-goal-counts goals]
  (->> team-goal-counts
       (add-overtime-flag goals)
       (add-shootout-flag goals)))

(defn- filter-team-goals [awayOrHomeKey goals team-details]
  (let [team-id (:id (awayOrHomeKey team-details))]
    (filter #(= team-id (:id (:team %))) goals)))

(defn- parse-scores [api-game team-details]
  (let [goals (:scoring-plays api-game)
        away-goals (filter-team-goals :away goals team-details)
        home-goals (filter-team-goals :home goals team-details)
        team-goal-counts {(:abbreviation (:away team-details)) (count away-goals)
                          (:abbreviation (:home team-details)) (count home-goals)}]
    (add-score-flags team-goal-counts goals)))

(defn- parse-team-details [awayOrHomeKey api-game]
  (select-keys (:team (awayOrHomeKey (:teams api-game))) [:abbreviation :id]))

(defn- parse-game-team-details [api-game]
  {:away (parse-team-details :away api-game)
   :home (parse-team-details :home api-game)})

(defn- get-team-abbreviations [team-details]
  {:away (:abbreviation (:away team-details))
   :home (:abbreviation (:home team-details))})

(defn- parse-game-details [api-game]
  (let [team-details (parse-game-team-details api-game)]
    {:goals (parse-goals api-game)
     :scores (parse-scores api-game team-details)
     :teams (get-team-abbreviations team-details)}))

(defn parse-game-scores [api-games]
  (map parse-game-details api-games))
