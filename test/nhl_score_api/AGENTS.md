# Test Guide for nhl-score-api

This guide covers test implementation specific details for the nhl-score-api project. See the root [AGENTS.md](../../AGENTS.md) for general project information, including how to run the tests.

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

## Variable Naming

Avoid generic variable names in tests. Use descriptive names that clearly indicate what the variable represents. For example:
- Use `game-details` instead of `data` when working with game information
- Use `away-dressed` instead of `away` when referring to dressed players

Descriptive variable names improve test readability and make it easier to understand what each test is verifying.

## Test Data

- Test resources are in `test/nhl_score_api/fetchers/nhl_api_web/resources/`
- Test data includes:
  - Landing page responses (e.g., `landing-*.json`)
  - Right-rail responses (e.g., `right-rail-*.json`)
  - Roster HTML files (e.g., `roster-*.html`)
  - Schedule responses (e.g., `schedule-*.json`)
  - Standings responses (e.g., `standings-*.json`)

**Important**: Always use accessor functions from `test/nhl_score_api/fetchers/nhl_api_web/resources.clj` for resource access instead of directly accessing files with `slurp` or file paths. This ensures consistency and centralizes resource management. Available accessor functions include:
- `get-gamecenters [game-ids]` - Returns gamecenter data for multiple game IDs
- `get-landing [game-id]` - Returns landing page JSON data
- `get-right-rail [game-id]` - Returns right-rail JSON data
- `get-roster-html [game-id]` - Returns roster HTML content
