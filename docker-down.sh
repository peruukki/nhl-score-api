#!/usr/bin/env bash

CONTAINER=nhl-score-api-redis

docker stop ${CONTAINER}
docker rm ${CONTAINER}
