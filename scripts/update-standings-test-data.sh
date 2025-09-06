#!/usr/bin/env bash

if [ "$#" -eq 0 ]; then
  echo "Missing standings date arguments"
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
  echo "Fetching standings for date $DATE"
  STANDINGS_FILENAME="test/nhl_score_api/fetchers/nhl_api_web/resources/standings-$DATE.json"
  curl --silent "https://api-web.nhle.com/v1/standings/$DATE" | jq > "../$STANDINGS_FILENAME"
  echo "Standings response saved to $STANDINGS_FILENAME"
done
cd - >/dev/null
