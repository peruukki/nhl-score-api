(defproject nhl-score-api "0.1.0"
  :description "A JSON API that returns the scores and goals from the latest finished NHL games."
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/data.json "0.2.6"]
                 [camel-snake-kebab "0.3.2"]
                 [enlive "1.1.6"]
                 [http-kit "2.1.19"]]
  :main nhl-score-api.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
