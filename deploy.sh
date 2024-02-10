#!/usr/bin/env bash

if [ ! -f newrelic/newrelic.jar ]; then
  echo "newrelic/newrelic.jar is missing; set up New Relic and copy downloaded New Relic files into the newrelic directory"
  exit 1
fi

lein uberjar && heroku deploy:jar --includes newrelic target/server.jar
