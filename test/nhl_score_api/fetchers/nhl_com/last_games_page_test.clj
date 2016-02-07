(ns nhl-score-api.fetchers.nhl_com.last-games-page-test
  (:require [clojure.test :refer :all]
            [nhl-score-api.fetchers.nhl_com.last-games-page :refer :all]
            [nhl-score-api.fetchers.nhl_com.game-scores :as game-scores]
            [nhl-score-api.fetchers.nhl_com.resources :as resources]))

(deftest last-game-page-url-parsing

  (testing "Parsing last game page URL from page with finished games"
    (is (= "/ice/scores.htm?date=10/02/2015&season=20152016"
           (parse-last-games-page-path (game-scores/parse-dom resources/dom-with-game-missing-goals)))))

  (testing "Parsing last game page URL from page with games that have not yet started"
    (is (= "/ice/scores.htm?date=10/07/2015&season=20152016"
           (parse-last-games-page-path (game-scores/parse-dom resources/dom-with-not-started-games)))))

  (testing "Parsing last game page URL from page with no games"
    (is (= "/ice/scores.htm?date=10/04/2015"
           (parse-last-games-page-path (game-scores/parse-dom resources/dom-without-games))))))
