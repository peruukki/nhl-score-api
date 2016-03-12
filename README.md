# nhl-score-api

[![Build Status](https://travis-ci.org/peruukki/nhl-score-api.svg?branch=master)](https://travis-ci.org/peruukki/nhl-score-api)

A JSON API that returns the scores and goals from the latest finished NHL games, based on information from the
[Major League Baseball Advanced Media stats API](https://statsapi.web.nhl.com/api/v1/schedule?expand=schedule.teams,schedule.scoringplays).

The API is available at https://nhl-score-api.herokuapp.com/.

## API

### Goals from latest finished NHL games

##### `GET` [/api/scores/latest](https://nhl-score-api.herokuapp.com/api/scores/latest)

Returns an array of the latest round’s games, each game item containing these three fields:

- `goals` *(array)*
- `scores` *(object)*
- `teams` *(object)*

#### Example response:

```
[
  {
    "goals": [
      {
        "goalCount": 1,
        "period": "1",
        "scorer": "David Krejci",
        "team": "BOS",
        "min": 5,
        "sec": 36
      },
      ...
    ],
    "scores": {
      "BOS": 4,
      "CHI": 3
    },
    "teams": {
      "away": "BOS",
      "home": "CHI"
    }
  },
  {
    "goals": [
      ...
      {
        "goalCount": 1,
        "period": "OT",
        "scorer": "Kyle Turris",
        "team": "OTT",
        "min": 0,
        "sec": 30
      }
    ],
    "scores": {
      "OTT": 2,
      "DET": 1,
      "overtime": true
    },
    "teams": {
      "away": "OTT",
      "home": "DET"
    }
  },
  {
    "goals": [
      ...
      {
        "period": "SO",
        "scorer": "Phil Kessel",
        "team": "PIT"
      }
    ],
    "scores": {
      "NYR": 3,
      "PIT": 4,
      "shootout": true
    },
    "teams": {
      "away": "NYR",
      "home": "PIT"
    }
  }
]
```

#### Fields explained:

- `goals` array: list of goal details, in the order the goals were scored
  - gameplay goal:
    - `goalCount` *(number)*: the number of goals the player has scored this season
    - `min` *(number)*: the goal scoring time minutes, from the start of the period
    - `period` *(string)*: in which period the goal was scored; `"OT"` means overtime
    - `scorer` *(string)*: the goal scorer
    - `sec` *(number)*: the goal scoring time seconds, from the start of the period
    - `team` *(string)*: the team that scored the goal
  - shootout goal:
    - `period` *(string)*: `"SO"`
    - `scorer` *(string)*: the goal scorer
    - `team` *(string)*: the team that scored the goal
- `scores` object: each team’s goal count, plus one of these possible fields:
  - `overtime`: set to `true` if the game ended in overtime, absent if it didn’t
  - `shootout`: set to `true` if the game ended in shootout, absent if it didn’t
- `teams` object: away and home team names

## Requirements

- [Leiningen](http://leiningen.org/) is used for all project management.
- [Docker](https://www.docker.com/) is used for running [Redis](https://hub.docker.com/_/redis/) locally. You can
  also optionally disable Redis, in which case you won’t need Docker.

## Development Redis setup

To start a [Redis Docker container](https://hub.docker.com/_/redis/), install [Docker](https://www.docker.com/) and run:

```
./docker-up.sh
```

Downloading the Redis image will take a while on the first run, but it will be reused after that. After the command
finishes, you should see a container named `nhl-score-api-redis` running when checking Docker containers:

```
docker ps
```

To stop and delete the Redis container, run:

```
./docker-down.sh
```

## Running application

There is a script to run the application connected to a local Redis Docker container on Mac OS X:

```
./run-local-mac.sh
```

If you don’t want to use Redis caching, you can start the application with:

```
REDIS_DISABLED=true lein run
```

## Running tests

```
lein test
```

## Deployment setup

Deploying to [Heroku](http://heroku.com/):

```
lein uberjar && lein heroku deploy
```

The latest scores are cached for 5 minutes in [Heroku Redis](https://elements.heroku.com/addons/heroku-redis).

## License

[MIT](LICENSE)

## Acknowledgements

This project is a grateful recipient of the
[Futurice Open Source sponsorship program](http://futurice.com/blog/sponsoring-free-time-open-source-activities?utm_source=github&utm_medium=spice).
