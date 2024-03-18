(defproject nhl-score-api "0.48.4"
  :description "A JSON API that returns the scores and goals from the latest finished or on-going NHL games."
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.cache "1.1.234"]
                 [org.clojure/data.json "2.4.0"]
                 [clj-http "3.12.3"]
                 [clj-time "0.15.2"]
                 [camel-snake-kebab "0.4.3"]
                 [enlive "1.1.6"]
                 [http-kit "2.1.19"]
                 [jakarta.xml.bind/jakarta.xml.bind-api "2.3.3"]
                 [org.glassfish.jaxb/jaxb-runtime "2.3.7"]
                 [ring/ring-core "1.9.6"]
                 [yleisradio/new-reliquary "1.1.0"]]
  :main nhl-score-api.core
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]]
  :profiles {:kaocha  {:dependencies [[lambdaisland/kaocha "1.87.1366"]]}
             :uberjar {:aot :all}}
  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner" "--reporter" "kaocha.report/documentation" "--skip-meta" "skip"]}
  :uberjar-name "server.jar")
