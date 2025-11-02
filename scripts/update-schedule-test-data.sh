#!/usr/bin/env bash

if [ "$#" -eq 0 ]; then
  echo "Missing schedule date arguments"
  echo "Example usage: $0 2023-04-14 2023-11-08"
  exit 1
fi

if [ -z $(which jq) ]; then
  echo "jq is needed, see https://jqlang.github.io/jq/"
  exit 1
fi

cd "$(dirname "$0")"
for DATE in "$@"
do
  echo "Fetching schedule for date $DATE"
  SCHEDULE_FILENAME="test/nhl_score_api/fetchers/nhl_api_web/resources/schedule-$DATE.json"
  curl --silent "https://api-web.nhle.com/v1/schedule/$DATE" | jq > "../$SCHEDULE_FILENAME"
  echo "Schedule response saved to $SCHEDULE_FILENAME"
done
cd - >/dev/null
