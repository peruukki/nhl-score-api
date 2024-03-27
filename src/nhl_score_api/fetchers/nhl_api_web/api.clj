(ns nhl-score-api.fetchers.nhl-api-web.api)

(def base-url "https://api-web.nhle.com/v1")

(defn- all-games-in-official-state? [schedule-response start-date-str end-date-str]
  (->> schedule-response
       :game-week
       (filter (if end-date-str
                 #(and (<= (compare start-date-str (:date %)) 0)
                       (<= (compare (:date %) end-date-str) 0))
                 #(= 0 (compare start-date-str (:date %)))))
       (map :games)
       flatten
       (every? #(= "OFF" (:game-state %)))))

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
  (archive? [_ response] (all-games-in-official-state? response start-date-str end-date-str))
  (cache-key [_] (str "schedule-" start-date-str (when end-date-str (str "-" end-date-str))))
  (description [_] (str "schedule " {:date start-date-str}))
  (url [_] (str base-url "/schedule/" start-date-str)))

(defrecord StandingsApiRequest [date-str schedule-response]
  ApiRequest
  (archive? [_ _] (all-games-in-official-state? schedule-response date-str nil))
  (cache-key [_] (str "standings-" date-str))
  (description [_] (str "standings " {:date date-str}))
  (url [_] (str base-url "/standings/" date-str)))
