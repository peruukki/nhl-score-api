# nhl-score-api

A JSON API that returns the scores and goals from the latest finished NHL games, as reported at
http://www.nhl.com/ice/scores.htm.

The API is available at https://nhlscoreapi-peruukki.rhcloud.com/.

## API

### Goals from latest finished NHL games

##### `GET` [/api/scores/latest](https://nhlscoreapi-peruukki.rhcloud.com/api/scores/latest)

Returns an array of the latest round’s games, each game item containing these three fields:

- `goals` *(array)*
- `scores` *(object)*
- `teams` *(array)*

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
        "min": 0,
        "sec": 30
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
- `teams` array: away team first, home team second

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
