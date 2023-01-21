#!/usr/bin/env bash

lein uberjar && heroku deploy:jar target/server.jar
