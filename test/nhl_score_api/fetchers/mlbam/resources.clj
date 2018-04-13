(ns nhl-score-api.fetchers.mlbam.resources)

(def resource-path "test/nhl_score_api/fetchers/mlbam/resources/")

(def games-in-live-preview-and-final-states (slurp (str resource-path "schedule-2016-02-28-live-preview-final.json")))
(def games-in-preview-state (slurp (str resource-path "schedule-2016-03-30-preview.json")))
(def games-finished-in-regulation-overtime-and-shootout (slurp (str resource-path "schedule-2016-03-01-final-reg-ot-so.json")))
(def latest-games-in-live-and-preview-states (slurp (str resource-path "schedule-2017-11-10-live-preview.json")))
(def playoff-games-finished-in-regulation-and-overtime (slurp (str resource-path "schedule-2016-04-13-final-playoff-reg-ot.json")))
(def playoff-games-finished-with-2nd-games (slurp (str resource-path "schedule-2016-04-16-final-playoff-2nd-games.json")))
(def playoff-games-live-finished-with-1st-games (slurp (str resource-path "schedule-2018-04-13-live-final-playoff-1st-games.json")))
