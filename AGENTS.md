# Agent Guide for nhl-score-api

This guide provides essential information for AI agents working on this Clojure project.

## Project Overview

This is a Clojure project that provides a JSON API returning scores and goals from NHL games. The data is sourced from the NHL Web API at https://api-web.nhle.com.

- **Dependency Management**: Leiningen
- **Test Runner**: Kaocha (via Leiningen)
- **Language**: Clojure

## Running the Application

```bash
lein run
```

## Testing

Running tests:

```bash
# Run all tests
lein test

# Run tests in watch mode
lein test --watch

# Run a specific test or test group using Kaocha's --focus
# Example: single test function
lein test --focus nhl-score-api.fetchers.nhl-api-web.game-scores-test/game-scores-parsing-scores

# Focus on a namespace
lein test --focus nhl-score-api.fetchers.nhl-api-web.fetcher-test
```

For test implementation details, see [test/nhl_score_api/AGENTS.md](test/nhl_score_api/AGENTS.md).

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

- Source code is in `src/` directory
- Main namespace structure: `nhl_score_api/`

For test structure and test data locations, see [test/nhl_score_api/AGENTS.md](test/nhl_score_api/AGENTS.md).

## Key API Endpoints

The application provides these main endpoints:

- `GET /api/scores/latest` - Returns scores from the latest finished or on-going NHL games
- `GET /api/scores?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` - Returns scores from a given date range (max 7 days)

## Notes for AI Agents

- Always run tests after making changes: `lein test`
- Format code before committing: `lein format`
- Check linting: `lein lint`
- **Prefer concise code**: Do not add variables that are referenced only once, inline values instead.
- **Avoid unnecessary comments**: Do not add comments that simply restate what the following lines do. Only add comments that provide more context or explain non-obvious behavior.
- **Use fancy quotes in documentation**: When editing documentation in markdown files (like `README.md`), use fancy/curly quotes (`'` and `'`) instead of plain quotes (`'` and `"`). This applies to apostrophes in contractions (e.g., `don't`, `it's`) and possessive forms (e.g., `team's`, `player's`). But use plain quotes `'` in places that require them, like command examples.
- **Trim trailing whitespace**: When adding empty lines, ensure they contain no whitespace characters. Remove any spaces or tabs from otherwise empty lines.

## Code Organization: Alphabetical Sorting

**IMPORTANT: Always sort items alphabetically when possible.** This is a critical code organization principle for this project. Apply alphabetical sorting to:

- **Dependencies** in `project.clj`
- **Fields** in Malli schema definitions
- **Imports** in `:require` clauses
- **Protocol methods** in protocol definitions and implementations
- **Record methods** in record definitions
- **Private variable definitions**
- **Any lists, maps, or collections** where order doesn't affect functionality

Alphabetical sorting improves code readability, makes items easier to find, and reduces merge conflicts. When in doubt, sort alphabetically.
