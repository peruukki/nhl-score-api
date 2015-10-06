(ns nhl-score-api.html-parser
  (:require [net.cgrand.enlive-html :as html]))

(declare parse-games)
(declare parse-goals)

(defn parse-scores [html-resource]
  (let [dom (html/html-resource html-resource)
        games (parse-games dom)]
    (map parse-goals games)))

(defn parse-games [dom-page]
  (html/select dom-page [:.sbGame]))

(defn parse-goals [dom-game]
  (html/select dom-game [:.goalDetails]))
