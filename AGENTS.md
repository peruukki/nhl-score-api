# Agent Guide for nhl-score-api

This guide provides essential information for AI agents working on this Clojure project.

## Project Overview

This is a Clojure project that provides a JSON API returning scores and goals from NHL games. The data is sourced from the NHL Web API at https://api-web.nhle.com.

- **Dependency Management**: Leiningen
- **Test Runner**: Kaocha (via Leiningen)
- **Language**: Clojure
- **Java Version**: 8

## Running the Application

### Using Docker

```bash
./docker-up.sh
```

To stop and delete containers:

```bash
./docker-down.sh
```

### Without Docker

```bash
lein run
```

## Testing

### Run all tests

```bash
lein test
```

### Run tests with watch mode

```bash
lein test --watch
```

### Run a specific test file or test group

Use Kaocha's `--focus` argument:

```bash
lein test --focus nhl-score-api.fetchers.nhlstats.game-scores-test/game-scores-parsing-scores
```

Or focus on a namespace:

```bash
lein test --focus nhl-score-api.fetchers.nhl-api-web.fetcher-test
```

### Test Structure

- Tests are located in the `test/` directory
- Test files follow the pattern `*_test.clj`
- Test namespaces end with `-test` suffix
- Uses `clojure.test` for testing framework
- Project uses Kaocha as the test runner

### Test Organization

Tests are organized to match the source structure:
- `test/nhl_score_api/fetchers/nhl_api_web/fetcher_test.clj` tests `src/nhl_score_api/fetchers/nhl_api_web/fetcher.clj`
- Each test file contains multiple test functions using `deftest`
- Test assertions use `is` from `clojure.test`

## Code Formatting

Formatting is done with [cljfmt](https://github.com/weavejester/cljfmt).

Format the code automatically:

```bash
lein format
```

Only check the formatting without making changes:

```bash
lein format-check
```

## Linting

Lint the code with the [clj-kondo Leiningen plugin](https://github.com/clj-kondo/lein-clj-kondo):

```bash
lein lint
```

## Project Structure

### Source Code
- Source code is in `src/` directory
- Main namespace structure: `nhl_score_api/`

### Test Data
- Test resources are in `test/nhl_score_api/fetchers/nhl_api_web/resources/`
- Test data includes:
  - Schedule responses (e.g., `schedule-*.json`)
  - Landing page responses (e.g., `landing-*.json`)
  - Right-rail responses (e.g., `right-rail-*.json`)

## Key API Endpoints

The application provides these main endpoints:

- `GET /api/scores/latest` - Returns scores from the latest finished or on-going NHL games
- `GET /api/scores?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` - Returns scores from a given date range (max 7 days)

## NHL Web API Usage

The project uses these NHL Web API endpoints:

- `https://api-web.nhle.com/v1/schedule/{date}` - List of week's games
- `https://api-web.nhle.com/v1/gamecenter/{gameId}/landing` - Basic game details
- `https://api-web.nhle.com/v1/gamecenter/{gameId}/right-rail` - Game stats and recap video links
- `https://api-web.nhle.com/v1/standings/{date}` - Team stats

API responses are cached in-memory for one minute, then refreshed upon the next request.

## Notes for AI Agents

- Always run tests after making changes: `lein test`
- Format code before committing: `lein format`
- Check linting: `lein lint`
- Test files use the `-test` suffix in namespace names
- The project uses Kaocha for improved test failure reporting
