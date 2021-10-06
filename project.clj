(defproject nhl-score-api "0.38.0"
  :description "A JSON API that returns the scores and goals from the latest finished NHL games."
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http-lite "0.3.0"] ; clj-http-lite supports SNI (unlike http-kit or clj-http)
                 [clj-time "0.11.0"]
                 [camel-snake-kebab "0.3.2"]
                 [com.taoensso/carmine "2.19.1"]
                 [enlive "1.1.6"]
                 [http-kit "2.1.19"]
                 [ring/ring-core "1.9.4"]
                 [yleisradio/new-reliquary "1.0.1"]]
  :main nhl-score-api.core
  :profiles {:kaocha {:dependencies [[lambdaisland/kaocha "0.0-590"]]}
             :uberjar {:aot :all}}
  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner" "--reporter" "kaocha.report/documentation" "--skip-meta" "skip"]}
  :plugins [[lein-heroku "0.5.3"]]
  :uberjar-name "server.jar"
  :heroku {:app-name "nhl-score-api"
           :include-files ["target" "newrelic"]
           :process-types { "web" "java -javaagent:newrelic/newrelic.jar -jar target/server.jar" }})
