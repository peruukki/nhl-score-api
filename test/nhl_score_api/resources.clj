(ns nhl-score-api.resources
  (:import (java.io File)))

(def resource-path "test/nhl_score_api/resources/")

(def html-with-games (File. (str resource-path "scores-2015-10-03.htm")))
(def html-with-not-started-games (File. (str resource-path "scores-2015-10-08-not-started.htm")))
(def html-without-games (File. (str resource-path "scores-2015-10-06-no-games.htm")))
