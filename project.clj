(defproject nhl-score-api "0.1.0-SNAPSHOT"
  :description "A small, hopefully useful, project to write Clojure."
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [enlive "1.1.6"]
                 [http-kit "2.1.19"]]
  :main nhl-score-api.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
