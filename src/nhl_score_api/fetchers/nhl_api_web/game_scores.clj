(ns nhl-score-api.fetchers.nhl-api-web.game-scores
  (:require [clojure.string :as str]
            [nhl-score-api.fetchers.nhl-api-web.data :as data]
            [nhl-score-api.fetchers.nhl-api-web.transformer :refer [finished-game?
                                                                    get-game-state
                                                                    live-game?
                                                                    non-playoff-game?
                                                                    playoff-game?
                                                                    regular-season-game?]])
  (:import (java.text DecimalFormat DecimalFormatSymbols)
           (java.util Locale)))

(def pre-game-stats-key :pre-game-stats)
(def current-stats-key :current-stats)

(defn- add-stats-field [game-details stats-key field value]
  (assoc game-details stats-key (assoc (stats-key game-details) field value)))

(defn- parse-time-str
  ([time-str] (parse-time-str time-str nil))
  ([time-str default-value]
   (let [time (re-find #"(\d\d):(\d\d)" time-str)]
     {:min (if time (Integer/parseInt (nth time 1)) default-value)
      :sec (if time (Integer/parseInt (nth time 2)) default-value)})))

(defn- parse-goal-time [goal-details period]
  (when (not= "SO" period)
    (parse-time-str (:time-in-period goal-details))))

(defn- parse-goal-scorer-details [goal-details]
  (select-keys goal-details [:first-name :goals-to-date :last-name :player-id]))

(defn- parse-player-id [player-details]
  (:player-id player-details))

(defn- parse-player-name [player-details]
  (str (:default (:first-name player-details)) " " (:default (:last-name player-details))))

(defn- parse-goal-scorer-season-total [player-details period]
  (when (not= "SO" period)
    (:goals-to-date player-details)))

(defn- scored-in-empty-net? [goal-details]
  (contains? #{"awarded-empty-net" "empty-net" "own-goal-empty-net"} (:goal-modifier goal-details)))

(defn- parse-goal-strength [goal-details]
  (case (:strength goal-details)
    "pp" "PPG"
    "sh" "SHG"
    "EVEN"))

(defn- add-goal-time [goal-details time]
  (if time
    (assoc goal-details :min (:min time) :sec (:sec time))
    goal-details))

(defn- add-season-total [player-details season-total]
  (if season-total
    (assoc player-details :season-total season-total)
    player-details))

(defn- parse-goal-scorer [goal-details period]
  (let [scorer-details (parse-goal-scorer-details goal-details)]
    (add-season-total {:player (parse-player-name scorer-details)
                       :player-id (parse-player-id scorer-details)}
                      (parse-goal-scorer-season-total scorer-details period))))

(defn- parse-goal-assist [assist-details]
  {:player (parse-player-name assist-details)
   :player-id (parse-player-id assist-details)
   :season-total (:assists-to-date assist-details)})

(defn- parse-goal-assists [goal-details]
  (let [assists-details (:assists goal-details)]
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

(defn- parse-goal-details [goal-details]
  (let [team (:default (:team-abbrev goal-details))
        period (:period goal-details)
        time (parse-goal-time goal-details period)
        scorer (parse-goal-scorer goal-details period)
        assists (parse-goal-assists goal-details)
        empty-net? (scored-in-empty-net? goal-details)
        strength (parse-goal-strength goal-details)]
    (-> {:team team :period period :scorer scorer}
        (add-goal-assists assists period)
        (add-goal-time time)
        (add-empty-net-flag empty-net?)
        (add-goal-strength strength))))

(defn- parse-period [period-descriptor only-numeric-periods?]
  (let [period-type (:period-type period-descriptor)]
    (if (or only-numeric-periods? (= period-type "REG"))
      (:number period-descriptor)
      period-type)))

(defn- parse-period-ordinal [period-descriptor only-numeric-periods?]
  (let [period (parse-period period-descriptor only-numeric-periods?)]
    (if (not (number? period))
      period
      (case period
        1 "1st"
        2 "2nd"
        3 "3rd"
        4 "OT"
        (str (- period 3) "OT")))))

(defn- parse-period-goals [period-scoring only-numeric-periods?]
  (let [period (str (parse-period (:period-descriptor period-scoring) only-numeric-periods?))]
    (map #(assoc % :period period) (:goals period-scoring))))

(defn- parse-goals [gamecenter]
  (let [period-scorings (:scoring (:summary gamecenter))
        only-numeric-periods? (playoff-game? gamecenter)
        period-goals (flatten (map #(parse-period-goals % only-numeric-periods?) period-scorings))]
    (map parse-goal-details period-goals)))

(defn- game-finished? [game-details]
  (let [game-state (get-in game-details [:status :state])]
    (= "FINAL" game-state)))

(defn- parse-scores [schedule-game team-details]
  (let [team-goal-counts {(:abbreviation (:away team-details)) (:score (:away team-details))
                          (:abbreviation (:home team-details)) (:score (:home team-details))}]
    (case (:last-period-type (:game-outcome schedule-game))
      "OT" (assoc team-goal-counts :overtime true)
      "SO" (assoc team-goal-counts :shootout true)
      team-goal-counts)))

(defn- parse-team-details [team-info]
  {:abbreviation (:abbrev team-info)
   :id (:id team-info)
   :location-name (:default (:place-name team-info))
   :score (:score team-info 0)})

(defn- parse-game-team-details [schedule-game]
  {:away (parse-team-details (:away-team schedule-game))
   :home (parse-team-details (:home-team schedule-game))})

(defn- get-teams [team-details season]
  {:away (merge
          (select-keys (:away team-details) [:abbreviation :id :location-name])
          (data/get-team-names (:abbreviation (:away team-details)) season))
   :home (merge
          (select-keys (:home team-details) [:abbreviation :id :location-name])
          (data/get-team-names (:abbreviation (:home team-details)) season))})

(defn- parse-game-state [schedule-game]
  (str/upper-case (get-game-state schedule-game)))

(defn- parse-game-progress [schedule-game gamecenter]
  (if (nil? gamecenter)
    nil
    (let [clock (:clock gamecenter)
          time-remaining-pretty (if (or (:in-intermission clock)
                                        (= (:seconds-remaining clock) 0))
                                  "END"
                                  (get clock :time-remaining "20:00"))
          time-remaining-structured (parse-time-str time-remaining-pretty 0)
          period-descriptor {:number (get-in schedule-game [:period-descriptor :number] 1)
                             :period-type (get-in schedule-game [:period-descriptor :period-type] "REG")}]
      {:current-period (:number period-descriptor)
       :current-period-ordinal (parse-period-ordinal period-descriptor (playoff-game? schedule-game))
       :current-period-time-remaining (assoc time-remaining-structured :pretty time-remaining-pretty)})))

(defn- parse-team-record-from-standings [standings team-abbreviation]
  (or (some #(when (= (:default (:team-abbrev %)) team-abbreviation) %) standings)
      {:wins 0 :losses 0 :ot-losses 0}))

(defn- parse-current-records [standings teams]
  (let [away-team (:abbreviation (:away teams))
        home-team (:abbreviation (:home teams))
        away-details (parse-team-record-from-standings standings away-team)
        home-details (parse-team-record-from-standings standings home-team)]
    {away-team (assoc (select-keys away-details [:wins :losses])
                      :ot (:ot-losses away-details))
     home-team (assoc (select-keys home-details [:wins :losses])
                      :ot (:ot-losses home-details))}))

(defn- parse-records [current-and-pre-game-standings teams]
  {current-stats-key (parse-current-records (:current current-and-pre-game-standings) teams)
   pre-game-stats-key (parse-current-records (:pre-game current-and-pre-game-standings) teams)})

(defn- parse-streak-from-standings [standings team-abbreviation]
  (let [team-record (parse-team-record-from-standings standings team-abbreviation)
        streak-count (:streak-count team-record 0)]
    (if (= streak-count 0)
      nil
      {:type (case (:streak-code team-record)
               "W" "WINS"
               "L" "LOSSES"
               "OT")
       :count streak-count})))

(defn- parse-streaks [team-details standings]
  (let [away-team (:abbreviation (:away team-details))
        home-team (:abbreviation (:home team-details))]
    {away-team (parse-streak-from-standings standings away-team)
     home-team (parse-streak-from-standings standings home-team)}))

(defn- parse-wild-card-teams [conference-name standings]
  (let [conference-records (filter #(= (:conference-name %) conference-name) standings)
        wild-card-teams (filter #(not= (:wildcard-sequence %) 0) conference-records)]
    (sort-by :wildcard-sequence wild-card-teams)))

(defn- get-last-team-record-in-direct-playoff-spot [division-name standings]
  (let [records-matching-division-name (filter #(= (:division-name %) division-name) standings)
        last-team-in-direct-playoff-spot (first (filter #(= (:division-sequence %) 3) records-matching-division-name))]
    last-team-in-direct-playoff-spot))

(defn- get-comparison-team-points-for-team-out-of-playoff-spot [division-name
                                                                standings
                                                                wild-card-teams
                                                                no-wild-card-playoff-teams-in-division]
  (let [points-for-last-wild-card-team-in-playoffs (:points (second wild-card-teams))
        points-for-last-direct-playoff-team-in-division (:points (get-last-team-record-in-direct-playoff-spot division-name standings))]
    (if (and
         no-wild-card-playoff-teams-in-division
         (< points-for-last-direct-playoff-team-in-division points-for-last-wild-card-team-in-playoffs))
      points-for-last-direct-playoff-team-in-division
      points-for-last-wild-card-team-in-playoffs)))

(defn- get-comparison-team-points-for-team-in-playoff-spot [division-name
                                                            wild-card-teams
                                                            no-wild-card-playoff-teams-in-division]
  (let [points-for-first-team-out-of-playoffs (:points (nth wild-card-teams 2))
        points-for-first-team-out-of-playoffs-in-division (:points (first (filter #(= (:division-name %) division-name) wild-card-teams)))]
    (if no-wild-card-playoff-teams-in-division
      points-for-first-team-out-of-playoffs-in-division
      points-for-first-team-out-of-playoffs)))

(defn- get-point-difference-to-playoff-spot [conference-name division-name team-record standings]
  (let [wild-card-teams (parse-wild-card-teams conference-name standings)]
    (if (empty? wild-card-teams)
      ""
      (let [is-team-out-of-playoffs (> (:wildcard-sequence team-record) 2)
            no-wild-card-playoff-teams-in-division (not (some #(= (:division-name %) division-name)
                                                              (take 2 wild-card-teams)))
            comparison-team-points
            (if is-team-out-of-playoffs
              (get-comparison-team-points-for-team-out-of-playoff-spot division-name
                                                                       standings
                                                                       wild-card-teams
                                                                       no-wild-card-playoff-teams-in-division)
              (get-comparison-team-points-for-team-in-playoff-spot division-name
                                                                   wild-card-teams
                                                                   no-wild-card-playoff-teams-in-division))
            difference (- (:points team-record) comparison-team-points)]
        (str (if (> difference 0) "+" "") difference)))))

(defn- derive-standings [standings team-details]
  (let [abbreviation (:abbreviation team-details)
        team-record (parse-team-record-from-standings standings abbreviation)
        conference-id (:conference-name team-record)
        division-name (:division-name team-record)
        point-difference-to-playoff-spot
        (get-point-difference-to-playoff-spot conference-id division-name team-record standings)]
    {:division-rank            (str (:division-sequence team-record))
     :conference-rank          (str (:conference-sequence team-record))
     :league-rank              (str (:league-sequence team-record))
     :points-from-playoff-spot point-difference-to-playoff-spot}))

(defn- parse-standings [team-details standings]
  (let [away-details (:away team-details)
        home-details (:home team-details)]
    {(:abbreviation away-details)
     (derive-standings standings away-details)
     (:abbreviation home-details)
     (derive-standings standings home-details)}))

(defn- parse-team-playoff-series-wins [schedule-game team-abbreviation]
  (let [status (:series-status schedule-game)]
    (cond
      (= (:bottom-seed-team-abbrev status) team-abbreviation)
      (:bottom-seed-wins status)
      (= (:top-seed-team-abbrev status) team-abbreviation)
      (:top-seed-wins status)
      :else
      (throw (IllegalArgumentException. (str "No match for team abbreviation '" team-abbreviation "'"))))))

(defn- parse-current-playoff-series-wins [schedule-game teams]
  {(:abbreviation (:away teams))
   (parse-team-playoff-series-wins schedule-game (:abbreviation (:away teams)))
   (:abbreviation (:home teams))
   (parse-team-playoff-series-wins schedule-game (:abbreviation (:home teams)))})

(defn- parse-playoff-round [schedule-game]
  (:round (:series-status schedule-game)))

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

(defn- parse-playoff-series-information [schedule-game game-details]
  (let [teams (:teams game-details)
        parsed-current-wins (parse-current-playoff-series-wins schedule-game teams)
        win-count (apply + (vals parsed-current-wins))
        game-number (:game-number-of-series (:series-status schedule-game))
        current-wins (cond (and (< win-count game-number) (finished-game? schedule-game))
                           (add-current-game-to-playoff-series-wins parsed-current-wins game-details)

                           (and (= win-count game-number) (live-game? schedule-game))
                           (reduce-current-game-from-playoff-series-wins parsed-current-wins game-details)

                           :else
                           parsed-current-wins)
        wins-before-game (if (finished-game? schedule-game)
                           (reduce-current-game-from-playoff-series-wins current-wins game-details)
                           current-wins)
        round (parse-playoff-round schedule-game)]
    {current-stats-key {:round round :wins current-wins}
     pre-game-stats-key {:round round :wins wins-before-game}}))

(defn- parse-game-status [schedule-game gamecenter]
  (let [state (parse-game-state schedule-game)]
    (if (= state "LIVE")
      {:state state :progress (parse-game-progress schedule-game gamecenter)}
      {:state state})))

(defn- parse-game-start-time [schedule-game]
  (:start-time-utc schedule-game))

(defn- parse-integer-game-stat [stat-value]
  (if (string? stat-value)
    (Integer/parseInt stat-value)
    (or stat-value 0)))

(defn- parse-game-stat
  ([game-stats away-abbreviation home-abbreviation stat-category]
   (parse-game-stat game-stats away-abbreviation home-abbreviation stat-category parse-integer-game-stat))
  ([game-stats away-abbreviation home-abbreviation stat-category parse-fn]
   (let [stat (some #(when (= (:category %) stat-category) %) game-stats)]
     {away-abbreviation (parse-fn (:away-value stat))
      home-abbreviation (parse-fn (:home-value stat))})))

(defn- parse-power-play-stat [stat-value]
  (let [goals-and-opportunities (if (nil? stat-value) ["0" "0"] (str/split stat-value #"/"))
        goals (Integer/parseInt (first goals-and-opportunities))
        opportunities (Integer/parseInt (last goals-and-opportunities))
        percentage (if (> opportunities 0)
                     (* (/ goals (double opportunities)) 100.0)
                     0)
        formatter (DecimalFormat. "#0.0" (DecimalFormatSymbols. Locale/US))]
    {:goals         goals
     :opportunities opportunities
     :percentage    (.format formatter percentage)}))

(defn- parse-float-percentage [stat-value]
  (if (nil? stat-value)
    "0.0"
    (format "%.1f" (* 100 stat-value))))

(defn- add-game-stats [game-details team-details gamecenter]
  (if (nil? gamecenter)
    game-details
    (assoc game-details
           :game-stats
           (let [away-abbreviation (get-in team-details [:away :abbreviation])
                 home-abbreviation (get-in team-details [:home :abbreviation])
                 parse-stat (partial parse-game-stat
                                     (:team-game-stats gamecenter) away-abbreviation home-abbreviation)]
             {:blocked (parse-stat "blockedShots")
              :face-off-win-percentage (parse-stat "faceoffWinningPctg" parse-float-percentage)
              :giveaways (parse-stat "giveaways")
              :hits (parse-stat "hits")
              :pim (parse-stat "pim")
              :power-play (parse-stat "powerPlay" parse-power-play-stat)
              :shots (parse-stat "sog")
              :takeaways (parse-stat "takeaways")}))))

(defn- add-team-records [game-details current-and-pre-game-standings teams]
  (let [records (parse-records current-and-pre-game-standings teams)]
    (-> game-details
        (add-stats-field pre-game-stats-key :records (pre-game-stats-key records))
        (add-stats-field current-stats-key :records (current-stats-key records)))))

(defn- add-team-streaks [game-details schedule-game team-details current-and-pre-game-standings]
  (if (not (regular-season-game? schedule-game))
    game-details
    (-> game-details
        (add-stats-field pre-game-stats-key :streaks
                         (parse-streaks team-details (:pre-game current-and-pre-game-standings)))
        (add-stats-field current-stats-key :streaks
                         (parse-streaks team-details (:current current-and-pre-game-standings))))))

(defn- add-team-standings [game-details team-details current-and-pre-game-standings]
  (-> game-details
      (add-stats-field pre-game-stats-key :standings
                       (parse-standings team-details (:pre-game current-and-pre-game-standings)))
      (add-stats-field current-stats-key :standings
                       (parse-standings team-details (:current current-and-pre-game-standings)))))

(defn- add-playoff-series-information [game-details schedule-game]
  (if (non-playoff-game? schedule-game)
    game-details
    (let [playoff-series-information
          (parse-playoff-series-information schedule-game game-details)
          with-pre-game-stats
          (add-stats-field game-details pre-game-stats-key :playoff-series (pre-game-stats-key playoff-series-information))]
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

(defn- add-link-if-exists [map key link]
  (cond-> map
    link (assoc key (str "https://www.nhl.com" link))))

(defn- parse-links [schedule-game]
  (-> {}
      (add-link-if-exists :game-center (:game-center-link schedule-game))
      (add-link-if-exists :playoff-series (:series-url schedule-game))
      (add-link-if-exists :video-recap (:three-min-recap schedule-game))))

(defn- add-validation-errors [game-details]
  (let [errors (keep identity [(validate-score-and-goal-counts game-details)])]
    (if (empty? errors)
      game-details
      (assoc game-details :errors errors))))

(defn- reject-empty-vals-except-for-keys [game-details keys-to-keep]
  (into {} (filter #(or (contains? keys-to-keep (key %))
                        (seq (val %)))
                   game-details)))

(defn- parse-game-details
  ([current-and-pre-game-standings gamecenter schedule-game]
   (parse-game-details current-and-pre-game-standings gamecenter schedule-game nil))
  ([current-and-pre-game-standings gamecenter schedule-game _rosters]
   (let [team-details (parse-game-team-details schedule-game)
         scores (parse-scores schedule-game team-details)
         teams (get-teams team-details (:season schedule-game))]
     (-> {:status (parse-game-status schedule-game gamecenter)
          :start-time (parse-game-start-time schedule-game)
          :goals (parse-goals gamecenter)
          :links (parse-links schedule-game)
          :scores scores
          :teams teams
          pre-game-stats-key {}
          current-stats-key {}}
         (add-game-stats team-details gamecenter)
         (add-team-records current-and-pre-game-standings teams)
         (add-team-streaks schedule-game team-details current-and-pre-game-standings)
         (add-team-standings team-details current-and-pre-game-standings)
         (add-playoff-series-information schedule-game)
         (add-validation-errors)
         (reject-empty-vals-except-for-keys #{:goals :links})))))

(defn parse-game-scores
  ([date-and-schedule-games current-and-pre-game-standings]
   (parse-game-scores date-and-schedule-games current-and-pre-game-standings nil nil))
  ([date-and-schedule-games current-and-pre-game-standings gamecenters]
   (parse-game-scores date-and-schedule-games current-and-pre-game-standings gamecenters nil))
  ([date-and-schedule-games current-and-pre-game-standings gamecenters rosters]
   {:date (:date date-and-schedule-games)
    :games (map #(parse-game-details current-and-pre-game-standings (get gamecenters (:id %)) % (get rosters (:id %)))
                (:games date-and-schedule-games))}))
