(ns nhl-score-api.fetchers.nhlstats.resources
  (:require [nhl-score-api.fetchers.nhlstats.fetcher :refer [api-response-to-json]]))

(def resource-path "test/nhl_score_api/fetchers/nhlstats/resources/")

(defn- read-resource [filename]
  (api-response-to-json (slurp (str resource-path filename))))

(def games-in-live-preview-and-final-states (read-resource "schedule-2016-02-28-live-preview-final.json"))
(def games-in-preview-state (read-resource "schedule-2016-03-30-preview.json"))
(def games-finished-in-regulation-overtime-and-shootout (read-resource "schedule-2016-03-01-final-reg-ot-so.json"))
(def latest-games-in-live-and-preview-states (read-resource "schedule-2017-11-10-live-preview.json"))
(def playoff-games-finished-in-regulation-and-overtime (read-resource "schedule-2016-04-13-final-playoff-reg-ot.json"))
(def playoff-games-finished-with-2nd-games (read-resource "schedule-2016-04-16-final-playoff-2nd-games.json"))
(def playoff-games-live-finished-with-1st-games (read-resource "schedule-2018-04-13-live-final-playoff-1st-games.json"))

(def standings (read-resource "standings-2019-02-22.json"))
