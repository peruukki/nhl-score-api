#!/usr/bin/env bash

lein uberjar && heroku deploy:jar --includes newrelic target/server.jar
