(ns nhl-score-api.fetchers.mlbam.resources)

(def resource-path "test/nhl_score_api/fetchers/mlbam/resources/")

(def games-in-live-preview-and-final-states (slurp (str resource-path "schedule-2016-02-28-live-preview-final.json")))
