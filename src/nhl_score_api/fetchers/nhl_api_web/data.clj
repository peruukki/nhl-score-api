(ns nhl-score-api.fetchers.nhl-api-web.data)

(def ^:private team-names
  {"ANA" {:short-name "Anaheim"
          :team-name  "Ducks"}
   "ARI" {:short-name "Arizona"
          :team-name  "Coyotes"} ; Replaced by Utah starting from 2024–2025 season
   "BOS" {:short-name "Boston"
          :team-name  "Bruins"}
   "BUF" {:short-name "Buffalo"
          :team-name  "Sabres"}
   "CGY" {:short-name "Calgary"
          :team-name  "Flames"}
   "CAR" {:short-name "Carolina"
          :team-name  "Hurricanes"}
   "CHI" {:short-name "Chicago"
          :team-name  "Blackhawks"}
   "COL" {:short-name "Colorado"
          :team-name  "Avalanche"}
   "CBJ" {:short-name "Columbus"
          :team-name  "Blue Jackets"}
   "DAL" {:short-name "Dallas"
          :team-name  "Stars"}
   "DET" {:short-name "Detroit"
          :team-name  "Red Wings"}
   "EDM" {:short-name "Edmonton"
          :team-name  "Oilers"}
   "FLA" {:short-name "Florida"
          :team-name  "Panthers"}
   "LAK" {:short-name "Los Angeles"
          :team-name  "Kings"}
   "MIN" {:short-name "Minnesota"
          :team-name  "Wild"}
   "MTL" {:short-name "Montréal"
          :team-name  "Canadiens"}
   "NSH" {:short-name "Nashville"
          :team-name  "Predators"}
   "NJD" {:short-name "New Jersey"
          :team-name  "Devils"}
   "NYI" {:short-name "NY Islanders"
          :team-name  "Islanders"}
   "NYR" {:short-name "NY Rangers"
          :team-name  "Rangers"}
   "OTT" {:short-name "Ottawa"
          :team-name  "Senators"}
   "PHI" {:short-name "Philadelphia"
          :team-name  "Flyers"}
   "PIT" {:short-name "Pittsburgh"
          :team-name  "Penguins"}
   "SJS" {:short-name "San Jose"
          :team-name  "Sharks"}
   "SEA" {:short-name "Seattle"
          :team-name  "Kraken"}
   "STL" {:short-name "St Louis"
          :team-name  "Blues"}
   "TBL" {:short-name "Tampa Bay"
          :team-name  "Lightning"}
   "TOR" {:short-name "Toronto"
          :team-name  "Maple Leafs"}
   "UTA" {:short-name "Utah"
          :team-name  "Mammoth"
          :previous-team-name {:until-season 20252026 :team-name "Hockey Club"}}
   "VAN" {:short-name "Vancouver"
          :team-name  "Canucks"}
   "VGK" {:short-name "Vegas"
          :team-name  "Golden Knights"}
   "WSH" {:short-name "Washington"
          :team-name  "Capitals"}
   "WPG" {:short-name "Winnipeg"
          :team-name  "Jets"}})

(defn get-team-names [team-abbreviation season]
  (let [team (get team-names team-abbreviation {:short-name "" :team-name ""})
        effective-team-name (or (when-let [previous (:previous-team-name team)]
                                  (when (< season (:until-season previous))
                                    (:team-name previous)))
                                (:team-name team))]
    {:short-name (:short-name team)
     :team-name effective-team-name}))
