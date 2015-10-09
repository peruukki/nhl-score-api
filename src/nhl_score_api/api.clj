(ns nhl-score-api.api
  (:require [nhl-score-api.game-scores :as game-scores]
            [nhl-score-api.last-games-page :as last-games-page])
  (:import (java.net URL)))

(def scores-base-url "http://www.nhl.com")
(def scores-path "/ice/scores.htm")

(defn- parse-page-dom [page-path]
  (game-scores/parse-dom (URL. (str scores-base-url page-path))))

(defn fetch-latest-scores []
  (let [current-scores-dom (parse-page-dom scores-path)
        current-scores (game-scores/parse-scores current-scores-dom)]
    (if (seq current-scores)
      current-scores
      (let [last-scores-path (last-games-page/parse-last-games-page-path current-scores-dom)
            latest-scores-dom (parse-page-dom last-scores-path)]
        (game-scores/parse-scores latest-scores-dom)))))
