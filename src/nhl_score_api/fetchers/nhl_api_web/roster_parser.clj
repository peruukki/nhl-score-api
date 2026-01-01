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
  (let [html-doc (html/html-snippet html-content)
        team-sections (html/select html-doc [:td.border :> :table])
        away-section (first team-sections)
        home-section (second team-sections)]
    (when (and away-section home-section)
      {:away (parse-team-roster away-section)
       :home (parse-team-roster home-section)})))
