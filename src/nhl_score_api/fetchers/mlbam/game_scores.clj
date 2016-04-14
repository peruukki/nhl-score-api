(ns nhl-score-api.fetchers.mlbam.game-scores)

(defn- regular-season-game? [api-game]
  (= "R" (:game-type api-game)))

(defn- parse-goal-team [scoring-play team-details]
  (let [team-id (:id (:team scoring-play))
        team (first (filter #(= team-id (:id %)) (vals team-details)))]
    (:abbreviation team)))

(defn- parse-goal-period [api-game scoring-play]
  (let [period-number (str (:period (:about scoring-play)))]
    (if (regular-season-game? api-game)
      (case (:period-type (:about scoring-play))
        "REGULAR" period-number
        "OVERTIME" "OT"
        "SHOOTOUT" "SO")
      period-number)))

(defn- parse-time-str [time-str]
  (let [time (re-find #"(\d\d):(\d\d)" time-str)]
    {:min (Integer/parseInt (nth time 1))
     :sec (Integer/parseInt (nth time 2))}))

(defn- parse-goal-time [scoring-play period]
  (when (not= "SO" period)
    (parse-time-str (:period-time (:about scoring-play)))))

(defn- parse-goal-scorer-details [scoring-play]
  (first (filter #(= "Scorer" (:player-type %)) (:players scoring-play))))

(defn- parse-goal-scorer-name [scoring-play]
  (:full-name (:player (parse-goal-scorer-details scoring-play))))

(defn- parse-goal-scorer-goal-count [scoring-play period]
  (when (not= "SO" period)
    (:season-total (parse-goal-scorer-details scoring-play))))

(defn- add-goal-time [goal-details time]
  (if time
    (assoc goal-details :min (:min time) :sec (:sec time))
    goal-details))

(defn- add-goal-count [goal-details goal-count]
  (if goal-count
    (assoc goal-details :goal-count goal-count)
    goal-details))

(defn- parse-goal-details [api-game scoring-play team-details]
  (let [team (parse-goal-team scoring-play team-details)
        period (parse-goal-period api-game scoring-play)
        time (parse-goal-time scoring-play period)
        scorer (parse-goal-scorer-name scoring-play)
        goal-count (parse-goal-scorer-goal-count scoring-play period)]
    (-> {:team team :period period :scorer scorer}
        (add-goal-time time)
        (add-goal-count goal-count))))

(defn- parse-goals [api-game team-details]
  (map #(parse-goal-details api-game % team-details) (:scoring-plays api-game)))

(defn- ended-in-overtime? [api-game goals]
  (and
    (regular-season-game? api-game)
    (some #(= "OT" (:ordinal-num (:about %))) goals)))

(defn- ended-in-shootout? [goals]
  (some #(= "SO" (:ordinal-num (:about %))) goals))

(defn- add-overtime-flag [api-game goals scores]
  (if (ended-in-overtime? api-game goals)
    (assoc scores :overtime true)
    scores))

(defn- add-shootout-flag [goals scores]
  (if (ended-in-shootout? goals)
    (assoc scores :shootout true)
    scores))

(defn- add-score-flags [api-game team-goal-counts goals]
  (->> team-goal-counts
       (add-overtime-flag api-game goals)
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
    (add-score-flags api-game team-goal-counts goals)))

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
    {:goals (parse-goals api-game team-details)
     :scores (parse-scores api-game team-details)
     :teams (get-team-abbreviations team-details)}))

(defn parse-game-scores [api-games]
  (map parse-game-details api-games))
