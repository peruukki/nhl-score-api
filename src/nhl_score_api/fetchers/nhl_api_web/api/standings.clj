(ns nhl-score-api.fetchers.nhl-api-web.api.standings
  (:require [nhl-score-api.fetchers.nhl-api-web.api.index :as api]))

(defn- get-team-standings [team standings-response]
  (->> standings-response
       :standings
       (some #(when (= (get-in % [:team-abbrev :default]) team) %))))

(defrecord StandingsApiRequest [date-str schedule-response pre-game-standings-response]
  api/ApiRequest
  (archive? [_ response]
    (let [games (api/get-games-in-date-range date-str nil schedule-response)
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
  (url [_] (str api/base-url "/standings/" date-str)))
