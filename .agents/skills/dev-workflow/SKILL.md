---
name: dev-workflow
description: Provides the standard development workflow for `nhl-score-api`, including run/test/format/lint validation and project conventions. Use when implementing or verifying changes in this repository.
---

# Dev Workflow

## Project overview, structure, and endpoints

- Source code is in the `src/` directory
- Tests are in the `test/` directory
- Main namespace structure is `nhl_score_api/`

Key API endpoints:

- `GET /api/scores/latest` – returns scores from the latest finished or on-going NHL games
- `GET /api/scores?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` – returns scores from a given date range (max 7 days)

## Run the app

```bash
lein run
```

## Test

```bash
lein test
lein test --watch
```

Focus a specific test or set of tests via Kaocha `--focus`:

```bash
lein test --focus nhl-score-api.fetchers.nhl-api-web.game-scores-test/game-scores-parsing-scores
```

## Format and lint

```bash
lein format
lein format-check
lein lint
```

## Suggested loop after changes

1. `lein format` (or `lein format-check` if you prefer not to modify files)
2. `lein lint`
3. `lein test`

## Contribution principles

- Prefer concise code; avoid introducing variables that are referenced only once.
- Avoid unnecessary comments that simply restate what the following lines do; add comments only when they provide extra context or explain non-obvious behavior.
- Trim trailing whitespace; ensure blank lines contain no spaces or tabs.
- In markdown documentation (e.g. `README.md`), use fancy/curly quotes for prose, and plain quotes in command examples.
