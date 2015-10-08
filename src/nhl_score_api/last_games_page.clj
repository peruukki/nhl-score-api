(ns nhl-score-api.last-games-page
  (:require [net.cgrand.enlive-html :as html]))

(declare parse-last-game-day-url)
(declare parse-previous-day-url)

(defn parse-last-games-page-url [dom]
  (or (parse-last-game-day-url dom)
      (parse-previous-day-url dom)))

(defn- parse-last-game-day-url [dom]
  (let [last-game-day-link (first (html/select dom [:#noGamesScheduled :a]))]
    (:href (:attrs last-game-day-link))))

(defn- parse-previous-day-url [dom]
  (let [previous-day-link (first (html/select dom [:.sectionHeader :a]))]
    (:href (:attrs previous-day-link))))
