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

(defn- parse-current-playoff-series-wins [api-game teams]
  (let [away-team (:away teams)
        home-team (:home teams)
        series-summary-description (:series-status-short (:series-summary api-game))
        series-tied-match (re-find #"Tied (\d)-\d" series-summary-description)]
    (if series-tied-match
      (let [win-count (read-string (nth series-tied-match 1))]
        {away-team win-count
         home-team win-count})
      (let [team-leads-match (re-find #"(\w+) leads (\d)-(\d)" series-summary-description)
            leading-team (nth team-leads-match 1)
            leading-team-win-count (read-string (nth team-leads-match 2))
            trailing-team-win-count (read-string (nth team-leads-match 3))]
        {away-team (if (= away-team leading-team) leading-team-win-count trailing-team-win-count)
         home-team (if (= home-team leading-team) leading-team-win-count trailing-team-win-count)}))))

(defn- get-winning-team [game-details]
  (let [away-team (:away (:teams game-details))
        home-team (:home (:teams game-details))
        away-goals (get (:scores game-details) away-team)
        home-goals (get (:scores game-details) home-team)]
    (if (> away-goals home-goals) away-team home-team)))

(defn- reduce-current-game-from-playoff-series-wins [current-wins game-details]
  (let [away-team (:away (:teams game-details))
        home-team (:home (:teams game-details))
        away-wins (get current-wins away-team)
        home-wins (get current-wins home-team)
        winning-team (get-winning-team game-details)]
    {away-team (if (= winning-team away-team) (- away-wins 1) away-wins)
     home-team (if (= winning-team home-team) (- home-wins 1) home-wins)}))

(defn- parse-playoff-series-information [api-game game-details]
  (let [teams (:teams game-details)
        current-wins (parse-current-playoff-series-wins api-game teams)
        wins-before-game (reduce-current-game-from-playoff-series-wins current-wins game-details)]
    {:wins wins-before-game}))

(defn- add-playoff-series-information [api-game game-details]
  (if (regular-season-game? api-game)
    game-details
    (assoc game-details :playoff-series (parse-playoff-series-information api-game game-details))))

(defn- parse-game-details [api-game]
  (let [team-details (parse-game-team-details api-game)
        game-details {:goals  (parse-goals api-game team-details)
                      :scores (parse-scores api-game team-details)
                      :teams  (get-team-abbreviations team-details)}]
    (add-playoff-series-information api-game game-details)))

(defn parse-game-scores [api-games]
  (map parse-game-details api-games))
