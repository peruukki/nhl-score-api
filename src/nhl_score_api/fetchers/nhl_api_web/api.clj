(ns nhl-score-api.fetchers.nhl-api-web.api)

(def base-url "https://api-web.nhle.com/v1")

(defn- get-games-in-date-range [start-date-str end-date-str schedule-response]
  (->> schedule-response
       :game-week
       (filter (if end-date-str
                 #(and (<= (compare start-date-str (:date %)) 0)
                       (<= (compare (:date %) end-date-str) 0))
                 #(= 0 (compare start-date-str (:date %)))))
       (map :games)
       flatten))

(defn- get-team-standings [team standings-response]
  (->> standings-response
       :standings
       (some #(when (= (get-in % [:team-abbrev :default]) team) %))))

(defprotocol ApiRequest
  (archive? [_ response])
  (cache-key [_])
  (description [_])
  (url [_]))

(defrecord LandingApiRequest [game-id]
  ApiRequest
  (archive? [_ response] (= "OFF" (:game-state response)))
  (cache-key [_] (str "landing-" game-id))
  (description [_] (str "landing " {:game-id game-id}))
  (url [_] (str base-url "/gamecenter/" game-id "/landing")))

(defrecord ScheduleApiRequest [start-date-str end-date-str]
  ApiRequest
  (archive? [_ response] (->> response
                              (get-games-in-date-range start-date-str end-date-str)
                              (every? #(and (= "OFF" (:game-state %))
                                            (:three-min-recap %)))))
  (cache-key [_] (str "schedule-" start-date-str (when end-date-str (str "-" end-date-str))))
  (description [_] (str "schedule " {:date start-date-str}))
  (url [_] (str base-url "/schedule/" start-date-str)))

(defrecord StandingsApiRequest [date-str schedule-response pre-game-standings-response]
  ApiRequest
  (archive? [_ response]
            (let [games (get-games-in-date-range date-str nil schedule-response)
                  all-games-in-official-state? (every? #(= "OFF" (:game-state %)) games)]
              (if (and all-games-in-official-state? pre-game-standings-response)
                (->> games
                     (map #(seq [(get-in % [:away-team :abbrev])
                                 (get-in % [:home-team :abbrev])]))
                     flatten
                     (every? #(let [pre-game-standings (get-team-standings % pre-game-standings-response)
                                    current-standings (get-team-standings % response)]
                                (and pre-game-standings
                                     current-standings
                                     (> (:games-played current-standings)
                                        (:games-played pre-game-standings))))))
                all-games-in-official-state?)))
  (cache-key [_] (str "standings-" date-str))
  (description [_] (str "standings " {:date date-str}))
  (url [_] (str base-url "/standings/" date-str)))
