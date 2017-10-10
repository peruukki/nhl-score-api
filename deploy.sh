#!/usr/bin/env bash

lein uberjar && lein heroku deploy
