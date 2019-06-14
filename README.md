# nhl-score-api

[![Build Status](https://travis-ci.org/peruukki/nhl-score-api.svg?branch=master)](https://travis-ci.org/peruukki/nhl-score-api)

A JSON API that returns the scores and goals from the latest finished or on-going NHL games, based on the
[schedule](https://statsapi.web.nhl.com/api/v1/schedule?expand=schedule.teams,schedule.scoringplays,schedule.game.seriesSummary,seriesSummary.series,schedule.linescore)
and [standings](https://statsapi.web.nhl.com/api/v1/standings) information from the NHL Stats API.
The NHL Stats API is undocumented, but [unofficial documentation](https://gitlab.com/dword4/nhlapi) exists.

The API is available at https://nhl-score-api.herokuapp.com/, and it serves as the backend for [nhl-recap](https://github.com/peruukki/nhl-recap).

**NOTE: The API returns the last game of season 2019–20 until the start of next season.**

## API

### Goals from latest finished NHL games

#### `GET` [/api/scores/latest](https://nhl-score-api.herokuapp.com/api/scores/latest)

Returns an object with the date and the scores from the latest round’s games.

The `date` object contains the date in a raw format and a prettier, displayable format.

The `games` array contains details of the games, each game item containing these fields:

- `status` *(object)*
- `startTime` *(string)*
- `goals` *(array)*
- `scores` *(object)*
- `teams` *(object)*
- `preGameStats` *(object)*
- `currentStats` *(object)*
- `errors` *(array)* (only present if data validity errors were detected)

The fields are described in more detail [later in this README](#date-fields-explained).

##### Example regular season scores response:

```json
{
  "date": {
    "raw": "2017-10-16",
    "pretty": "Mon Oct 16"
  },
  "games": [
    {
      "status": {
        "state": "FINAL"
      },
      "startTime": "2016-02-29T00:00:00Z",
      "goals": [
        ...
        {
          "period": "OT",
          "scorer": {
            "player": "David Krejci",
            "seasonTotal": 1
          },
          "assists": [
            {
              "player": "Torey Krug",
              "seasonTotal": 3
            },
            {
              "player": "Zdeno Chara",
              "seasonTotal": 2
            }
          ],
          "team": "BOS",
          "min": 2,
          "sec": 36,
          "strength": "PPG"
        }
      ],
      "scores": {
        "BOS": 4,
        "CHI": 3,
        "overtime": true
      },
      "teams": {
        "away": {
          "abbreviation": "BOS",
          "id": 6
        },
        "home": {
          "abbreviation": "CHI",
          "id": 16
        }
      },
      "preGameStats": {
        "records": {
          "BOS": {
            "wins": 43,
            "losses": 31,
            "ot": 7
          },
          "CHI": {
            "wins": 50,
            "losses": 22,
            "ot": 9
          }
        }
      },
      "currentStats": {
        "records": {
          "BOS": {
            "wins": 44,
            "losses": 31,
            "ot": 7
          },
          "CHI": {
            "wins": 50,
            "losses": 22,
            "ot": 10
          }
        },
        "streaks": {
          "BOS": {
            "count": 2,
            "type": "WINS"
          },
          "CHI": {
            "count": 1,
            "type": "OT"
          }
        },
        "standings": {
          "BOS": {
            "conferenceRank": "4",
            "leagueRank": "8"
          },
          "CHI": {
            "conferenceRank": "11",
            "leagueRank": "25"
          }
        }
      }
    },
    {
      "status": {
        "state": "LIVE",
        "progress": {
          "currentPeriod": 3,
          "currentPeriodOrdinal": "3rd",
          "currentPeriodTimeRemaining": {
            "pretty": "01:58",
            "min": 1,
            "sec": 58
          }
        }
      },
      "startTime": "2016-02-29T02:30:00Z",
      "goals": [
        ...
        {
          "period": "OT",
          "scorer": {
            "player": "Kyle Turris",
            "seasonTotal": 1
          },
          "assists": [
            {
              "player": "Mika Zibanejad",
              "seasonTotal": 3
            }
          ],
          "team": "OTT",
          "min": 17,
          "sec": 30,
          "emptyNet": true
        }
      ],
      "scores": {
        "OTT": 3,
        "DET": 1
      },
      "teams": {
        "away": {
          "abbreviation": "OTT",
          "id": 9
        },
        "home": {
          "abbreviation": "DET",
          "id": 17
        }
      },
      "preGameStats": {
        "records": {
          "OTT": {
            "wins": 43,
            "losses": 28,
            "ot": 10
          },
          "DET": {
            "wins": 33,
            "losses": 36,
            "ot": 12
          }
        }
      },
      "currentStats": {
        "records": {
          "OTT": {
            "wins": 43,
            "losses": 28,
            "ot": 10
          },
          "DET": {
            "wins": 33,
            "losses": 36,
            "ot": 12
          }
        },
        "streaks": {
          "OTT": {
            "count": 1,
            "type": "WINS"
          },
          "DET": {
            "count": 1,
            "type": "LOSSES"
          }
        },
        "standings": {
          "OTT": {
            "conferenceRank": "15",
            "leagueRank": "29"
          },
          "DET": {
            "conferenceRank": "12",
            "leagueRank": "23"
          }
        }
      }
    }
  ]
}
```

##### Example playoff scores response:

```json
{
  "date": {
    "raw": "2017-10-16",
    "pretty": "Mon Oct 16"
  },
  "games": [
    {
      "status": {
        "state": "PREVIEW"
      },
      "startTime": "2016-02-29T02:30:00Z",
      "goals": [],
      "scores": {
        "NYR": 0,
        "PIT": 0
      },
      "teams": {
        "away": {
          "abbreviation": "NYR",
          "id": 3
        },
        "home": {
          "abbreviation": "PIT",
          "id": 5
        }
      },
      "preGameStats": {
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
      },
      "currentStats": {
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
          "round": 0,
          "wins": {
            "NYR": 1,
            "PIT": 1
          }
        }
      }
    }
  ]
}
```

##### Date fields explained:

- `raw` *(string)*: the raw date in "YYYY-MM-DD" format, usable for any kind of processing
- `pretty` *(string)*: a prettified format, can be shown as-is in the client

##### Game fields explained:

- `status` object: current game status, with the fields:
  - `state` *(string)*:
    - `"FINAL"` if the game has ended
    - `"LIVE"` if the game is still in progress
    - `"PREVIEW"` if the game has not started yet
  - `progress` object: game progress, only present if `state` is `"LIVE"`, with the fields:
    - `currentPeriod` *(number)*: current period as a number
    - `currentPeriodOrdinal` *(string)*: current period as a display string (e.g. `"2nd"`)
    - `currentPeriodTimeRemaining` *(object*): time remaining in current period:
      - `pretty` (*string*): time remaining in prettified `mm:ss` format; `"END"` if the current period has ended
      - `min` *(number)*: minutes remaining; `0` if the current period has ended
      - `sec` *(number)*: seconds remaining; `0` if the current period has ended
- `startTime` string: the game start time in standard [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) format
  "YYYY-MM-DDThh:mm:ssZ"
- `goals` array: list of goal details, in the order the goals were scored
  - gameplay goal:
    - `assists` *(array)* of objects with the fields (an empty array for unassisted goals):
      - `player`: the name of the player credited with the assist
      - `seasonTotal` *(number)*: the number of assists the player has had this season
    - `emptyNet` *(boolean)*: set to `true` if the goal was scored in an empty net, absent if it wasn’t
    - `min` *(number)*: the goal scoring time minutes, from the start of the period
    - `period` *(string)*: in which period the goal was scored; `"OT"` means **regular season 5 minute overtime**
    - `scorer` *(object)*:
      - `player`: the name of the goal scorer
      - `seasonTotal` *(number)*: the number of goals the player has scored this season
    - `sec` *(number)*: the goal scoring time seconds, from the start of the period
    - `strength` *(string)*: can be set to `"PPG"` (power play goal) or `"SHG"` (short handed goal); absent
      if the goal was scored on even strength
    - `team` *(string)*: the team that scored the goal
  - shootout goal:
    - `period` *(string)*: `"SO"`
    - `scorer` *(object)*:
      - `player`: the name of the goal scorer
    - `team` *(string)*: the team that scored the goal
- `scores` object: each team’s goal count, plus one of these possible fields:
  - `overtime`: set to `true` if the game ended in overtime, absent if it didn’t
  - `shootout`: set to `true` if the game ended in shootout, absent if it didn’t
- `teams` object:
  - `away` *(object)*: away team info:
    - `abbreviation`: team name abbreviation
    - `id`: team ID in NHL APIs (can be used to fetch other resources from NHL APIs)
  - `home` *(object)*: home team info:
    - `abbreviation`: team name abbreviation
    - `id`: team ID in NHL APIs (can be used to fetch other resources from NHL APIs)
- `preGameStats` object: each teams’s season statistics *before the game*, with the fields:
  - `records` object: each teams’s record for this regular/playoff season, with the fields:
    - `wins` *(number)*: win count (earning 2 pts)
    - `losses` *(number)*: regulation loss count (0 pts)
    - `ot` *(number)*: loss count for games that went to overtime (1 pt)
  - `playoffSeries` object: current playoff series related information, only present during playoffs
    - `round` *(number)*: the game’s playoff round; `0` for the Stanley Cup Qualifiers best-of-5 series, actual playoffs start from `1`
    - `wins` object: each team’s win count in the series
  - `standings` object: each teams’s standings related information, with the field:
    - `conferenceRank` *(string)*: the team's regular season ranking in their conference; this comes as a *string* value from the NHL Stats API
    - `leagueRank` *(string)*: the team's regular season ranking in the league; this comes as a *string* value from the NHL Stats API
- `currentStats` object: each teams’s *current* (ie. after the game if it has finished and NHL have updated their stats) season statistics, with the fields:
  - `records` object: each teams’s record for this regular/playoff season, with the fields:
    - `wins` *(number)*: win count (earning 2 pts)
    - `losses` *(number)*: regulation loss count (0 pts)
    - `ot` *(number)*: loss count for games that went to overtime (1 pt)
  - `streaks` object: each teams’s current (regular season) form streak, with the fields:
    - `type` *(string)*: `"WINS"` (wins in regulation, OT or SO), `"LOSSES"` (losses in regulation) or `"OT"` (losses in OT or SO)
    - `count` *(number)*: streak’s length in consecutive games
  - `standings` object: each teams’s standings related information, with the field:
    - `conferenceRank` *(string)*: the team's regular season ranking in their conference; this comes as a *string* value from the NHL Stats API
    - `leagueRank` *(string)*: the team's regular season ranking in the league; this comes as a *string* value from the NHL Stats API
    - `pointsFromPlayoffSpot` *(string)*: point difference to the last playoff spot in the conference:
      (**NOTE: This field is currently removed due to exceptional playoff spot logic in season 2019–20**)
      - for teams currently in the playoffs, this is the point difference to the first team out of the playoffs;
        i.e. by how many points the team is safe
      - for teams currently outside the playoffs, this is the point difference to the team in the last playoff spot (2nd wildcard
        position); i.e. by how many points (at minimum) the team needs to catch up
      - Note: this value only indicates point differences and doesn’t consider which team is ranked higher if they have the same
        number of points
  - `playoffSeries` object: current playoff series related information, only present during playoffs
    - `round` *(number)*: the game’s playoff round; `0` for the Stanley Cup Qualifiers best-of-5 series, actual playoffs start from `1`
    - `wins` object: each team’s win count in the series
- `errors` array: list of data validation errors, only present if any were detected. Sometimes the NHL Stats API temporarily contains
  invalid or missing data. Currently we check if the goal data from the NHL Stats API (read from its `scoringPlays` field) contains the
  same number of goals than the score data (read from its `teams` field). If it doesn't, two different errors can be reported:
  - `{ "error": "MISSING-ALL-GOALS" }`: all goal data is missing; this has happened occasionally
  - `{ "error": "SCORE-AND-GOAL-COUNT-MISMATCH", "details": { "goalCount": 3, "scoreCount": 4 } }`: goal data exists but doesn't contain
    the same number of goals than the teams' scores; haven't noticed this happen but good to check anyway

**Note on overtimes:** Only regular season 5 minute overtimes are considered "overtime" in the
`goals` array. Playoff overtime periods are returned as period 4, 5, and so on, since they are
20 minute periods. However, all games (including playoff games) that went into overtime are
marked as having ended in overtime in the `scores` object.

## Requirements

- Java version 8
- [Leiningen](http://leiningen.org/) is used for all project management.
- [Docker](https://www.docker.com/) is used for running the application and [Redis](https://hub.docker.com/_/redis/)
  locally. You can also optionally run the application without Docker and Redis.

## Running application

### Using [Docker](https://www.docker.com/)

To run the application locally in [Docker](https://www.docker.com/) containers, install Docker and run:

```sh
./docker-up.sh
```

Downloading the [Clojure](https://hub.docker.com/_/clojure/) and [Redis](https://hub.docker.com/_/redis/)
images will take quite a while on the first run, but they will be reused after that.

To delete all containers, run:

```sh
./docker-down.sh
```

### Without Docker

You can also run the application locally with `lein run`.

If you have Redis running somewhere externally, you can specify it with the `REDIS_URL` environment variable:

```sh
REDIS_URL=redis://localhost lein run
```

You can also run the application without Redis caching:

```sh
REDIS_DISABLED=true lein run
```

To return latest scores from mock NHL Stats API data, you can specify a mock data source file:

```sh
REDIS_DISABLED=true MOCK_NHL_STATS_API=test/nhl_score_api/fetchers/nhlstats/resources/schedule-2018-04-13-live-final-playoff-1st-games.json lein run
```

## Running tests

Run tests with the [Kaocha test runner](https://github.com/lambdaisland/kaocha) for improved test failure reporting:

```sh
lein kaocha [--watch]
```

Run single tests or test groups with [Kaocha's `--focus` argument](https://cljdoc.org/d/lambdaisland/kaocha/0.0-590/doc/focusing-on-specific-tests), e.g.:

```sh
lein kaocha --focus nhl-score-api.fetchers.nhlstats.game-scores-test/game-scores-parsing-scores
```

Or with the regular test runner:

```sh
lein test
```

## Deployment setup

Deploying to [Heroku](http://heroku.com/):

```sh
./deploy.sh
```

The latest scores are cached for one minute in [Heroku Redis](https://elements.heroku.com/addons/heroku-redis).

## License

[MIT](LICENSE)

## Acknowledgements

This project has been a grateful recipient of the
[Futurice Open Source sponsorship program](https://www.futurice.com/blog/sponsoring-free-time-open-source-activities/?utm_source=github&utm_medium=spice).
