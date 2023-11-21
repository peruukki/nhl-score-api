(ns nhl-score-api.fetchers.nhl-api-web.resources
  (:require [nhl-score-api.fetchers.nhl-api-web.fetcher :refer [api-response-to-json]]))

(def resource-path "test/nhl_score_api/fetchers/nhl_api_web/resources/")

(defn- read-resource [filename]
  (api-response-to-json (slurp (str resource-path filename))))

(def games-finished-in-regulation-overtime-and-shootout (read-resource "schedule-2023-11-09-modified.json"))
(def games-in-preview-state (read-resource "schedule-2023-11-11.json"))
(def playoff-games-finished-in-regulation-and-overtime (read-resource "schedule-2023-04-17-modified.json"))
(def games-for-validation-testing (read-resource "schedule-2023-11-09-modified.json"))

(def standings (read-resource "standings-2023-11-09.json"))
(def standings-for-playoffs (read-resource "standings-2023-04-14.json"))
(def standings-parameters (read-resource "standings-season-2023-11-18.json"))

(defn get-landings [game-ids]
  (into {} (for [game-id game-ids] [game-id (read-resource (str "landing-" game-id ".json"))])))
