#!/usr/bin/env bash

CONTAINER=nhl-score-api-redis
EXPOSED_PORT=63799

docker run --name ${CONTAINER} -p ${EXPOSED_PORT}:6379 -d redis:2.8
