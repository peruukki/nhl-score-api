(ns nhl-score-api.fetchers.nhlstats.resources
  (:require [nhl-score-api.fetchers.nhlstats.fetcher :refer [api-response-to-json]]))

(def resource-path "test/nhl_score_api/fetchers/nhlstats/resources/")

(defn- read-resource [filename]
  (api-response-to-json (slurp (str resource-path filename))))

(def games-in-live-preview-and-final-states (read-resource "schedule-2016-02-28-live-preview-final-postponed-modified.json"))
(def games-in-preview-state (read-resource "schedule-2016-03-30-preview.json"))
(def games-finished-in-regulation-overtime-and-shootout (read-resource "schedule-2016-03-01-final-reg-ot-so.json"))
(def games-for-validation-testing (read-resource "schedule-2016-02-28-modified-for-validation.json"))
(def latest-games-in-live-and-preview-states (read-resource "schedule-2017-11-10-live-preview.json"))
(def playoff-games-finished-in-regulation-and-overtime (read-resource "schedule-2016-04-13-final-playoff-reg-ot.json"))
(def playoff-games-finished-with-2nd-games (read-resource "schedule-2016-04-16-final-playoff-2nd-games.json"))
(def playoff-games-live-finished-with-1st-games (read-resource "schedule-2018-04-13-live-final-playoff-1st-games-modified.json"))
(def playoff-games-with-ot-losses-in-records (read-resource "schedule-2020-08-02-playoff-games-with-ot-losses-in-records.json"))

(def standings (read-resource "standings-2020-08-02.json"))
(def standings-playoff-spots-per-division-5-3-4-4 (read-resource "standings-2020-02-16-modified.json"))

(def boxscore-2015020930 (read-resource "boxscore-2015020930.json"))
(def boxscore-2015020931 (read-resource "boxscore-2015020931.json"))
(def boxscore-2015020932 (read-resource "boxscore-2015020932.json"))
