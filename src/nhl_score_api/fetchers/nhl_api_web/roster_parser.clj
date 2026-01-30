(ns nhl-score-api.fetchers.nhl-api-web.roster-parser
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as html]))

(def ^:private name-overrides
  "Map of uppercase name (or word) to normalized form for names that do not follow title-case rules."
  {"DEANGELO"  "DeAngelo"
   "JJ"        "JJ"
   "MACCELLI"  "Maccelli"
   "MACKENZIE" "Mackenzie"
   "MACKEY"    "Mackey"
   "VAN"       "van"})

(defn normalize-name
  "Normalizes player name from UPPERCASE HTML format to title case.
   Removes captain/alternate markers like '(C)' or '(A)'.
   Handles hyphenated names (e.g. Ekman-Larsson), apostrophes (e.g. O'Reilly),
   Mc/Mac prefixes (e.g. McDavid, MacTavish), initials (e.g. J.J.), and name overrides (e.g. DeAngelo)."
  [name-str]
  (let [apply-override (fn [word]
                         (get name-overrides (str/upper-case word) word))
        fix-initials (fn [s]
                       (str/replace s #"\.([a-z])" (fn [[_ c]] (str "." (str/upper-case c)))))
        fix-mac-mc (fn [base]
                     (cond
                       (and (> (count base) 3) (str/starts-with? base "Mac"))
                       (str "Mac" (str/capitalize (subs base 3)))
                       (and (> (count base) 2) (str/starts-with? base "Mc"))
                       (str "Mc" (str/capitalize (subs base 2)))
                       :else base))
        capitalize-segment (fn [s]
                             (let [base (->> (str/split s #"'")
                                             (map str/capitalize)
                                             (str/join "'"))]
                               (-> base fix-mac-mc fix-initials)))
        capitalize-word (fn [word]
                          (->> (str/split word #"-")
                               (map capitalize-segment)
                               (str/join "-")
                               apply-override))
        strip-captaincy-indicators (fn [s]
                                     (str/replace s #"\s*\([CA]\)\s*" ""))]
    (-> name-str
        strip-captaincy-indicators
        str/trim
        (str/split #"\s+")
        (->> (map capitalize-word)
             (str/join " ")))))

(defn- has-bold-class?
  "Checks if any td element in the row has a class containing 'bold'."
  [row]
  (let [tds (html/select row [:td])]
    (some #(when-let [class-attr (:class (:attrs %))]
             (or (= class-attr "bold")
                 (and (string? class-attr)
                      (str/includes? class-attr "bold"))))
          tds)))

(defn- parse-player-row
  "Parses a player row from the roster table.
   Returns a map with :name, :position, :number, and optionally :starting-lineup."
  [row]
  (let [tds (html/select row [:td])
        number-td (nth tds 0)
        position-td (nth tds 1)
        name-td (nth tds 2)
        number (-> number-td html/text str/trim)
        position (-> position-td html/text str/trim)
        name (-> name-td html/text str/trim normalize-name)
        starting-lineup (has-bold-class? row)]
    (cond-> {:name name
             :number (Integer/parseInt number)
             :position position}
      starting-lineup (assoc :starting-lineup true))))

(defn- parse-team-dressed-players
  "Parses dressed players from a team's roster table.
   Returns a vector of player maps."
  [table]
  (->> (html/select table [:tr])
       (drop 1)
       (filter #(let [tds (html/select % [:td])]
                  (and (= (count tds) 3)
                       (not (str/blank? (html/text (nth tds 0))))
                       (not (str/blank? (html/text (nth tds 1))))
                       (not (str/blank? (html/text (nth tds 2)))))))
       (map parse-player-row)
       vec))

(defn- parse-team-scratched-players
  "Parses scratched players from a team's scratched players table.
   Returns a vector of player maps."
  [table]
  (->> (html/select table [:tr])
       (drop 1)
       (filter #(let [tds (html/select % [:td])]
                  (and (= (count tds) 3)
                       (not (str/blank? (html/text (nth tds 0))))
                       (not (str/blank? (html/text (nth tds 1))))
                       (not (str/blank? (html/text (nth tds 2)))))))
       (map #(let [tds (html/select % [:td])
                   number-td (nth tds 0)
                   position-td (nth tds 1)
                   name-td (nth tds 2)
                   number (-> number-td html/text str/trim)
                   position (-> position-td html/text str/trim)
                   name (-> name-td html/text str/trim normalize-name)]
               {:name name
                :number (Integer/parseInt number)
                :position position}))
       vec))

(defn- is-roster-table?
  "Checks if a table is a player roster table by examining its structure."
  [table]
  (let [rows (html/select table [:tr])
        header-row (first rows)
        header-tds (when header-row (html/select header-row [:td]))
        header-texts (when header-row (map html/text header-tds))
        ; Check if header row has exactly 3 columns with #, Pos, Name
        has-correct-header (and header-tds
                                (= (count header-tds) 3)
                                (some #(str/includes? (str/trim %) "#") header-texts)
                                (some #(str/includes? (str/trim %) "Pos") header-texts)
                                (some #(str/includes? (str/trim %) "Name") header-texts))
        ; Check if there are player rows (rows with numeric first column)
        has-player-rows (some (fn [row]
                                (let [tds (html/select row [:td])
                                      first-td-text (when (seq tds) (str/trim (html/text (first tds))))]
                                  (and first-td-text
                                       (re-matches #"^\d+$" first-td-text))))
                              (rest rows))]
    (and has-correct-header has-player-rows)))

(defn- find-roster-tables
  "Finds the roster tables by looking for tables with '#', 'Pos', 'Name' headers and player rows.
   Returns a vector of [away-dressed-table home-dressed-table away-scratched-table home-scratched-table]."
  [doc]
  (let [all-tables (html/select doc [:table])
        roster-tables (filter is-roster-table? all-tables)]
    (vec roster-tables)))

(defn parse-roster-html
  "Parses roster HTML and extracts player information.
   Returns a map with :away and :home keys, each containing
   :dressed-players and :scratched-players vectors."
  [html-str]
  (let [doc (html/html-resource (java.io.StringReader. html-str))
        ; Find roster tables by looking for tables with the header row
        roster-tables (find-roster-tables doc)
        ; The first two roster tables are the dressed players (away, then home)
        ; The next two (if present) are scratched players (away, then home)
        dressed-tables (take 2 roster-tables)
        scratched-tables (drop 2 roster-tables)
        away-dressed-table (first dressed-tables)
        home-dressed-table (second dressed-tables)
        away-scratched-table (first scratched-tables)
        home-scratched-table (second scratched-tables)]
    {:away {:dressed-players (when away-dressed-table
                               (parse-team-dressed-players away-dressed-table))
            :scratched-players (when away-scratched-table
                                 (parse-team-scratched-players away-scratched-table))}
     :home {:dressed-players (when home-dressed-table
                               (parse-team-dressed-players home-dressed-table))
            :scratched-players (when home-scratched-table
                                 (parse-team-scratched-players home-scratched-table))}}))
