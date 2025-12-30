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

### 3. Create Team Roster API Module (`src/nhl_score_api/fetchers/nhl_api_web/api/roster.clj` - NEW FILE)

**Action**: Create a new API request module for fetching team roster data from NHL Web API.

**Details**:
- Create `RosterApiRequest` record implementing `ApiRequest` protocol
- Endpoint pattern: `/v1/roster/{teamAbbrev}/{season}` or `/v1/roster/{teamAbbrev}` (to be confirmed from NHL Web API Reference)
- Parameters needed:
  - Team abbreviation (e.g., "CGY", "TOR")
  - Season (e.g., "20232024") - optional, may default to current season
- Create schema validation for roster API response
- Response likely contains:
  - Player IDs
  - Player names (full names)
  - Jersey numbers
  - Positions
  - Other player details

**Rationale**: Team roster API provides accurate player information (including player IDs) that can be matched with roster HTML data.

### 4. Create Roster Parser Module (`src/nhl_score_api/fetchers/nhl_api_web/roster_parser.clj` - NEW FILE)

**Action**: Create a new module to fetch and parse roster HTML, then enrich with team roster API data.

**Details**:
- Create `fetch-roster-html` function to fetch HTML from roster URL
- Create `parse-roster-html` function to parse HTML and extract player information
- Use `enlive` (already in project dependencies) for HTML parsing
- Create `enrich-roster-with-api-data` function to match HTML roster data with API roster data
- Matching strategy:
  - Match by jersey number + position (most reliable)
  - Fallback: match by name (normalize both: uppercase, handle special characters)
  - Handle cases where HTML player not found in API data
- Extract roster data structure:
  - Away team roster (players with positions, numbers, names, player IDs)
  - Home team roster (players with positions, numbers, names, player IDs)
  - Goalies (position "G")
  - Skaters (other positions)

**HTML Parsing Strategy**:
- The roster HTML has been inspected (see `roster-2023020207.html` in test resources)
- Structure:
  - Two main sections: `<table id="Visitor">` (away team) and `<table id="Home">` (home team)
  - Each section contains a table with columns: `#` (jersey number), `Pos` (position), `Name`
  - Player rows: `<tr><td>#</td><td>Pos</td><td>Name</td></tr>`
  - Positions: `G` (goalie), `D` (defense), `C` (center), `L` (left wing), `R` (right wing)
  - Names are in UPPERCASE format (e.g., "DUSTIN WOLF", "JACOB MARKSTROM")
  - Some rows have `class="bold"` indicating starting lineup
  - Some names include `(C)` for captain or `(A)` for alternate captain
- Extract:
  - **Jersey number** (from first `<td>`)
  - **Position** (from second `<td>`)
  - **Player name** (from third `<td>`, remove captain/alternate markers)
  - **Team** (away/home based on table ID)
- **Important**: Player IDs are NOT available in the HTML - only jersey numbers, positions, and names
- Matching players by ID would require cross-referencing with other data sources (landing/right-rail)

**Caching**: Cache parsed roster data similar to other API responses to avoid repeated HTML fetching/parsing.

**Rationale**: Centralize roster fetching and parsing logic in a dedicated module.

### 5. Update Fetcher to Support Optional Roster Fetching (`src/nhl_score_api/fetchers/nhl_api_web/fetcher.clj`)

**Action**: Conditionally fetch roster HTML and team roster API data when requested.

**Details**:
- Update `fetch-latest-scores` to accept `include-rosters` parameter
- Update `fetch-scores-in-date-range` to accept `include-rosters` parameter
- When `include-rosters` is true:
  - For each game:
    1. Fetch roster HTML (using roster URL from right-rail data)
    2. Fetch team roster API data for both away and home teams
    3. Parse roster HTML
    4. Enrich parsed roster HTML with team roster API data (match by jersey number/name)
    5. Store enriched roster data alongside gamecenter data
- When `include-rosters` is false:
  - Skip roster fetching entirely (no extra HTTP requests)

**Team Roster Fetching**:
- Extract team abbreviations from game data (away-team and home-team)
- Extract season from game data
- Fetch team roster for each team
- Cache team roster data (roster doesn't change frequently during a season)

**Caching**: Cache parsed roster data to avoid repeated fetching/parsing.

**Rationale**: Only fetch rosters when explicitly requested to avoid unnecessary HTTP requests.

### 6. Add Roster to Game Details (`src/nhl_score_api/fetchers/nhl_api_web/game_scores.clj`)

**Action**: Include roster information in game details when available.

**Details**:
- Update `parse-game-details` function signature to accept optional roster data
- Add `:rosters` field to game details map when roster data is present
- Structure roster data as:
  ```clojure
  {:rosters
   {:away [{:player-id 8482074 :name "Connor Zary" :position "C" :number 47} ...]
    :home [{:player-id 8477939 :name "William Nylander" :position "R" :number 88} ...]}}
  ```
- Include enriched data from team roster API:
  - `player-id`: Player ID from team roster API (when matched)
  - `name`: Full player name (normalized from uppercase HTML, enriched with API data)
  - `position`: Player position (G, D, C, L, R)
  - `number`: Jersey number
- **Matching**: Players from roster HTML are matched with team roster API data by jersey number + position, with name fallback
- If a player from HTML can't be matched with API data, include them with available HTML data only (no player-id)
- Use `reject-empty-vals-except-for-keys` pattern to exclude roster field when not present

**Location in Code Flow**:
- Modify `parse-game-details` function around line 480
- Add roster data after parsing team details
- Update `parse-game-scores` to pass roster data to `parse-game-details`

**Rationale**: Include roster information at the game level when requested.

### 7. Update Tests

#### 7a. Team Roster API Tests (`test/nhl_score_api/fetchers/nhl_api_web/api/roster_test.clj` - NEW FILE)

**Action**: Add tests for team roster API requests.

**Details**:
- Test roster API request creation
- Test URL generation
- Test cache key generation
- Test schema validation
- Test response parsing

**Test Data**: 
- May need to fetch sample team roster API responses
- Or create mock responses based on API structure

#### 7b. Roster Parser Tests (`test/nhl_score_api/fetchers/nhl_api_web/roster_parser_test.clj` - NEW FILE)

**Action**: Add tests for roster HTML parsing and enrichment.

**Details**:
- Test parsing roster HTML structure
- Test extracting away team roster
- Test extracting home team roster
- Test extracting player information (name, position, number)
- Test matching HTML roster data with API roster data
- Test enrichment with player IDs
- Test handling of unmatched players
- Test handling of malformed or missing roster HTML
- Test handling of network errors

**Test Data**: 
- Sample roster HTML file saved: `roster-2023020207.html` (game ID 2023020207)
- Need sample team roster API responses for matching tests
- Can fetch additional roster HTML files for other test games if needed

#### 7c. Param Parser Tests (`test/nhl_score_api/param_parser_test.clj`)

**Action**: Add tests for `include` parameter parsing.

**Details**:
- Test parsing single value: `include=rosters`
- Test parsing multiple values: `include=rosters,otherThing`
- Test default behavior (missing parameter)
- Test empty value handling
- Test whitespace handling in comma-separated list

#### 7d. Game Scores Tests (`test/nhl_score_api/fetchers/nhl_api_web/game_scores_test.clj`)

**Action**: Add tests for roster inclusion in game details.

**Details**:
- Test roster data included when provided
- Test roster data excluded when not provided
- Test roster structure matches expected format
- Test with multiple games

#### 7e. Core Tests (`test/nhl_score_api/core_test.clj`)

**Action**: Add tests for query parameter handling.

**Details**:
- Test `include=rosters` query parameter
- Test `include=rosters,otherThing` (should still include rosters)
- Test missing parameter (no inclusions)
- Test empty `include` parameter
- Test case-insensitive parsing (e.g., `include=ROSTERS`)

## Implementation Order

1. **Step 1**: Add string parameter parsing for `include` (param_parser.clj)
2. **Step 2**: Update request handler to parse and pass parameter (core.clj)
3. **Step 3**: Create team roster API module (api/roster.clj - NEW)
   - Determine exact endpoint from NHL Web API Reference documentation
   - Implement ApiRequest protocol
   - Create response schema validation
   - Test endpoint URL and response structure
4. **Step 4**: Create roster parser module (roster_parser.clj - NEW)
   - Implement HTML parsing with enlive (structure already understood)
   - Extract roster data from HTML
   - Implement matching logic (jersey number + position, name fallback)
   - Implement enrichment with team roster API data
5. **Step 5**: Update fetcher to conditionally fetch rosters (fetcher.clj)
   - Add include-rosters parameter to fetch functions
   - Fetch team roster API data for both teams
   - Conditionally fetch and parse roster HTML
   - Enrich roster HTML with API data
   - Cache parsed roster data (both HTML and API)
6. **Step 6**: Add roster to game details (game_scores.clj)
   - Update parse-game-details to accept roster data
   - Include enriched roster in response when present
7. **Step 7**: Add tests
   - Team roster API tests
   - Roster parser tests (parsing + enrichment)
   - Param parser tests
   - Game scores tests
   - Core handler tests

## Edge Cases to Consider

1. **Missing roster link**:
   - Roster link may not be available for all games
   - Handle gracefully by not including roster data for that game
   - Don't fail the entire request

3. **Missing team roster API data**:
   - Team roster API may fail or be unavailable
   - Fallback: include roster HTML data without enrichment (no player IDs)
   - Don't fail the entire request if team roster fetch fails

4. **Player matching failures**:
   - Some players in HTML may not match API roster (trades, call-ups, etc.)
   - Include unmatched players with HTML data only (no player-id)
   - Log warnings for unmatched players for debugging

5. **Roster HTML parsing failures**:
   - Malformed HTML
   - Network errors when fetching roster
   - Handle gracefully - log error but don't fail request
   - Don't include roster data for that game

6. **Roster HTML structure changes**:
   - NHL may change HTML structure
   - Parsing may fail silently
   - Consider adding validation/logging

7. **Performance**:
   - Fetching roster HTML adds HTTP request per game
   - Fetching team roster API adds 2 requests per game (away + home teams)
   - Only fetch when requested (query parameter)
   - Cache parsed results to avoid repeated fetching/parsing
   - Team roster API data can be cached longer (roster doesn't change frequently)

7. **Empty roster data**:
   - Use `reject-empty-vals-except-for-keys` to exclude empty roster fields
   - Similar pattern to how `goals` and `links` are handled

9. **Query parameter values**:
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
        "playerId": 8482074,
        "name": "Connor Zary",
        "position": "C",
        "number": 47
      },
      {
        "playerId": 8478397,
        "name": "Rasmus Andersson",
        "position": "D",
        "number": 4
      },
      {
        "playerId": 8481540,
        "name": "Dustin Wolf",
        "position": "G",
        "number": 32
      },
      ...
    ],
    "home": [
      {
        "playerId": 8477939,
        "name": "William Nylander",
        "position": "R",
        "number": 88
      },
      {
        "playerId": 8481540,
        "name": "Ilya Samsonov",
        "position": "G",
        "number": 35
      },
      ...
    ]
  }
}
```

**Note**: Player IDs are included by matching roster HTML data with team roster API data. If a player can't be matched, they'll be included without a `playerId` field.

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
- Roster fetching adds multiple HTTP requests per game when requested:
  - 1 request for roster HTML
  - 2 requests for team roster API (away team + home team)
- Team roster API data is cached (roster doesn't change frequently during a season)
- Project already includes `enlive` HTML parsing library (no new dependency needed)
- Roster data (both HTML and API) is cached to avoid repeated fetching/parsing
- Query parameter is optional - existing API calls continue to work without changes
- `include` parameter supports comma-separated values for future extensibility (e.g., `include=rosters,otherThing`)
- Roster HTML structure has been analyzed (see `roster-2023020207.html` in test resources)
- Team roster API endpoint needs to be confirmed from NHL Web API Reference documentation
- Player matching uses jersey number + position as primary key, with name as fallback

## Next Steps

1. ✅ Fetch a sample roster HTML file to understand the structure (`roster-2023020207.html` saved)
2. ✅ Determine the exact HTML structure and parsing strategy (documented above)
3. **Determine team roster API endpoint** from NHL Web API Reference documentation
   - Confirm endpoint URL pattern (e.g., `/v1/roster/{teamAbbrev}/{season}`)
   - Understand response structure
   - Fetch sample response to create schema
4. Implement team roster API module (`api/roster.clj`)
5. Implement roster parser module (`roster_parser.clj`)
   - HTML parsing with `enlive`
   - Matching logic (jersey number + position, name fallback)
   - Enrichment with API data
6. Integrate into fetcher
7. Test with real roster data
