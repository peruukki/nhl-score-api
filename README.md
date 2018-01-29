# nhl-score-api

[![Build Status](https://travis-ci.org/peruukki/nhl-score-api.svg?branch=master)](https://travis-ci.org/peruukki/nhl-score-api)

A JSON API that returns the scores and goals from the latest finished or on-going NHL games, based on information from the
[Major League Baseball Advanced Media stats API](https://statsapi.web.nhl.com/api/v1/schedule?expand=schedule.teams,schedule.scoringplays).

The API is available at https://nhl-score-api.herokuapp.com/, and it serves as the backend for [nhl-recap](https://github.com/peruukki/nhl-recap).

## API

### Goals from latest finished NHL games

##### `GET` [/api/scores/latest](https://nhl-score-api.herokuapp.com/api/scores/latest)

Returns an object with the date and the scores from the latest round’s games.

The `date` object contains the date in a raw format and a prettier, displayable format.

The `games` array contains details of the games, each game item containing these fields:

- `state` *(string)*
- `goals` *(array)*
- `scores` *(object)*
- `teams` *(object)*
- `records` *(object)*, not included in all star games
- `playoffSeries` *(object)*, only included if the game is a playoff game

#### Example response:

```json
{
  "date": {
    "raw": "2017-10-16",
    "pretty": "Mon Oct 16"
  },
  "games": [
    {
      "state": "FINAL",
      "goals": [
        ...
        {
          "goalCount": 1,
          "period": "1",
          "scorer": "David Krejci",
          "team": "BOS",
          "min": 5,
          "sec": 36,
          "strength": "PPG",
          "emptyNet": true
        }
      ],
      "scores": {
        "BOS": 4,
        "CHI": 3
      },
      "teams": {
        "away": "BOS",
        "home": "CHI"
      },
      "records": {
        "BOS": {
          "wins": 44,
          "losses": 31,
          "ot": 7
        },
        "CHI": {
          "wins": 50,
          "losses": 23,
          "ot": 9
        }
      },
      "playoffSeries": {
        "wins": {
          "BOS": 0,
          "CHI": 0
        }
      }
    },
    {
      "state": "LIVE",
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
      },
      "records": {
        "OTT": {
          "wins": 44,
          "losses": 28,
          "ot": 10
        },
        "DET": {
          "wins": 33,
          "losses": 36,
          "ot": 13
        }
      },
      "playoffSeries": {
        "wins": {
          "OTT": 0,
          "DET": 1
        }
      }
    },
    {
      "state": "PREVIEW",
      "goals": [],
      "scores": {
        "NYR": 0,
        "PIT": 0
      },
      "teams": {
        "away": "NYR",
        "home": "PIT"
      },
      "records": {
        "NYR": {
          "wins": 48,
          "losses": 28,
          "ot": 6
        },
        "PIT": {
          "wins": 50,
          "losses": 21,
          "ot": 11
        }
      },
      "playoffSeries": {
        "wins": {
          "NYR": 1,
          "PIT": 1
        }
      }
    }
  ]
}
```

#### Date fields explained:

- `raw` *(string)*: the raw date in "yyyy-MM-dd" format, usable for any kind of processing
- `pretty` *(string)*: a prettified format, can be shown as-is in the client

#### Game fields explained:

- `state` *(string)*:
  - `FINAL` if the game has ended
  - `LIVE` if the game is still in progress
  - `PREVIEW` if the game has not started yet
- `goals` array: list of goal details, in the order the goals were scored
  - gameplay goal:
    - `goalCount` *(number)*: the number of goals the player has scored this season
    - `emptyNet` *(boolean)*: set to `true` if the goal was scored in an empty net, absent if it wasn’t
    - `min` *(number)*: the goal scoring time minutes, from the start of the period
    - `period` *(string)*: in which period the goal was scored; `"OT"` means **regular season 5 minute overtime**
    - `scorer` *(string)*: the goal scorer
    - `sec` *(number)*: the goal scoring time seconds, from the start of the period
    - `strength` *(string)*: can be set to `"PPG"` (power play goal) or `"SHG"` (short handed goal); absent
      if the goal was scored on even strength
    - `team` *(string)*: the team that scored the goal
  - shootout goal:
    - `period` *(string)*: `"SO"`
    - `scorer` *(string)*: the goal scorer
    - `team` *(string)*: the team that scored the goal
- `scores` object: each team’s goal count, plus one of these possible fields:
  - `overtime`: set to `true` if the game ended in overtime, absent if it didn’t
  - `shootout`: set to `true` if the game ended in shootout, absent if it didn’t
- `teams` object: away and home team names
- `records` object: each teams’s record for this regular season *before the game*, with the fields:
  - `wins` *(number)*: win count (earning 2 pts)
  - `losses` *(number)*: regulation loss count (0 pts)
  - `ot` *(number)*: loss count for games that went to overtime (1 pt)
- `playoffSeries` object: playoff series related information, only present during playoffs
  - `wins` object: each team’s win count in the series *before the game*

**Note on overtimes:** Only regular season 5 minute overtimes are considered "overtime" in the
`goals` array. Playoff overtime periods are returned as period 4, 5, and so on, since they are
20 minute periods. However, all games (including playoff games) that went into overtime are
marked as having ended in overtime in the `scores` object.

## Requirements

- [Leiningen](http://leiningen.org/) is used for all project management.
- [Docker](https://www.docker.com/) is used for running the application and [Redis](https://hub.docker.com/_/redis/)
  locally. You can also optionally run the application without Docker and Redis.

## Running application

### Using [Docker](https://www.docker.com/)

To run the application locally in [Docker](https://www.docker.com/) containers, install Docker and run:

```
./docker-up.sh
```

Downloading the [Clojure](https://hub.docker.com/_/clojure/) and [Redis](https://hub.docker.com/_/redis/)
images will take quite a while on the first run, but they will be reused after that.

To delete all containers, run:

```
./docker-down.sh
```

### Without Docker

You can also run the application locally with `lein run`.

If you have Redis running somewhere externally, you can specify it with the `REDIS_URL` environment variable:

```
REDIS_URL=redis://localhost lein run
```

You can also run the application without Redis caching:

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
./deploy.sh
```

The latest scores are cached for 5 minutes in [Heroku Redis](https://elements.heroku.com/addons/heroku-redis).

## License

[MIT](LICENSE)

## Acknowledgements

This project is a grateful recipient of the
[Futurice Open Source sponsorship program](http://futurice.com/blog/sponsoring-free-time-open-source-activities?utm_source=github&utm_medium=spice).
