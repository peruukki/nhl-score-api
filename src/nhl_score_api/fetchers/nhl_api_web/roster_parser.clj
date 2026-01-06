(ns nhl-score-api.fetchers.nhl-api-web.roster-parser
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as html]))

(defn- normalize-name
  "Normalizes a player name from HTML format (UPPERCASE) to title case.
   Removes captain/alternate markers like '(C)' or '(A)'.
   Handles abbreviations (e.g., 'A.J.' stays as 'A.J.').
   Example: 'MIKAEL BACKLUND  (C)' -> 'Mikael Backlund'
   Example: 'A.J. GREER' -> 'A.J. Greer'"
  [name-str]
  (-> name-str
      str/trim
      (str/replace #"\s*\([CA]\)\s*" "") ; Remove (C) or (A) markers
      str/trim
      (str/split #"\s+")
      (->> (map (fn [word]
                  (if (empty? word)
                    word
                    (if (re-find #"^[A-Z]\.[A-Z]\.?$" word)
                       ;; Abbreviation like "A.J." or "A.J" - keep as is
                      word
                       ;; Regular word - title case
                      (str (str/upper-case (subs word 0 1))
                           (str/lower-case (subs word 1)))))))
           (str/join " "))))

(defn- parse-number
  "Parses jersey number from text content of a td element."
  [td]
  (when-let [text (str/trim (html/text td))]
    (try
      (Integer/parseInt text)
      (catch NumberFormatException _
        nil))))

(defn- parse-position
  "Parses position code from text content of a td element.
   Returns position code (G, D, C, L, R) or nil."
  [td]
  (when-let [pos (str/trim (html/text td))]
    (when (contains? #{"G" "D" "C" "L" "R"} pos)
      pos)))

(defn- parse-name
  "Parses and normalizes player name from text content of a td element."
  [td]
  (when-let [name-text (html/text td)]
    (normalize-name name-text)))

(defn- is-starting-lineup?
  "Checks if a td element has 'bold' in its class attribute.
   Starting lineup players have class='bold' or class='bold + italic'."
  [td]
  (when-let [class-attr (:class (:attrs td))]
    (let [class-str (if (string? class-attr)
                      class-attr
                      (str class-attr))]
      (boolean (re-find #"bold" class-str)))))

(defn- parse-player-row
  "Parses a player row (<tr>) from the roster HTML.
   Returns a map with :number, :position, :name, and :starting-lineup (boolean)."
  [row]
  (let [tds (html/select row [:td])
        number-td (nth tds 0 nil)
        position-td (nth tds 1 nil)
        name-td (nth tds 2 nil)]
    (when (and number-td position-td name-td)
      (let [number (parse-number number-td)
            position (parse-position position-td)
            name (parse-name name-td)
            starting-lineup (or (is-starting-lineup? number-td)
                                (is-starting-lineup? position-td)
                                (is-starting-lineup? name-td))]
        (when (and number position name)
          {:number number
           :position position
           :name name
           :starting-lineup starting-lineup})))))

(defn- parse-team-roster
  "Parses roster data for a single team from the HTML.
   team-section should be a seq of nodes containing the team's roster table."
  [team-section]
  (let [roster-table (first (html/select team-section [:table]))
        player-rows (html/select roster-table [:tr])]
    (->> player-rows
         (drop 1) ; Skip header row
         (map parse-player-row)
         (filter some?)
         vec)))

(defn parse-roster-html
  "Parses roster HTML and extracts player information for both teams.

   Returns a map with:
   - :away - vector of player maps for away team
   - :home - vector of player maps for home team

   Each player map contains:
   - :number - jersey number (integer)
   - :position - position code (G, D, C, L, R)
   - :name - normalized player name (string)
   - :starting-lineup - boolean indicating if player is in starting lineup

   Example:
   {:away [{:number 80 :position \"G\" :name \"Dan Vladar\" :starting-lineup true}
           {:number 32 :position \"G\" :name \"Dustin Wolf\" :starting-lineup false}]
    :home [{:number 60 :position \"G\" :name \"Joseph Woll\" :starting-lineup true}]}"
  [html-content]
  (let [html-doc (html/html-resource (java.io.StringReader. html-content))
        team-sections (html/select html-doc [:td.border :> :table])
        away-section (first team-sections)
        home-section (second team-sections)]
    (when (and away-section home-section)
      {:away (parse-team-roster away-section)
       :home (parse-team-roster home-section)})))

(defn- combine-api-roster-arrays
  "Combines forwards, defensemen, and goalies arrays from API roster response
   into a single list of players.

   api-roster-response should be a map with :forwards, :defensemen, and :goalies keys.

   Returns a vector of player maps with keys transformed to kebab-case:
   - :id (player ID)
   - :first-name (localized object)
   - :last-name (localized object)
   - :sweater-number (jersey number)
   - :position-code (position code)"
  [api-roster-response]
  (->> [:forwards :defensemen :goalies]
       (mapcat #(get api-roster-response % []))
       vec))

(defn- get-api-player-name
  "Extracts full name from API player data.
   Combines first-name and last-name from localized objects.
   Returns normalized name string (title case)."
  [api-player]
  (let [first-name (get-in api-player [:first-name :default] "")
        last-name (get-in api-player [:last-name :default] "")]
    (str/trim (str first-name " " last-name))))

(defn- match-by-number-and-position
  "Matches HTML player with API player by jersey number and position code.
   Returns matching API player or nil."
  [html-player api-players]
  (first (filter (fn [api-player]
                   (and (= (:number html-player)
                           (:sweater-number api-player))
                        (= (:position html-player)
                           (:position-code api-player))))
                 api-players)))

(defn- match-by-name
  "Matches HTML player with API player by normalized name comparison.
   Returns matching API player or nil."
  [html-player api-players]
  (let [html-name (str/lower-case (:name html-player))]
    (first (filter (fn [api-player]
                     (let [api-name (str/lower-case (get-api-player-name api-player))]
                       (= html-name api-name)))
                   api-players))))

(defn- match-player
  "Matches an HTML player with an API player.
   Uses primary matching (jersey number + position) first, then fallback (name).
   Returns matching API player or nil."
  [html-player api-players]
  (or (match-by-number-and-position html-player api-players)
      (match-by-name html-player api-players)))

(defn enrich-roster-with-api-data
  "Enriches HTML roster data with player IDs from API roster data.

   html-roster should be a map with :away and :home keys, each containing
   vectors of player maps from parse-roster-html.

   api-roster-away and api-roster-home should be API roster response maps
   (with :forwards, :defensemen, :goalies keys).

   Returns enriched roster map with same structure as html-roster, but each
   player map includes:
   - :player-id (from API, when matched)
   - All original HTML fields (:number, :position, :name, :starting-lineup)

   Players that can't be matched are included with HTML data only (no :player-id)."
  [html-roster api-roster-away api-roster-home]
  (let [api-players-away (combine-api-roster-arrays api-roster-away)
        api-players-home (combine-api-roster-arrays api-roster-home)
        enrich-team (fn [html-players api-players]
                      (mapv (fn [html-player]
                              (if-let [matched-api-player (match-player html-player api-players)]
                                (assoc html-player :player-id (:id matched-api-player))
                                html-player))
                            html-players))]
    {:away (enrich-team (:away html-roster) api-players-away)
     :home (enrich-team (:home html-roster) api-players-home)}))
