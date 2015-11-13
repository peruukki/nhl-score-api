#!/usr/bin/env bash

CMD="lein run"

REDISCLOUD_HOSTNAME=`docker-machine ip default` REDISCLOUD_PORT=63799 ${CMD}
