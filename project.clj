(defproject nhl-score-api "0.42.0"
  :description "A JSON API that returns the scores and goals from the latest finished NHL games."
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.memoize "1.0.257"]
                 [org.clojure/data.json "2.4.0"]
                 [clj-http-lite "0.3.0"] ; clj-http-lite supports SNI (unlike http-kit or clj-http)
                 [clj-time "0.15.2"]
                 [camel-snake-kebab "0.4.3"]
                 [enlive "1.1.6"]
                 [http-kit "2.1.19"]
                 [jakarta.xml.bind/jakarta.xml.bind-api "2.3.3"]
                 [org.glassfish.jaxb/jaxb-runtime "2.3.7"]
                 [ring/ring-core "1.9.6"]
                 [yleisradio/new-reliquary "1.1.0"]]
  :main nhl-score-api.core
  :profiles {:kaocha  {:dependencies [[lambdaisland/kaocha "1.70.1086"]]}
             :uberjar {:aot :all}}
  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner" "--reporter" "kaocha.report/documentation" "--skip-meta" "skip"]}
  :plugins [[lein-heroku "0.5.3"]]
  :uberjar-name "server.jar"
  :heroku {:app-name "nhl-score-api"
           :include-files ["target" "newrelic"]
           :process-types { "web" "java -javaagent:newrelic/newrelic.jar -jar target/server.jar" }})
