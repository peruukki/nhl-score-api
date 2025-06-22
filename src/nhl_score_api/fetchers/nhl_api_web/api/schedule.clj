(ns nhl-score-api.fetchers.nhl-api-web.api.schedule
  (:require [nhl-score-api.fetchers.nhl-api-web.api.index :as api]))

(defrecord ScheduleApiRequest [start-date-str end-date-str]
  api/ApiRequest
  (archive? [_ response] (->> response
                              (api/get-games-in-date-range start-date-str end-date-str)
                              (every? #(and (= "OFF" (:game-state %))
                                            (:three-min-recap %)))))
  (cache-key [_] (str "schedule-" start-date-str (when end-date-str (str "-" end-date-str))))
  (description [_] (str "schedule " {:date start-date-str}))
  (url [_] (str api/base-url "/schedule/" start-date-str)))
