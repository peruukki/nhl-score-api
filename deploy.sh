#!/usr/bin/env bash

lein uberjar && heroku deploy:jar --includes newrelic --options -javaagent:newrelic/newrelic.jar target/server.jar
