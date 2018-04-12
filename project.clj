(defproject nhl-score-api "0.16.1"
  :description "A JSON API that returns the scores and goals from the latest finished NHL games."
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http-lite "0.3.0"] ; clj-http-lite supports SNI (unlike http-kit or clj-http)
                 [clj-time "0.11.0"]
                 [camel-snake-kebab "0.3.2"]
                 [com.taoensso/carmine "2.12.0"]
                 [enlive "1.1.6"]
                 [http-kit "2.1.19"]
                 [yleisradio/new-reliquary "1.0.1"]]
  :main nhl-score-api.core
  :profiles {:uberjar {:aot :all}}
  :plugins [[lein-heroku "0.5.3"]]
  :uberjar-name "server.jar"
  :heroku {:app-name "nhl-score-api"
           :include-files ["target" "newrelic"]
           :process-types { "web" "java -javaagent:newrelic/newrelic.jar -jar target/server.jar" }})
