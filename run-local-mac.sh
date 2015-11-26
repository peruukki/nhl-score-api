#!/usr/bin/env bash

CMD="lein run"

REDIS_URL="redis://`docker-machine ip default`:63799" ${CMD}
