#!/usr/bin/env bash

lein uberjar && heroku deploy:jar -i newrelic target/server.jar
