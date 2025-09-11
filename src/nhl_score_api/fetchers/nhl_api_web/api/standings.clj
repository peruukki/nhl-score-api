(ns nhl-score-api.fetchers.nhl-api-web.api.standings
  (:require [malli.core :as malli]
            [nhl-score-api.fetchers.nhl-api-web.api.index :as api]
            [nhl-score-api.fetchers.nhl-api-web.api.schema :as schema]))

(def TeamStandingSchema
  (malli/schema
   [:map
    [:games-played :int]
    [:losses :int]
    [:ot-losses :int]
    [:place-name #'schema/Localized]
    [:points :int]
    [:streak-code {:optional true} :string]
    [:streak-count {:optional true} :int]
    [:team-abbrev #'schema/Localized]
    [:wildcard-sequence :int]
    [:wins :int]]))

(def ResponseSchema
  (malli/schema
   [:map
    [:standings [:vector TeamStandingSchema]]]))

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
  (response-schema [_] ResponseSchema)
  (url [_] (str api/base-url "/standings/" date-str)))
