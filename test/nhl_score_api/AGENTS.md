# Test Guide for nhl-score-api

This guide covers test-specific details for the nhl-score-api project. See the root [AGENTS.md](../../AGENTS.md) for general project information.

## Running Tests

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

## Test Structure

- Tests are located in the `test/` directory
- Test files follow the pattern `*_test.clj`
- Test namespaces end with `-test` suffix
- Uses `clojure.test` for testing framework
- Project uses Kaocha as the test runner

## Test Organization

Tests are organized to match the source structure:
- `test/nhl_score_api/fetchers/nhl_api_web/fetcher_test.clj` tests `src/nhl_score_api/fetchers/nhl_api_web/fetcher.clj`
- Each test file contains multiple test functions using `deftest`
- Test assertions use `is` from `clojure.test`

## Test Data

- Test resources are in `test/nhl_score_api/fetchers/nhl_api_web/resources/`
- Test data includes:
  - Landing page responses (e.g., `landing-*.json`)
  - Right-rail responses (e.g., `right-rail-*.json`)
  - Schedule responses (e.g., `schedule-*.json`)
  - Standings responses (e.g., `standings-*.json`)
