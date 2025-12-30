# Plan: Adding Optional Roster Information to API Responses

## Overview
The NHL Web API right-rail endpoint includes a link to an HTML roster report for finished or live games. This plan outlines adding optional roster information to API responses, controlled by a query parameter. Since fetching and parsing HTML requires an extra HTTP request, it will only be included when explicitly requested.

## Implementation Strategy: Phased Approach

This implementation is divided into **8 incremental phases** that can be fully implemented, tested, and released independently:

1. **Phase 1**: Query Parameter Infrastructure (foundational, no external changes)
2. **Phase 2**: Team Roster API Module (standalone module)
3. **Phase 3**: Roster HTML Parser Module (standalone module)
4. **Phase 4**: Player Matching & Enrichment Logic (uses phases 2 & 3)
5. **Phase 5**: Fetcher Integration - Team Roster API (internal changes)
6. **Phase 6**: Fetcher Integration - Roster HTML (internal changes)
7. **Phase 7**: API Response Integration (**external API change**)
8. **Phase 8**: End-to-End Testing & Documentation

**Key Benefits**:
- Phases 1-6 can be released without external API changes
- Each phase is independently testable and releasable
- Smaller, focused changes are easier to review and debug
- Can stop at any phase if needed

See [Implementation Phases](#implementation-phases) section for detailed breakdown.

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
- **Endpoint**: `/v1/roster/{teamAbbrev}/{season}` (confirmed)
  - Example: `https://api-web.nhle.com/v1/roster/CGY/20232024`
- Parameters needed:
  - Team abbreviation (e.g., "CGY", "TOR")
  - Season (e.g., "20232024") - required
- Create schema validation for roster API response

**Response Structure** (confirmed from sample responses):
```json
{
  "forwards": [
    {
      "id": 8474150,
      "firstName": {"default": "Mikael"},
      "lastName": {"default": "Backlund"},
      "sweaterNumber": 11,
      "positionCode": "C",
      ...
    }
  ],
  "defensemen": [...],
  "goalies": [...]
}
```

**Key Fields**:
- `id`: Player ID (integer) - **critical for matching**
- `firstName`: Localized object with `default` field
- `lastName`: Localized object with `default` field
- `sweaterNumber`: Jersey number (integer) - **critical for matching**
- `positionCode`: Position code ("C", "D", "G", "L", "R") - **critical for matching**

**Schema**:
- Response contains three arrays: `forwards`, `defensemen`, `goalies`
- Each player has: `id`, `firstName`, `lastName`, `sweaterNumber`, `positionCode`
- Position codes match roster HTML: C, D, G, L, R

**Rationale**: Team roster API provides accurate player information (including player IDs) that can be matched with roster HTML data using jersey number + position code.

### 4. Create Roster Parser Module (`src/nhl_score_api/fetchers/nhl_api_web/roster_parser.clj` - NEW FILE)

**Action**: Create a new module to fetch and parse roster HTML, then enrich with team roster API data.

**Details**:
- Create `fetch-roster-html` function to fetch HTML from roster URL
- Create `parse-roster-html` function to parse HTML and extract player information
- Use `enlive` (already in project dependencies) for HTML parsing
- Create `enrich-roster-with-api-data` function to match HTML roster data with API roster data
- **Matching Strategy**:
  - **Primary**: Match by `sweaterNumber` (jersey number) + `positionCode` (position)
    - Roster HTML: jersey number + position (G, D, C, L, R)
    - API: `sweaterNumber` + `positionCode` (G, D, C, L, R)
    - Position codes match exactly between HTML and API
  - **Fallback**: Match by name (normalize both: uppercase HTML to title case, handle special characters)
    - HTML names are UPPERCASE (e.g., "DUSTIN WOLF")
    - API names are title case (e.g., "Dustin Wolf")
    - Normalize HTML name to title case for comparison
  - Handle cases where HTML player not found in API data (include with HTML data only)
- **API Data Structure**:
  - API has three arrays: `forwards`, `defensemen`, `goalies`
  - Need to search across all three arrays for matching
  - Combine all players into single list for matching
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
- Test URL generation (e.g., `/v1/roster/CGY/20232024`)
- Test cache key generation
- Test schema validation
- Test response parsing
- Test with sample data: `roster-api-CGY-20232024.json`

**Test Data**: 
- ✅ Sample team roster API responses saved:
  - `roster-api-CGY-20232024.json` (Calgary Flames)
  - `roster-api-TOR-20232024.json` (Toronto Maple Leafs)

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
- ✅ Sample roster HTML file saved: `roster-2023020207.html` (game ID 2023020207)
- ✅ Sample team roster API responses saved:
  - `roster-api-CGY-20232024.json` (away team for game 2023020207)
  - `roster-api-TOR-20232024.json` (home team for game 2023020207)
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

## Implementation Phases

The implementation is divided into incremental phases that can be fully implemented, tested, and released independently. Each phase builds on previous phases.

### Phase 1: Query Parameter Infrastructure
**Goal**: Add support for parsing `include` query parameter (foundational, no external API changes)

**Tasks**:
- Add string parameter parsing to `param_parser.clj`
- Support comma-separated values (e.g., `include=rosters,otherThing`)
- Add tests for parameter parsing

**Deliverables**:
- ✅ Parameter parsing functionality
- ✅ Tests passing
- ✅ No external API changes (parameter not yet used)

**Release**: Can be released immediately (backward compatible, unused feature)

---

### Phase 2: Team Roster API Module
**Goal**: Create standalone module for fetching team roster data from NHL Web API

**Tasks**:
- Create `api/roster.clj` with `RosterApiRequest` record
- Implement `ApiRequest` protocol (url, cache-key, description, response-schema, archive?)
- Create schema validation for roster API response
- Add tests using sample data (`roster-api-CGY-20232024.json`)

**Deliverables**:
- ✅ `RosterApiRequest` module
- ✅ Schema validation
- ✅ Tests passing
- ✅ Can fetch and validate team roster data

**Release**: Can be released immediately (new internal module, not yet integrated)

---

### Phase 3: Roster HTML Parser Module
**Goal**: Create standalone module for parsing roster HTML files

**Tasks**:
- Create `roster_parser.clj` module
- Implement HTML parsing with `enlive`
- Extract roster data from HTML (jersey number, position, name)
- Parse away and home team rosters
- Normalize names (handle UPPERCASE, remove captain/alternate markers)
- Add tests using sample HTML (`roster-2023020207.html`)

**Deliverables**:
- ✅ HTML parsing functionality
- ✅ Extracted roster data structure
- ✅ Tests passing
- ✅ Can parse roster HTML independently

**Release**: Can be released immediately (new internal module, not yet integrated)

---

### Phase 4: Player Matching and Enrichment Logic
**Goal**: Create logic to match HTML roster players with API roster data

**Tasks**:
- Add matching functions to `roster_parser.clj`
- Implement primary matching: `sweaterNumber` + `positionCode`
- Implement fallback matching: name normalization and comparison
- Combine API arrays (forwards, defensemen, goalies) for matching
- Enrich HTML roster data with player IDs from API
- Handle unmatched players gracefully
- Add tests for matching logic

**Deliverables**:
- ✅ Matching logic
- ✅ Enrichment functionality
- ✅ Tests passing (use both HTML and API sample data)
- ✅ Can match and enrich roster data

**Release**: Can be released immediately (new functionality in existing module)

---

### Phase 5: Fetcher Integration - Team Roster API
**Goal**: Integrate team roster API fetching into the fetcher (no HTML parsing yet)

**Tasks**:
- Update `fetcher.clj` to accept `include-rosters` parameter
- Extract team abbreviations and season from game data
- Fetch team roster API data for both teams when `include-rosters=true`
- Cache team roster API responses
- Pass team roster data to game parsing (not yet used in response)
- Add tests for fetching logic

**Deliverables**:
- ✅ Team roster API fetching integrated
- ✅ Caching working
- ✅ Tests passing
- ✅ No external API changes (data fetched but not yet in response)

**Release**: Can be released immediately (internal changes, no external impact)

---

### Phase 6: Fetcher Integration - Roster HTML
**Goal**: Integrate roster HTML fetching and parsing into the fetcher

**Tasks**:
- Fetch roster HTML when `include-rosters=true` (using roster URL from right-rail)
- Parse roster HTML using Phase 3 module
- Match and enrich HTML roster with API roster data using Phase 4 logic
- Cache parsed roster data
- Pass enriched roster data to game parsing
- Add tests for HTML fetching and parsing integration

**Deliverables**:
- ✅ Roster HTML fetching integrated
- ✅ Parsing integrated
- ✅ Enrichment integrated
- ✅ Tests passing
- ✅ No external API changes (data processed but not yet in response)

**Release**: Can be released immediately (internal changes, no external impact)

---

### Phase 7: API Response Integration
**Goal**: Add roster data to API responses

**Tasks**:
- Update `game_scores.clj` to accept roster data parameter
- Add `:rosters` field to game details when roster data present
- Format roster data: `{:rosters {:away [...], :home [...]}}`
- Use `reject-empty-vals-except-for-keys` to exclude when not present
- Update `parse-game-scores` to pass roster data
- Add tests for response format

**Deliverables**:
- ✅ Roster data in API responses
- ✅ Format matches specification
- ✅ Tests passing
- ✅ External API change: `?include=rosters` now returns roster data

**Release**: **External API change** - Feature complete and ready for use

---

### Phase 8: End-to-End Testing and Documentation
**Goal**: Comprehensive testing and documentation

**Tasks**:
- End-to-end integration tests
- Test with multiple games
- Test edge cases (missing rosters, unmatched players, etc.)
- Performance testing
- Update API documentation
- Update README if needed

**Deliverables**:
- ✅ Comprehensive test coverage
- ✅ Documentation updated
- ✅ Production ready

**Release**: Final polish and documentation

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
- ✅ Team roster API endpoint confirmed: `/v1/roster/{teamAbbrev}/{season}`
- ✅ Sample API responses saved for testing
- Player matching uses jersey number (`sweaterNumber`) + position code (`positionCode`) as primary key, with name as fallback
- Position codes match exactly between HTML (G, D, C, L, R) and API (G, D, C, L, R)

## Implementation Summary

### Completed Preparation Steps
1. ✅ Fetch a sample roster HTML file (`roster-2023020207.html` saved)
2. ✅ Determine the exact HTML structure and parsing strategy
3. ✅ Determine team roster API endpoint (`/v1/roster/{teamAbbrev}/{season}`)
4. ✅ Fetch sample API responses (`roster-api-CGY-20232024.json`, `roster-api-TOR-20232024.json`)

### Next Steps: Phase-by-Phase Implementation

**Start with Phase 1**: Query Parameter Infrastructure
- Smallest, most isolated change
- No external dependencies
- Can be released immediately
- Sets foundation for future phases

**Then proceed sequentially** through phases 2-8, each building on the previous.

### Phase Dependencies

```
Phase 1 (Query Parameter)
    ↓
Phase 2 (Team Roster API) ──┐
    ↓                        │
Phase 3 (HTML Parser) ───────┤
    ↓                        │
Phase 4 (Matching Logic) ←──┘
    ↓
Phase 5 (Fetcher - API)
    ↓
Phase 6 (Fetcher - HTML)
    ↓
Phase 7 (API Response) ← External API change
    ↓
Phase 8 (Testing & Docs)
```

### Benefits of Phased Approach

1. **Incremental Progress**: Each phase is completable and testable independently
2. **Early Releases**: Phases 1-6 can be released without external API changes
3. **Risk Mitigation**: Issues can be caught and fixed in isolated phases
4. **Code Review**: Smaller, focused changes are easier to review
5. **Testing**: Each phase can be thoroughly tested before moving to next
6. **Rollback**: If issues arise, can rollback specific phases
