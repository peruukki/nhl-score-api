(ns nhl-score-api.fetchers.nhl_com.last-games-page
  (:require [net.cgrand.enlive-html :as html]))

(declare parse-last-game-day-url parse-previous-day-url)

(defn parse-last-games-page-path [dom]
  (or (parse-last-game-day-url dom)
      (parse-previous-day-url dom)))

(defn- parse-last-game-day-url [dom]
  (let [last-game-day-link (first (html/select dom [:#noGamesScheduled :a]))]
    (:href (:attrs last-game-day-link))))

(defn- parse-previous-day-url [dom]
  (let [previous-day-link (first (html/select dom [:.sectionHeader :a]))]
    (:href (:attrs previous-day-link))))