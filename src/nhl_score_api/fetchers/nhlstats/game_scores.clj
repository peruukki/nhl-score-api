(ns nhl-score-api.fetchers.nhlstats.game-scores
  (:require [nhl-score-api.fetchers.nhlstats.transformer :refer [finished-game? live-game?]]
            [clojure.string :as str]))

(def pre-game-stats-key :pre-game-stats)
(def current-stats-key :current-stats)

(defn- add-stats-field [game-details stats-key field value]
  (assoc game-details stats-key (assoc (stats-key game-details) field value)))

(defn- regular-season-game? [api-game]
  (= "R" (:game-type api-game)))

(defn- all-star-game? [api-game]
  (= "A" (:game-type api-game)))

(defn- non-playoff-game? [api-game]
  (or
    (regular-season-game? api-game)
    (all-star-game? api-game)))

(defn- playoff-game? [api-game]
  (not (non-playoff-game? api-game)))

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

(defn- game-finished? [game-details]
  (let [game-state (get-in game-details [:status :state])]
    (= "FINAL" game-state)))

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

(defn- parse-scores [api-game team-details]
  (let [goals (:scoring-plays api-game)
        away-goals (get-in api-game [:teams :away :score])
        home-goals (get-in api-game [:teams :home :score])
        team-goal-counts {(:abbreviation (:away team-details)) away-goals
                          (:abbreviation (:home team-details)) home-goals}]
    (add-score-flags team-goal-counts goals)))

(defn- parse-team-details [awayOrHomeKey api-game]
  (let [team-info (awayOrHomeKey (:teams api-game))
        record (select-keys (:league-record team-info) [:wins :losses :ot])
        details (select-keys (:team team-info) [:abbreviation :conference :division :id :location-name :short-name :team-name])]
    (assoc details :league-record record)))

(defn- parse-game-team-details [api-game]
  {:away (parse-team-details :away api-game)
   :home (parse-team-details :home api-game)})

(defn- get-teams [team-details]
  {:away (select-keys (:away team-details) [:abbreviation :id :location-name :short-name :team-name])
   :home (select-keys (:home team-details) [:abbreviation :id :location-name :short-name :team-name])})

(defn- parse-game-state [api-game]
  (let [status (:status api-game)
        abstract-state (str/upper-case (:abstract-game-state status))
        detailed-state (str/upper-case (:detailed-state status))]
    (if (= "POSTPONED" detailed-state) detailed-state abstract-state)))

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

(defn- reduce-current-game-from-records [records api-game teams scores]
  (let [away-team (:abbreviation (:away teams))
        home-team (:abbreviation (:home teams))
        away-goals (get scores away-team)
        home-goals (get scores home-team)
        winning-team (if (> away-goals home-goals) away-team home-team)
        losing-team (if (= winning-team home-team) away-team home-team)
        winning-team-current-record (get records winning-team)
        losing-team-current-record (get records losing-team)
        records-have-ot (contains? losing-team-current-record :ot)
        ot-loss (and
                  records-have-ot
                  (non-playoff-game? api-game)
                  (or (contains? scores :overtime) (contains? scores :shootout)))
        loss-key (if ot-loss :ot :losses)]
    {winning-team (assoc winning-team-current-record :wins (- (:wins winning-team-current-record) 1))
     losing-team (assoc losing-team-current-record loss-key (- (loss-key losing-team-current-record) 1))}))

(defn- parse-records [api-game team-details teams scores]
  (let [current-records (parse-current-records team-details)]
    {current-stats-key
     current-records
     pre-game-stats-key
     (if (finished-game? api-game)
       (reduce-current-game-from-records current-records api-game teams scores)
       current-records)}
    ))

(defn- parse-team-record-from-standings [standings division-id team-id]
  (let [division-standings (first (filter #(= (:id (:division %)) division-id) standings))]
    (first (filter #(= (:id (:team %)) team-id) (:team-records division-standings)))))

(defn- parse-streak-from-standings [standings division-id team-id]
  (let [team-record (parse-team-record-from-standings standings division-id team-id)
        streak (:streak team-record)]
    (if (nil? streak)
      nil
      {:type (str/upper-case (:streak-type streak))
       :count (:streak-number streak)})))

(defn- parse-streaks [team-details standings]
  (let [away-details (:away team-details)
        home-details (:home team-details)]
    {(:abbreviation away-details)
     (parse-streak-from-standings standings (:id (:division away-details)) (:id away-details))
     (:abbreviation home-details)
     (parse-streak-from-standings standings (:id (:division home-details)) (:id home-details))}))

(defn- assoc-team-records-with-division-id [division-records]
  (let [team-records (:team-records division-records)
        division-id (:id (:division division-records))]
    (map #(assoc % :division-id division-id) team-records)))

(defn- parse-wild-card-teams [conference-id standings]
  (let [records-matching-conference-id (filter #(= (:id (:conference %)) conference-id) standings)
        team-records (flatten (map assoc-team-records-with-division-id records-matching-conference-id))
        wild-card-teams (filter #(not= (:wild-card-rank %) "0") team-records)]
    (sort-by #(read-string (:wild-card-rank %)) wild-card-teams)))

(defn- get-last-team-record-in-direct-playoff-spot [division-id standings]
  (let [records-matching-division-id (:team-records (first (filter #(= (:id (:division %)) division-id) standings)))
        last-team-in-direct-playoff-spot (first (filter #(= (:division-rank %) "3") records-matching-division-id))]
    last-team-in-direct-playoff-spot))

(defn- get-comparison-team-points-for-team-out-of-playoff-spot [division-id
                                                                standings
                                                                wild-card-teams
                                                                no-wild-card-playoff-teams-in-division]
  (let [points-for-last-wild-card-team-in-playoffs (:points (second wild-card-teams))
        points-for-last-direct-playoff-team-in-division (:points (get-last-team-record-in-direct-playoff-spot division-id standings))]
    (if (and
          no-wild-card-playoff-teams-in-division
          (< points-for-last-direct-playoff-team-in-division points-for-last-wild-card-team-in-playoffs))
      points-for-last-direct-playoff-team-in-division
      points-for-last-wild-card-team-in-playoffs)))

(defn- get-comparison-team-points-for-team-in-playoff-spot [division-id
                                                            wild-card-teams
                                                            no-wild-card-playoff-teams-in-division]
  (let [points-for-first-team-out-of-playoffs (:points (nth wild-card-teams 2))
        points-for-first-team-out-of-playoffs-in-division (:points (first (filter #(= (:division-id %) division-id) wild-card-teams)))]
    (if no-wild-card-playoff-teams-in-division
      points-for-first-team-out-of-playoffs-in-division
      points-for-first-team-out-of-playoffs)))

; TODO: Use this again when "normal" playoff spot logic is resumed
(defn- get-point-difference-to-playoff-spot [conference-id division-id team-record standings]
  (let [wild-card-teams (parse-wild-card-teams conference-id standings)]
    (if (empty? wild-card-teams)
      nil
      (let [is-team-out-of-playoffs (> (read-string (:wild-card-rank team-record)) 2)
            no-wild-card-playoff-teams-in-division (not (some #(= (:division-id %) division-id)
                                                              (take 2 wild-card-teams)))
            comparison-team-points
            (if is-team-out-of-playoffs
              (get-comparison-team-points-for-team-out-of-playoff-spot division-id
                                                                       standings
                                                                       wild-card-teams
                                                                       no-wild-card-playoff-teams-in-division)
              (get-comparison-team-points-for-team-in-playoff-spot division-id
                                                                   wild-card-teams
                                                                   no-wild-card-playoff-teams-in-division))
            difference (- (:points team-record) comparison-team-points)]
        (str (if (> difference 0) "+" "") difference)))))

(defn- derive-standings [standings team-details]
  (let [division-id (:id (:division team-details))
        team-id (:id team-details)
        team-record (parse-team-record-from-standings standings division-id team-id)]
    {:division-rank (:pp-division-rank team-record)
     :league-rank (:pp-league-rank team-record)}))

(defn- parse-standings [team-details standings]
  (let [away-details (:away team-details)
        home-details (:home team-details)]
    {(:abbreviation away-details)
     (derive-standings standings away-details)
     (:abbreviation home-details)
     (derive-standings standings home-details)}))

(defn- parse-current-playoff-series-wins [api-game teams]
  (let [away-team (:away teams)
        home-team (:home teams)
        matchup-teams (get-in api-game [:series-summary :series :matchup-teams])
        matchup-team-away (first (filter #(= (:id (:team %)) (:id away-team)) matchup-teams))
        matchup-team-home (first (filter #(= (:id (:team %)) (:id home-team)) matchup-teams))
        away-team-wins (get-in matchup-team-away [:series-record :wins])
        home-team-wins (get-in matchup-team-home [:series-record :wins])]
    {(:abbreviation away-team) away-team-wins
     (:abbreviation home-team) home-team-wins}))

(defn- parse-playoff-round [api-game]
  (get-in api-game [:series-summary :series :round :number]))

(defn- get-winning-team [game-details]
  (let [away-team (:abbreviation (:away (:teams game-details)))
        home-team (:abbreviation (:home (:teams game-details)))
        away-goals (get (:scores game-details) away-team)
        home-goals (get (:scores game-details) home-team)]
    (if (> away-goals home-goals) away-team home-team)))

(defn- add-or-reduce-current-game-to-or-from-playoff-series-wins [current-wins game-details change-fn]
  (let [away-team (:abbreviation (:away (:teams game-details)))
        home-team (:abbreviation (:home (:teams game-details)))
        away-wins (get current-wins away-team)
        home-wins (get current-wins home-team)
        winning-team (get-winning-team game-details)]
    {away-team (if (= winning-team away-team) (change-fn away-wins 1) away-wins)
     home-team (if (= winning-team home-team) (change-fn home-wins 1) home-wins)}))

(defn- add-current-game-to-playoff-series-wins [current-wins game-details]
  (add-or-reduce-current-game-to-or-from-playoff-series-wins current-wins game-details +))

(defn- reduce-current-game-from-playoff-series-wins [current-wins game-details]
  (add-or-reduce-current-game-to-or-from-playoff-series-wins current-wins game-details -))

(defn- parse-playoff-series-information [api-game game-details]
  (let [teams (:teams game-details)
        parsed-current-wins (parse-current-playoff-series-wins api-game teams)
        win-count (apply + (vals parsed-current-wins))
        game-number (get-in api-game [:series-summary :game-number])
        current-wins (cond (and (< win-count game-number) (finished-game? api-game))
                           (add-current-game-to-playoff-series-wins parsed-current-wins game-details)

                           (and (= win-count game-number) (live-game? api-game))
                           (reduce-current-game-from-playoff-series-wins parsed-current-wins game-details)

                           :else
                           parsed-current-wins)
        wins-before-game (if (finished-game? api-game)
                           (reduce-current-game-from-playoff-series-wins current-wins game-details)
                           current-wins)
        round (parse-playoff-round api-game)]
    {current-stats-key {:round round :wins current-wins}
     pre-game-stats-key {:round round :wins wins-before-game}}))

(defn- parse-game-status [api-game]
  (let [state (parse-game-state api-game)]
    (if (= state "LIVE")
      {:state state :progress (parse-linescore api-game)}
      {:state state})))

(defn- parse-game-start-time [api-game]
  (:game-date api-game))

(defn- add-team-records [game-details api-game team-details teams scores include-pre-game-stats?]
  (if (all-star-game? api-game)
    game-details
    (let [records (parse-records api-game team-details teams scores)
          with-pre-game-stats (if include-pre-game-stats?
                                (add-stats-field game-details pre-game-stats-key :records (pre-game-stats-key records))
                                game-details)]
      (add-stats-field with-pre-game-stats current-stats-key :records (current-stats-key records)))))

(defn- add-team-streaks [game-details api-game team-details standings]
  (if (not (regular-season-game? api-game))
    game-details
    (add-stats-field game-details current-stats-key :streaks (parse-streaks team-details standings))))

(defn- add-team-standings [game-details api-game team-details standings include-pre-game-stats?]
  (if (all-star-game? api-game)
    game-details
    (let [parsed-standings (parse-standings team-details standings)]
      (cond-> game-details
              (and include-pre-game-stats? (playoff-game? api-game))
              (add-stats-field pre-game-stats-key :standings parsed-standings)
              true
              (add-stats-field current-stats-key :standings parsed-standings)))))

(defn- add-playoff-series-information [game-details api-game include-pre-game-stats?]
  (if (non-playoff-game? api-game)
    game-details
    (let [playoff-series-information
          (parse-playoff-series-information api-game game-details)
          with-pre-game-stats
          (if include-pre-game-stats?
            (add-stats-field game-details pre-game-stats-key :playoff-series (pre-game-stats-key playoff-series-information))
            game-details)]
      (add-stats-field with-pre-game-stats current-stats-key :playoff-series (current-stats-key playoff-series-information)))))

(defn- get-score-affecting-goal-count [game-details]
  (let [goals (:goals game-details)
        non-so-goal-count (count (filter #(not= "SO" (:period %)) goals))
        so-goal-count (count (filter #(= "SO" (:period %)) goals))
        affecting-so-goal-count (if (and (> so-goal-count 0) (game-finished? game-details)) 1 0)]
    (+ non-so-goal-count affecting-so-goal-count)))

(defn- validate-score-and-goal-counts [game-details]
  (let [away-team (get-in game-details [:teams :away :abbreviation])
        home-team (get-in game-details [:teams :home :abbreviation])
        away-score (get-in game-details [:scores away-team])
        home-score (get-in game-details [:scores home-team])
        score-count (+ away-score home-score)
        goal-count (get-score-affecting-goal-count game-details)]
    (cond
      (and (= goal-count 0) (> score-count 0))
      {:error :MISSING-ALL-GOALS}

      (not= score-count goal-count)
      {:error :SCORE-AND-GOAL-COUNT-MISMATCH :details {:score-count score-count :goal-count goal-count}}

      :else
      nil)))

(defn- add-validation-errors [game-details]
  (let [errors (keep identity [(validate-score-and-goal-counts game-details)])]
    (if (empty? errors)
      game-details
      (assoc game-details :errors errors))))

(defn- reject-empty-vals [game-details]
  (into {} (filter (comp not empty? val) game-details)))

(defn- parse-game-details [standings include-pre-game-stats? api-game]
  (let [team-details (parse-game-team-details api-game)
        scores (parse-scores api-game team-details)
        teams (get-teams team-details)]
    (-> {:status (parse-game-status api-game)
         :start-time (parse-game-start-time api-game)
         :goals (parse-goals api-game team-details)
         :scores scores
         :teams teams
         pre-game-stats-key {}
         current-stats-key {}}
        (add-team-records api-game team-details teams scores include-pre-game-stats?)
        (add-team-streaks api-game team-details standings)
        (add-team-standings api-game team-details standings include-pre-game-stats?)
        (add-playoff-series-information api-game include-pre-game-stats?)
        (add-validation-errors)
        (reject-empty-vals))))

(defn parse-game-scores
  ([date-and-api-games standings]
   (parse-game-scores date-and-api-games standings true))
  ([date-and-api-games standings include-pre-game-stats?]
   {:date (:date date-and-api-games)
    :games (map (partial parse-game-details standings include-pre-game-stats?)
                (:games date-and-api-games))}))
