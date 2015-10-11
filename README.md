# nhl-score-api

A JSON API that returns the scores and goals from the latest finished NHL games, as reported at
http://www.nhl.com/ice/scores.htm.

The API is available at https://nhlscoreapi-peruukki.rhcloud.com/.

## API

### Goals from latest finished NHL games

Returns an array of the latest round’s games, each game item containing information on team scores and goals.

```
GET /api/scores/latest
```

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
        "time": "05:36"
      },
      ...
    ],
    "scores": {
      "BOS": 4,
      "CHI": 3
    },
    "teams": [
      "BOS",
      "CHI"
    ]
  },
  {
    "goals": [
      ...
      {
        "goalCount": 1,
        "period": "OT",
        "scorer": "Kyle Turris",
        "team": "OTT",
        "time": "00:30"
      }
    ],
    "scores": {
      "OTT": 2,
      "DET": 1,
      "overtime": true
    },
    "teams": [
      "OTT",
      "DET"
    ]
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
    "teams": [
      "NYR",
      "PIT"
    ]
  }
]
```

##### Glossary:

- `goals` array:
  - gameplay goal:
    - `goalCount`: the number of goals the player has scored this season
    - `period`: in which period the goal was scored (string); `"OT"` means overtime
    - `scorer`: the goal scorer
    - `team`: the team that scored the goal
    - `time`: the goal scoring time, from the start of the period
  - shootout goal:
    - `period`: `"SO"`
    - `scorer`: the goal scorer
    - `team`: the team that scored the goal
- `scores` object:
  - `overtime`: set to `true` if the game ended in overtime, absent if it didn’t
  - `shootout`: set to `true` if the game ended in shootout, absent if it didn’t

## Requirements

- [Leiningen](http://leiningen.org/) is used for all project management.

## Running server

`lein run`

## Running tests

`lein test`

## OpenShift setup

This application is running on [OpenShift](https://www.openshift.com), created using
[clojure-cartridge](https://github.com/openshift-cartridges/clojure-cartridge).

## License

[MIT](LICENSE)
