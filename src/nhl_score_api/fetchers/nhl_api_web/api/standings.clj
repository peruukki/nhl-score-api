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

(defn- team-standings-updated? [pre-game-standings current-standings]
  ; In theory, it would be enough to check only one field, e.g. games played, but
  ; there have been data inconsistencies in the past, so let's check all reasonable fields
  (and (> (:games-played current-standings 0)
          (:games-played pre-game-standings 0))
       (boolean (some #(> (% current-standings 0)
                          (% pre-game-standings 0))
                      [:losses :ot-losses :wins]))))

(defrecord StandingsApiRequest [date-str schedule-response pre-game-standings-response]
  api/ApiRequest
  (archive? [_ response]
    (let [games (api/get-games-in-date-range date-str nil schedule-response)
          all-games-in-official-state? (and
                                        (> (count games) 0)
                                        (every? #(= "OFF" (:game-state %)) games))]
      (and all-games-in-official-state?
           (->> games
                (map #(seq [(get-in % [:away-team :abbrev])
                            (get-in % [:home-team :abbrev])]))
                flatten
                (every? #(team-standings-updated? (get-team-standings % pre-game-standings-response)
                                                  (get-team-standings % response)))))))
  (cache-key [_] (str "standings-" date-str))
  (description [_] (str "standings " {:date date-str}))
  (response-schema [_] ResponseSchema)
  (url [_] (str api/base-url "/standings/" date-str)))
