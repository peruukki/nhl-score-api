#!/usr/bin/env bash

CMD="lein run"

REDISCLOUD_HOSTNAME=`boot2docker ip` REDISCLOUD_PORT=63799 ${CMD}
