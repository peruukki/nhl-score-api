#!/usr/bin/env bash

if [ "$#" -eq 0 ]; then
  echo "Missing game ID arguments"
  echo "Example usage: $0 2023020205 2023020206"
  exit 1
fi

if [ -z $(which jq) ]; then
  echo "jq is needed, see https://jqlang.github.io/jq/"
  exit 1
fi

cd "$(dirname "$0")"
for GAME_ID in "$@"
do
  echo "Fetching landing for game ID $GAME_ID"
  LANDING_FILENAME="test/nhl_score_api/fetchers/nhl_api_web/resources/landing-$GAME_ID.json"
  curl --silent "https://api-web.nhle.com/v1/gamecenter/$GAME_ID/landing" | jq > "../$LANDING_FILENAME"
  echo "Landing response saved to $LANDING_FILENAME"

  echo "Fetching right-rail for game ID $GAME_ID"
  RIGHT_RAIL_FILENAME="test/nhl_score_api/fetchers/nhl_api_web/resources/right-rail-$GAME_ID.json"
  curl --silent "https://api-web.nhle.com/v1/gamecenter/$GAME_ID/right-rail" | jq > "../$RIGHT_RAIL_FILENAME"
  echo "Right-rail response saved to $RIGHT_RAIL_FILENAME"
done
cd - >/dev/null
