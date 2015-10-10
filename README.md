# nhl-score-api

A JSON API that returns the goals from the latest finished NHL games, as reported at http://www.nhl.com/ice/scores.htm.

The API is available at https://nhlscoreapi-peruukki.rhcloud.com/.

## API

### Goals from latest finished NHL games

Returns an array of the latest round’s games, each game item containing the team abbreviations and goal details.

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
        "period": 1,
        "scorer": "David Krejci",
        "team": "BOS",
        "time": "05:36"
      },
      ...
    ],
    "teams": [
      "BOS",
      "CHI"
    ]
  },
  {
    "goals": [
      {
        "goalCount": 1,
        "period": 1,
        "scorer": "Kyle Turris",
        "team": "OTT",
        "time": "00:30"
      },
      ...
    ],
    "teams": [
      "OTT",
      "DET"
    ]
  }
]
```

##### Glossary:

- `goalCount`: the number of goals the player has scored this season
- `period`: in which period the goal was scored; `4` means overtime
- `scorer`: the goal scorer
- `team`: the team that scored the goal
- `time`: the goal scoring time, from the start of the period

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
