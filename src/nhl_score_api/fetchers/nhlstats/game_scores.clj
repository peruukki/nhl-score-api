(ns nhl-score-api.fetchers.nhlstats.game-scores
  (:require [nhl-score-api.fetchers.nhlstats.latest-games :refer [finished-game?]]
            [nhl-score-api.utils :refer [fmap]]
            [clojure.string :as str]))

(defn- regular-season-game? [api-game]
  (= "R" (:game-type api-game)))

(defn- all-star-game? [api-game]
  (= "A" (:game-type api-game)))

(defn- non-playoff-game? [api-game]
  (or
    (regular-season-game? api-game)
    (all-star-game? api-game)))

(defn- parse-goal-team [scoring-play team-details]
  (let [team-id (:id (:team scoring-play))
        team (first (filter #(= team-id (:id %)) (vals team-details)))]
    (:abbreviation team)))

(defn- parse-goal-period [api-game scoring-play]
  (let [period-number (str (:period (:about scoring-play)))]
    (if (non-playoff-game? api-game)
      (case (:period-type (:about scoring-play))
        "REGULAR" period-number
        "OVERTIME" "OT"
        "SHOOTOUT" "SO")
      period-number)))

(defn- parse-time-str
  ([time-str] (parse-time-str time-str nil))
  ([time-str default-value]
   (let [time (re-find #"(\d\d):(\d\d)" time-str)]
     {:min (if time (Integer/parseInt (nth time 1)) default-value)
      :sec (if time (Integer/parseInt (nth time 2)) default-value)})))

(defn- parse-goal-time [scoring-play period]
  (when (not= "SO" period)
    (parse-time-str (:period-time (:about scoring-play)))))

(defn- parse-goal-scorer-details [scoring-play]
  (first (filter #(= "Scorer" (:player-type %)) (:players scoring-play))))

(defn- parse-goal-assists-details [scoring-play]
  (filter #(= "Assist" (:player-type %)) (:players scoring-play)))

(defn- parse-player-name [player-details]
  (:full-name (:player player-details)))

(defn- parse-season-total [player-details period]
  (when (not= "SO" period)
    (:season-total player-details)))

(defn- scored-in-empty-net? [scoring-play]
  (:empty-net (:result scoring-play)))

(defn- parse-goal-strength [scoring-play]
  (:code (:strength (:result scoring-play))))

(defn- add-goal-time [goal-details time]
  (if time
    (assoc goal-details :min (:min time) :sec (:sec time))
    goal-details))

(defn- add-season-total [player-details season-total]
  (if season-total
    (assoc player-details :season-total season-total)
    player-details))

(defn- parse-goal-scorer [scoring-play period]
  (let [scorer-details (parse-goal-scorer-details scoring-play)
        player (parse-player-name scorer-details)
        season-total (parse-season-total scorer-details period)]
    (add-season-total {:player player} season-total)))

(defn- parse-goal-assist [assist-details]
  {:player (parse-player-name assist-details)
   :season-total (:season-total assist-details)})

(defn- parse-goal-assists [scoring-play]
  (let [assists-details (parse-goal-assists-details scoring-play)]
    (map parse-goal-assist assists-details)))

(defn- add-goal-assists [goal-details assists period]
  (if (not= "SO" period)
    (assoc goal-details :assists assists)
    goal-details))

(defn- add-empty-net-flag [goal-details empty-net?]
  (if empty-net?
    (assoc goal-details :empty-net true)
    goal-details))

(defn- add-goal-strength [goal-details strength]
  (if (contains? #{"PPG" "SHG"} strength)
    (assoc goal-details :strength strength)
    goal-details))

(defn- parse-goal-details [api-game scoring-play team-details]
  (let [team (parse-goal-team scoring-play team-details)
        period (parse-goal-period api-game scoring-play)
        time (parse-goal-time scoring-play period)
        scorer (parse-goal-scorer scoring-play period)
        assists (parse-goal-assists scoring-play)
        empty-net? (scored-in-empty-net? scoring-play)
        strength (parse-goal-strength scoring-play)]
    (-> {:team team :period period :scorer scorer}
        (add-goal-assists assists period)
        (add-goal-time time)
        (add-empty-net-flag empty-net?)
        (add-goal-strength strength))))

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
  (let [team-info (awayOrHomeKey (:teams api-game))
        record (select-keys (:league-record team-info) [:wins :losses :ot])
        details (select-keys (:team team-info) [:abbreviation :id :conference :division])]
    (assoc details :league-record record)))

(defn- parse-game-team-details [api-game]
  {:away (parse-team-details :away api-game)
   :home (parse-team-details :home api-game)})

(defn- get-team-abbreviations [team-details]
  {:away (:abbreviation (:away team-details))
   :home (:abbreviation (:home team-details))})

(defn- parse-game-state [api-game]
  (str/upper-case (:abstract-game-state (:status api-game))))

(defn- parse-linescore [api-game]
  (let [linescore (select-keys (:linescore api-game)
                               [:current-period :current-period-ordinal :current-period-time-remaining])
        current-period-time-remaining-pretty (:current-period-time-remaining linescore)
        remaining-time (parse-time-str current-period-time-remaining-pretty 0)
        current-period-time-remaining (assoc remaining-time :pretty current-period-time-remaining-pretty)]
    (assoc linescore :current-period-time-remaining current-period-time-remaining)))

(defn- parse-current-records [team-details]
  (let [away-details (:away team-details)
        home-details (:home team-details)]
    {(:abbreviation away-details) (:league-record away-details)
     (:abbreviation home-details) (:league-record home-details)}))

(defn- reduce-current-game-from-records [records teams scores]
  (let [away-team (:away teams)
        home-team (:home teams)
        away-goals (get scores away-team)
        home-goals (get scores home-team)
        winning-team (if (> away-goals home-goals) away-team home-team)
        losing-team (if (= winning-team home-team) away-team home-team)
        winning-team-current-record (get records winning-team)
        losing-team-current-record (get records losing-team)
        records-have-ot (contains? losing-team-current-record :ot)
        ot-loss (and
                  records-have-ot
                  (or (contains? scores :overtime) (contains? scores :shootout)))
        loss-key (if ot-loss :ot :losses)]
    {winning-team (assoc winning-team-current-record :wins (- (:wins winning-team-current-record) 1))
     losing-team (assoc losing-team-current-record loss-key (- (loss-key losing-team-current-record) 1))}))

(defn- parse-records [api-game team-details teams scores]
  (let [current-records (parse-current-records team-details)]
    (if (finished-game? api-game)
      (reduce-current-game-from-records current-records teams scores)
      current-records)))

(defn- parse-team-record-from-standings [standings division-id team-id]
  (let [division-standings (first (filter #(= (:id (:division %)) division-id) standings))]
    (first (filter #(= (:id (:team %)) team-id) (:team-records division-standings)))))

(defn- parse-streak-from-standings [standings division-id team-id]
  (let [team-record (parse-team-record-from-standings standings division-id team-id)
        streak (:streak team-record)]
    {:type (str/upper-case (:streak-type streak))
     :count (:streak-number streak)}))

(defn- parse-streaks [team-details standings]
  (let [away-details (:away team-details)
        home-details (:home team-details)]
    {(:abbreviation away-details)
     (parse-streak-from-standings standings (:id (:division away-details)) (:id away-details))
     (:abbreviation home-details)
     (parse-streak-from-standings standings (:id (:division home-details)) (:id home-details))}))

(defn- get-point-difference-to-playoff-spot [conference-id team-record last-playoff-teams first-teams-out-of-the-playoffs]
  (let [wild-card-rank-ordinal (read-string (:wild-card-rank team-record))
        comparison-team-record (if (> wild-card-rank-ordinal 2)
                                 (get last-playoff-teams conference-id)
                                 (get first-teams-out-of-the-playoffs conference-id))
        difference (- (:points team-record) (:points comparison-team-record))]
    (str (if (> difference 0) "+" "") difference)))

(defn- derive-standings [standings team-details last-playoff-teams first-teams-out-of-the-playoffs]
  (let [conference-id (:id (:conference team-details))
        division-id (:id (:division team-details))
        team-id (:id team-details)
        team-record (parse-team-record-from-standings standings division-id team-id)]
    {:points-from-playoff-spot
     (get-point-difference-to-playoff-spot conference-id team-record last-playoff-teams first-teams-out-of-the-playoffs) }))

(defn- parse-standings [team-details standings last-playoff-teams first-teams-out-of-the-playoffs]
  (let [away-details (:away team-details)
        home-details (:home team-details)]
    {(:abbreviation away-details)
     (derive-standings standings away-details last-playoff-teams first-teams-out-of-the-playoffs)
     (:abbreviation home-details)
     (derive-standings standings home-details last-playoff-teams first-teams-out-of-the-playoffs)}))

(defn- parse-current-playoff-series-wins [api-game teams]
  (let [away-team (:away teams)
        home-team (:home teams)
        series-summary-description (:series-status-short (:series-summary api-game))
        series-tied-match (re-find #"Tied (\d)-\d" series-summary-description)
        team-leads-match (re-find #"(\w+) (?:leads|wins) (\d)-(\d)" series-summary-description)]
    (cond
      series-tied-match
      (let [win-count (read-string (nth series-tied-match 1))]
        {away-team win-count
         home-team win-count})

      team-leads-match
      (let [leading-team (nth team-leads-match 1)
            leading-team-win-count (read-string (nth team-leads-match 2))
            trailing-team-win-count (read-string (nth team-leads-match 3))]
        {away-team (if (= away-team leading-team) leading-team-win-count trailing-team-win-count)
         home-team (if (= home-team leading-team) leading-team-win-count trailing-team-win-count)})

      :else
      {away-team 0
       home-team 0})))

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
        wins-before-game (if (finished-game? api-game)
                           (reduce-current-game-from-playoff-series-wins current-wins game-details)
                           current-wins)]
    {:wins wins-before-game}))

(defn- parse-game-status [api-game]
  (let [state (parse-game-state api-game)]
    (if (= state "LIVE")
      {:state state :progress (parse-linescore api-game)}
      {:state state})))

(defn- parse-game-start-time [api-game]
  (:game-date api-game))

(defn- add-team-records [game-details api-game team-details teams scores]
  (if (all-star-game? api-game)
    game-details
    (assoc game-details :records (parse-records api-game team-details teams scores))))

(defn- add-team-streaks [game-details api-game team-details standings]
  (if (not (regular-season-game? api-game))
    game-details
    (assoc game-details :streaks (parse-streaks team-details standings))))

(defn- add-team-standings [game-details api-game team-details standings last-playoff-teams first-teams-out-of-the-playoffs]
  (if (or (not (regular-season-game? api-game)) (nil? last-playoff-teams))
    game-details
    (assoc game-details :standings (parse-standings team-details standings last-playoff-teams first-teams-out-of-the-playoffs))))

(defn- add-playoff-series-information [game-details api-game]
  (if (non-playoff-game? api-game)
    game-details
    (assoc game-details :playoff-series (parse-playoff-series-information api-game game-details))))

(defn- parse-game-details [standings last-playoff-teams first-teams-out-of-the-playoffs api-game]
  (let [team-details (parse-game-team-details api-game)
        scores (parse-scores api-game team-details)
        teams (get-team-abbreviations team-details)]
    (-> {:status (parse-game-status api-game)
         :start-time (parse-game-start-time api-game)
         :goals (parse-goals api-game team-details)
         :scores scores
         :teams teams}
        (add-team-records api-game team-details teams scores)
        (add-team-streaks api-game team-details standings)
        (add-team-standings api-game team-details standings last-playoff-teams first-teams-out-of-the-playoffs)
        (add-playoff-series-information api-game))))

(defn- parse-wild-card-teams-by-conference-id [standings wild-card-rank]
  (let [records-by-conference-id
        (group-by #(:id (:conference %)) standings)
        team-records-by-conference-id
        (fmap (fn [records] (flatten (map #(:team-records %) records))) records-by-conference-id)
        wild-card-teams
        (fmap (fn [team-records] (first (filter #(= (:wild-card-rank %) wild-card-rank) team-records)))
              team-records-by-conference-id)]
    (if (some nil? (vals wild-card-teams)) nil wild-card-teams)))

(defn parse-game-scores [date-and-api-games standings]
  (let [last-playoff-teams (parse-wild-card-teams-by-conference-id standings "2")
        first-teams-out-of-the-playoffs (parse-wild-card-teams-by-conference-id standings "3")]
    {:date (:date date-and-api-games)
     :games (map (partial parse-game-details standings last-playoff-teams first-teams-out-of-the-playoffs)
                 (:games date-and-api-games))}))
