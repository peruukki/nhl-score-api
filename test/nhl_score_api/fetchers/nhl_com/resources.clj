(ns nhl-score-api.fetchers.nhl_com.resources
  (:require [nhl-score-api.fetchers.nhl_com.game-scores :as game-scores])
  (:import (java.io File)))

(def resource-path "test/nhl_score_api/fetchers/nhl_com/resources/")

(def dom-with-ot-and-so-game (game-scores/parse-dom (File. (str resource-path "scores-2015-10-10-ot-and-so-game.htm"))))
(def dom-with-game-missing-goals (game-scores/parse-dom (File. (str resource-path "scores-2015-10-03-game-missing-goals.htm"))))
(def dom-with-not-started-games (game-scores/parse-dom (File. (str resource-path "scores-2015-10-08-not-started.htm"))))
(def dom-without-games (game-scores/parse-dom (File. (str resource-path "scores-2015-10-06-no-games.htm"))))
