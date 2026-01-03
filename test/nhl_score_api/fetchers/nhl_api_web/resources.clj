(ns nhl-score-api.fetchers.nhl-api-web.resources
  (:require [nhl-score-api.fetchers.nhl-api-web.fetcher :refer [api-response-to-json get-gamecenter]]))

(def resource-path "test/nhl_score_api/fetchers/nhl_api_web/resources/")

(defn- read-resource [filename]
  (api-response-to-json (slurp (str resource-path filename))))

(def first-day-of-regular-season (read-resource "schedule-2025-10-07.json"))
(def games-finished-in-regulation-overtime-and-shootout (read-resource "schedule-2023-11-08-modified.json"))
(def games-finished-in-regulation-overtime-and-shootout-minimal (read-resource "schedule-2023-11-08-modified-minimal.json"))
(def games-finished-missing-video-recap (read-resource "schedule-2023-11-08-modified-missing-video-recap.json"))
(def games-in-preview-state (read-resource "schedule-2023-11-11.json"))
(def playoff-games-live-finished-in-regulation-and-overtime (read-resource "schedule-2023-04-17-modified.json"))
(def playoff-games-live-finished-with-1st-games playoff-games-live-finished-in-regulation-and-overtime)
(def playoff-games-finished-with-2nd-games (read-resource "schedule-2023-04-19-modified.json"))
(def non-league-games (read-resource "schedule-2024-02-01-modified.json"))
(def games-for-validation-testing (read-resource "schedule-2023-11-09-modified-for-validation.json"))

(def current-standings (read-resource "standings-2023-11-09.json"))
(def current-standings-minimal (read-resource "standings-2023-11-09-modified-minimal.json"))
(def current-standings-not-fully-updated (read-resource "standings-2023-11-09-not-fully-updated.json"))
(def first-day-of-regular-season-standings (read-resource "standings-2025-10-07.json"))
(def first-day-of-regular-season-standings-not-fully-updated (read-resource "standings-2025-10-07-not-fully-updated.json"))
(def pre-game-standings (read-resource "standings-2023-11-08.json"))
(def standings-empty (read-resource "standings-empty.json"))
(def standings-for-playoffs (read-resource "standings-2023-04-14.json"))

(defn get-landing [game-id]
  (read-resource (str "landing-" game-id ".json")))
(defn get-right-rail [game-id]
  (read-resource (str "right-rail-" game-id ".json")))

(defn get-gamecenters [game-ids]
  (into {} (for [game-id game-ids]
             [game-id
              (get-gamecenter (get-landing game-id) (get-right-rail game-id))])))

(defn get-roster-api [team-abbrev season]
  (read-resource (str "roster-api-" team-abbrev "-" season ".json")))

(defn get-roster-html [game-id]
  (slurp (str resource-path "roster-" game-id ".html")))
