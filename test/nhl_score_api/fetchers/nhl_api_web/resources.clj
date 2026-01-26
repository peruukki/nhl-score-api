(ns nhl-score-api.fetchers.nhl-api-web.resources
  (:require [nhl-score-api.fetchers.nhl-api-web.fetcher :refer [api-response-to-json get-gamecenter]]))

(def resource-path "test/nhl_score_api/fetchers/nhl_api_web/resources/")

(defn- read-resource [filename]
  (slurp (str resource-path filename)))

(defn- read-json-resource [filename]
  (api-response-to-json (read-resource filename)))

(def first-day-of-regular-season (read-json-resource "schedule-2025-10-07.json"))
(def games-finished-in-regulation-overtime-and-shootout (read-json-resource "schedule-2023-11-08-modified.json"))
(def games-finished-in-regulation-overtime-and-shootout-minimal (read-json-resource "schedule-2023-11-08-modified-minimal.json"))
(def games-finished-missing-video-recap (read-json-resource "schedule-2023-11-08-modified-missing-video-recap.json"))
(def games-in-preview-state (read-json-resource "schedule-2023-11-11.json"))
(def playoff-games-live-finished-in-regulation-and-overtime (read-json-resource "schedule-2023-04-17-modified.json"))
(def playoff-games-live-finished-with-1st-games playoff-games-live-finished-in-regulation-and-overtime)
(def playoff-games-finished-with-2nd-games (read-json-resource "schedule-2023-04-19-modified.json"))
(def non-league-games (read-json-resource "schedule-2024-02-01-modified.json"))
(def games-for-validation-testing (read-json-resource "schedule-2023-11-09-modified-for-validation.json"))

(def current-standings (read-json-resource "standings-2023-11-09.json"))
(def current-standings-minimal (read-json-resource "standings-2023-11-09-modified-minimal.json"))
(def current-standings-not-fully-updated (read-json-resource "standings-2023-11-09-not-fully-updated.json"))
(def first-day-of-regular-season-standings (read-json-resource "standings-2025-10-07.json"))
(def first-day-of-regular-season-standings-not-fully-updated (read-json-resource "standings-2025-10-07-not-fully-updated.json"))
(def pre-game-standings (read-json-resource "standings-2023-11-08.json"))
(def standings-empty (read-json-resource "standings-empty.json"))
(def standings-for-playoffs (read-json-resource "standings-2023-04-14.json"))

(defn get-landing [game-id]
  (read-json-resource (str "landing-" game-id ".json")))
(defn get-right-rail [game-id]
  (read-json-resource (str "right-rail-" game-id ".json")))

(defn get-gamecenters [game-ids]
  (into {} (for [game-id game-ids]
             [game-id
              (get-gamecenter (get-landing game-id) (get-right-rail game-id))])))
