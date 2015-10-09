(ns nhl-score-api.resources
  (:require [nhl-score-api.game-scores :as game-scores])
  (:import (java.io File)))

(def resource-path "test/nhl_score_api/resources/")

(def dom-with-games (game-scores/parse-dom (File. (str resource-path "scores-2015-10-03.htm"))))
(def dom-with-not-started-games (game-scores/parse-dom (File. (str resource-path "scores-2015-10-08-not-started.htm"))))
(def dom-without-games (game-scores/parse-dom (File. (str resource-path "scores-2015-10-06-no-games.htm"))))
