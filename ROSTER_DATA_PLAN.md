# Plan: Adding Optional Roster Information to API Responses

## Overview
The NHL Web API right-rail endpoint includes a link to an HTML roster report for finished or live games. This plan outlines adding optional roster information to API responses, controlled by a query parameter. Since fetching and parsing HTML requires an extra HTTP request, it will only be included when explicitly requested.

## Current State

### Data Source
- **Roster Link**: Available in right-rail data as `(:rosters gamecenter-data)`
- **Format**: HTML file (not JSON)
- **URL Pattern**: `https://www.nhl.com/scores/htmlreports/{season}/{RO{gameId}.HTM}`
- **Example**: `https://www.nhl.com/scores/htmlreports/20232024/RO020207.HTM`

### Current Response Structure
The API currently returns game data with:
- `status`: game state and progress
- `startTime`: game start time
- `goals`: array of goal details
- `links`: game-related links
- `scores`: team scores
- `teams`: team information
- `gameStats`: game statistics
- `preGameStats`: pre-game statistics (records, streaks, standings, playoff series)
- `currentStats`: current statistics (records, streaks, standings, playoff series)

### Current Query Parameters
- `/api/scores/latest`: No parameters
- `/api/scores?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`: Date range parameters

## Implementation Plan

### 1. Add Query Parameter Parsing (`src/nhl_score_api/param_parser.clj`)

**Action**: Add support for parsing a string query parameter `include` that can contain comma-separated values.

**Details**:
- Add a new parse function for string parameters: `parse-fn-string` (or use existing string handling)
- Parse `include` parameter as a string
- Split by comma to support multiple inclusions (e.g., `include=rosters,otherThing`)
- Check if "rosters" is in the list of inclusions
- Default to empty list if parameter not provided

**Rationale**: Use flexible `include` parameter to support future additions (e.g., `include=rosters,stats`). For now, check if "rosters" is in the inclusion list.

### 2. Update Core Request Handler (`src/nhl_score_api/core.clj`)

**Action**: Parse `include` query parameter and determine if rosters should be included.

**Details**:
- Add `:include` to expected parameters for both `/api/scores/latest` and `/api/scores` endpoints
- Parse the string parameter (comma-separated list)
- Check if "rosters" is in the inclusion list
- Pass `include-rosters` boolean flag to `fetch-latest-scores` and `fetch-scores-in-date-range` functions
- Update function signatures to accept the new parameter

**Rationale**: Request handler needs to extract the `include` parameter, check for "rosters", and pass a boolean flag to the fetch layer. This design supports future additions (e.g., `include=rosters,otherThing`).

### 3. Create Roster Parser Module (`src/nhl_score_api/fetchers/nhl_api_web/roster_parser.clj` - NEW FILE)

**Action**: Create a new module to fetch and parse roster HTML.

**Details**:
- Create `fetch-roster-html` function to fetch HTML from roster URL
- Create `parse-roster-html` function to parse HTML and extract player information
- Use `enlive` (already in project dependencies) for HTML parsing
- Extract roster data structure:
  - Away team roster (players with positions, numbers, names)
  - Home team roster (players with positions, numbers, names)
  - Goalies (position "G")
  - Skaters (other positions)

**HTML Parsing Strategy**:
- The roster HTML is likely a structured table
- Need to identify the HTML structure (inspect actual roster HTML)
- Extract:
  - Player ID (if available in HTML)
  - Player name (full name)
  - Position
  - Jersey number
  - Team (away/home)

**Caching**: Cache parsed roster data similar to other API responses to avoid repeated HTML fetching/parsing.

**Rationale**: Centralize roster fetching and parsing logic in a dedicated module.

### 4. Update Fetcher to Support Optional Roster Fetching (`src/nhl_score_api/fetchers/nhl_api_web/fetcher.clj`)

**Action**: Conditionally fetch and parse roster HTML when requested.

**Details**:
- Update `fetch-latest-scores` to accept `include-rosters` parameter
- Update `fetch-scores-in-date-range` to accept `include-rosters` parameter
- When `include-rosters` is true:
  - Fetch roster HTML for each game (using roster URL from right-rail data)
  - Parse roster HTML
  - Store parsed roster data alongside gamecenter data
- When `include-rosters` is false:
  - Skip roster fetching entirely (no extra HTTP requests)

**Caching**: Cache parsed roster data to avoid repeated fetching/parsing.

**Rationale**: Only fetch rosters when explicitly requested to avoid unnecessary HTTP requests.

### 5. Add Roster to Game Details (`src/nhl_score_api/fetchers/nhl_api_web/game_scores.clj`)

**Action**: Include roster information in game details when available.

**Details**:
- Update `parse-game-details` function signature to accept optional roster data
- Add `:rosters` field to game details map when roster data is present
- Structure roster data as:
  ```clojure
  {:rosters
   {:away [{:player-id <id> :name "<full-name>" :position "G" :number 79} ...]
    :home [{:player-id <id> :name "<full-name>" :position "C" :number 13} ...]}}
  ```
- Use `reject-empty-vals-except-for-keys` pattern to exclude roster field when not present

**Location in Code Flow**:
- Modify `parse-game-details` function around line 480
- Add roster data after parsing team details
- Update `parse-game-scores` to pass roster data to `parse-game-details`

**Rationale**: Include roster information at the game level when requested.

### 6. Update Tests

#### 6a. Roster Parser Tests (`test/nhl_score_api/fetchers/nhl_api_web/roster_parser_test.clj` - NEW FILE)

**Action**: Add tests for roster HTML parsing.

**Details**:
- Test parsing roster HTML structure
- Test extracting away team roster
- Test extracting home team roster
- Test extracting player information (name, position, number, ID)
- Test handling of malformed or missing roster HTML
- Test handling of network errors

**Test Data**: 
- Need to fetch/create sample roster HTML files
- Or use actual roster HTML from test games

#### 6b. Param Parser Tests (`test/nhl_score_api/param_parser_test.clj`)

**Action**: Add tests for `include` parameter parsing.

**Details**:
- Test parsing single value: `include=rosters`
- Test parsing multiple values: `include=rosters,otherThing`
- Test default behavior (missing parameter)
- Test empty value handling
- Test whitespace handling in comma-separated list

#### 6c. Game Scores Tests (`test/nhl_score_api/fetchers/nhl_api_web/game_scores_test.clj`)

**Action**: Add tests for roster inclusion in game details.

**Details**:
- Test roster data included when provided
- Test roster data excluded when not provided
- Test roster structure matches expected format
- Test with multiple games

#### 6d. Core Tests (`test/nhl_score_api/core_test.clj`)

**Action**: Add tests for query parameter handling.

**Details**:
- Test `include=rosters` query parameter
- Test `include=rosters,otherThing` (should still include rosters)
- Test missing parameter (no inclusions)
- Test empty `include` parameter
- Test case-insensitive parsing (e.g., `include=ROSTERS`)

## Implementation Order

1. **Step 1**: Add boolean parameter parsing (param_parser.clj)
2. **Step 2**: Update request handler to parse and pass parameter (core.clj)
3. **Step 3**: Create roster parser module (roster_parser.clj - NEW)
   - Fetch actual roster HTML to understand structure
   - Implement HTML parsing with enlive
   - Extract roster data structure
4. **Step 4**: Update fetcher to conditionally fetch rosters (fetcher.clj)
   - Add include-rosters parameter to fetch functions
   - Conditionally fetch and parse roster HTML
   - Cache parsed roster data
5. **Step 5**: Add roster to game details (game_scores.clj)
   - Update parse-game-details to accept roster data
   - Include roster in response when present
6. **Step 6**: Add tests
   - Roster parser tests
   - Param parser tests
   - Game scores tests
   - Core handler tests

## Edge Cases to Consider

1. **Missing roster link**:
   - Roster link may not be available for all games
   - Handle gracefully by not including roster data for that game
   - Don't fail the entire request

2. **Roster HTML parsing failures**:
   - Malformed HTML
   - Network errors when fetching roster
   - Handle gracefully - log error but don't fail request
   - Don't include roster data for that game

3. **Roster HTML structure changes**:
   - NHL may change HTML structure
   - Parsing may fail silently
   - Consider adding validation/logging

4. **Performance**:
   - Fetching roster HTML adds HTTP request per game
   - Only fetch when requested (query parameter)
   - Cache parsed results to avoid repeated parsing

5. **Empty roster data**:
   - Use `reject-empty-vals-except-for-keys` to exclude empty roster fields
   - Similar pattern to how `goals` and `links` are handled

6. **Query parameter values**:
   - Handle single value: `include=rosters`
   - Handle multiple values: `include=rosters,otherThing` (for future use)
   - Case-insensitive parsing for inclusion names
   - Trim whitespace around comma-separated values
   - Empty or missing parameter means no inclusions

## API Response Format

### Before
```json
{
  "status": {...},
  "startTime": "...",
  "goals": [...],
  "scores": {...},
  "teams": {...}
}
```

### After (with `?include=rosters`)
```json
{
  "status": {...},
  "startTime": "...",
  "goals": [...],
  "scores": {...},
  "teams": {...},
  "rosters": {
    "away": [
      {
        "playerId": 8479292,
        "name": "Charlie Lindgren",
        "position": "G",
        "number": 79
      },
      ...
    ],
    "home": [
      {
        "playerId": 8480018,
        "name": "Nick Suzuki",
        "position": "C",
        "number": 14
      },
      ...
    ]
  }
}
```

**Note**: `rosters` field will only be present when `include=rosters` query parameter is provided. It will be omitted otherwise.

## API Endpoint Changes

### `/api/scores/latest`
- **New Query Parameter**: `include` (string, optional, comma-separated list)
- **Example**: `/api/scores/latest?include=rosters`
- **Future Example**: `/api/scores/latest?include=rosters,otherThing`

### `/api/scores`
- **New Query Parameter**: `include` (string, optional, comma-separated list)
- **Example**: `/api/scores?startDate=2023-11-08&endDate=2023-11-09&include=rosters`
- **Future Example**: `/api/scores?startDate=2023-11-08&include=rosters,otherThing`

## Testing Strategy

1. **Unit Tests**: 
   - Roster parser in isolation
   - Parameter parsing
   - HTML parsing edge cases

2. **Integration Tests**: 
   - Full request flow with `include=rosters`
   - Full request flow without `include` parameter
   - Multiple games with rosters
   - Future: test with `include=rosters,otherThing` (when other inclusions are added)

3. **Edge Case Tests**: 
   - Missing roster links
   - Parsing failures
   - Invalid query parameters

4. **Performance Tests**:
   - Verify rosters only fetched when `include=rosters` is present
   - Verify no extra requests when parameter is missing
   - Verify caching works correctly

## Notes

- Roster HTML is only available for finished or live games
- Roster fetching adds an additional HTTP request per game (only when requested)
- Project already includes `enlive` HTML parsing library (no new dependency needed)
- Roster data is cached to avoid repeated fetching/parsing
- Query parameter is optional - existing API calls continue to work without changes
- `include` parameter supports comma-separated values for future extensibility (e.g., `include=rosters,otherThing`)
- Roster structure needs to be determined by inspecting actual HTML files

## Next Steps

1. Fetch a sample roster HTML file to understand the structure
2. Determine the exact HTML structure and parsing strategy
3. Implement roster parser based on actual HTML format
4. Test with real roster data
