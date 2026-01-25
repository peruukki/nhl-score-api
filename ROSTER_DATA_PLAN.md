# Plan: Adding Optional Roster Information to API Responses

## Overview
The NHL Web API right-rail endpoint includes a link to an HTML roster report for finished or live games. This plan outlines adding optional roster information to API responses, controlled by a query parameter. Since fetching and parsing HTML requires an extra HTTP request, it will only be included when explicitly requested.

## Implementation Strategy: Phased Approach

This implementation is divided into **4 incremental phases** that can be fully implemented, tested, and released independently:

1. **Phase 1**: Roster HTML Parser Module (standalone module)
2. **Phase 2**: Fetcher Integration - Roster HTML (internal changes)
3. **Phase 3**: API Response Integration (**external API change**)
4. **Phase 4**: Query Parameter Infrastructure (**external API change**)

**Key Benefits**:
- Phases 1-2 can be released without external API changes
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

### 3. Create Roster Parser Module (`src/nhl_score_api/fetchers/nhl_api_web/roster_parser.clj` - NEW FILE)

**Action**: Create a new module to fetch and parse roster HTML.

**Details**:
- Create `fetch-roster-html` function to fetch HTML from roster URL
- Create `parse-roster-html` function to parse HTML and extract player information
- Use `enlive` (already in project dependencies) for HTML parsing
- Extract roster data structure:
  - Away team dressed players (players with positions, numbers, names)
  - Home team dressed players (players with positions, numbers, names)
  - Away team scratched players (players not in lineup)
  - Home team scratched players (players not in lineup)

**HTML Parsing Strategy**:
- The roster HTML has been inspected (see `roster-2023020207.html` in test resources)
- Structure:
  - **Playing roster**: Two main sections: `<table id="Visitor">` (away team) and `<table id="Home">` (home team)
    - Each section contains a table with columns: `#` (jersey number), `Pos` (position), `Name`
    - Player rows: `<tr><td>#</td><td>Pos</td><td>Name</td></tr>`
    - Positions: `G` (goalie), `D` (defense), `C` (center), `L` (left wing), `R` (right wing)
    - Names are in UPPERCASE format (e.g., "DUSTIN WOLF", "JACOB MARKSTROM")
    - **Starting lineup**: `<td>` elements with `class="bold"` or `class="bold + italic"` indicate starting lineup
    - Some names include `(C)` for captain or `(A)` for alternate captain
  - **Scratched Players**: Section with `<tr id="Scratches">` containing two tables (away and home teams)
    - Same structure as playing roster: `#`, `Pos`, `Name` columns
    - Scratched players do not have starting lineup indicators
- Extract for playing roster:
  - **Jersey number** (from first `<td>`)
  - **Position** (from second `<td>`)
  - **Player name** (from third `<td>`, remove captain/alternate markers, normalize from UPPERCASE to title case)
  - **Team** (away/home based on table ID)
  - **Starting lineup indicator**: Check if any `<td>` in the row has `class` containing "bold"
    - Example: `<td class="bold">80</td><td class="bold">G</td>` → starting lineup = true
    - **Especially important for goalies**: Starting goalie will have `class="bold"` on position `<td>`
- Extract for scratched players:
  - **Jersey number** (from first `<td>`)
  - **Position** (from second `<td>`)
  - **Player name** (from third `<td>`, normalize from UPPERCASE to title case)
  - **Team** (away/home based on table position)
- **Important**: Player IDs are NOT available in the HTML - only jersey numbers, positions, and names
- **Starting Lineup**: Players with `class="bold"` are in the starting lineup (especially important for goalies)

**Caching**: Cache parsed roster data similar to other API responses to avoid repeated HTML fetching/parsing.

**Rationale**: Centralize roster fetching and parsing logic in a dedicated module.

### 4. Update Fetcher to Support Roster Fetching (`src/nhl_score_api/fetchers/nhl_api_web/fetcher.clj`)

**Action**: Fetch and parse roster HTML for games.

**Details**:
- For each game:
  1. Fetch roster HTML (using roster URL from right-rail data)
  2. Parse roster HTML
  3. Store parsed roster data alongside gamecenter data
- **Note**: Phase 4 will add query parameter control to make roster fetching conditional.

**Caching**: Cache parsed roster data to avoid repeated fetching/parsing.

**Rationale**: Fetch and parse rosters for all games. Phase 4 will add conditional fetching based on query parameters.

### 5. Add Roster to Game Details (`src/nhl_score_api/fetchers/nhl_api_web/game_scores.clj`)

**Action**: Include roster information in game details when available.

**Details**:
- Update `parse-game-details` function signature to accept optional roster data
- Add `:rosters` field to game details map when roster data is present
- Structure roster data as:
  ```clojure
  {:rosters
   {:away {:dressedPlayers [{:name "Connor Zary" :position "C" :number 47} ...  ; not in starting lineup
                             {:name "Dan Vladar" :position "G" :number 80 :starting-lineup true} ...]  ; starting goalie
           :scratchedPlayers [{:name "Dennis Gilbert" :position "D" :number 48} ...]}
    :home {:dressedPlayers [{:name "William Nylander" :position "R" :number 88} ...]
           :scratchedPlayers [{:name "John Klingberg" :position "D" :number 3} ...]}}}
  ```
- Include data extracted from roster HTML:
  - For dressed players:
    - `name`: Full player name (normalized from uppercase HTML to title case)
    - `position`: Player position (G, D, C, L, R)
    - `number`: Jersey number
    - `starting-lineup`: Boolean indicating if player is in starting lineup (from HTML `class="bold"`)
      - **Only include when `true`** (omit when `false`, similar to `empty-net` and `strength` fields)
  - For scratched players:
    - `name`: Full player name (normalized from uppercase HTML to title case)
    - `position`: Player position (G, D, C, L, R)
    - `number`: Jersey number
- **Starting Lineup**: Extracted from HTML where players have `class="bold"` on their row
- **Goalies**: Starting goalie is especially important - will be marked with `starting-lineup: true` (only field present)
- Use conditional field inclusion pattern (similar to `add-goal-strength`, `add-empty-net-flag` functions)
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
- Test extracting away team dressed players
- Test extracting home team dressed players
- Test extracting away team scratched players
- Test extracting home team scratched players
- Test extracting player information (name, position, number)
- **Test extracting starting lineup information** (from `class="bold"`)
- **Test starting lineup for goalies** (verify starting goalie has `startingLineup: true`, backup goalies omit field)
- Test handling of malformed or missing roster HTML
- Test handling of network errors

**Test Data**:
- ✅ Sample roster HTML file saved: `roster-2023020207.html` (game ID 2023020207)
- Can fetch additional roster HTML files for other test games if needed

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
- Test roster structure matches expected format (away and home objects with dressedPlayers and scratchedPlayers)
- Test away team structure (dressedPlayers and scratchedPlayers arrays)
- Test home team structure (dressedPlayers and scratchedPlayers arrays)
- **Test starting lineup field only present when true** (omitted when false)
- **Test starting goalies are correctly marked** (`startingLineup: true` present, others omit field)
- Test with multiple games

#### 6d. Core Tests (`test/nhl_score_api/core_test.clj`)

**Action**: Add tests for query parameter handling.

**Details**:
- Test `include=rosters` query parameter
- Test `include=rosters,otherThing` (should still include rosters)
- Test missing parameter (no inclusions)
- Test empty `include` parameter
- Test case-insensitive parsing (e.g., `include=ROSTERS`)

## Implementation Phases

The implementation is divided into incremental phases that can be fully implemented, tested, and released independently. Each phase builds on previous phases.

### Phase 1: Roster HTML Parser Module
**Goal**: Create standalone module for parsing roster HTML files

**Tasks**:
- Create `roster_parser.clj` module
- Implement HTML parsing with `enlive`
- Extract roster data from HTML:
  - Jersey number, position, name
  - **Starting lineup indicator** (from `class="bold"` on `<tr>` or `<td>`)
- Parse away and home team dressed players (from `<table id="Visitor">` and `<table id="Home">`)
- Parse away and home team scratched players (from `<tr id="Scratches">` section)
- Normalize names (handle UPPERCASE, remove captain/alternate markers)
- Extract starting lineup status for each dressed player
- Add tests using sample HTML (`roster-2023020207.html`)

**Deliverables**:
- ✅ HTML parsing functionality
- ✅ Extracted roster data structure (including starting lineup)
- ✅ Tests passing (verify starting lineup extraction, especially for goalies)
- ✅ Can parse roster HTML independently

**Release**: Can be released immediately (new internal module, not yet integrated)

---

### Phase 2: Fetcher Integration - Roster HTML
**Goal**: Integrate roster HTML fetching and parsing into the fetcher

**Tasks**:
- Fetch roster HTML (using roster URL from right-rail)
- Parse roster HTML using Phase 1 module
- Cache parsed roster data
- Pass parsed roster data to game parsing
- Add tests for HTML fetching and parsing integration

**Deliverables**:
- ✅ Roster HTML fetching integrated
- ✅ Parsing integrated
- ✅ Tests passing
- ✅ No external API changes (data processed but not yet in response)

**Release**: Can be released immediately (internal changes, no external impact)

---

### Phase 3: API Response Integration
**Goal**: Add roster data to API responses (always included when available)

**Tasks**:
- Update `game_scores.clj` to accept roster data parameter
- Add `:rosters` field to game details when roster data present
- Format roster data: `{:rosters {:away {:dressedPlayers [...], :scratchedPlayers [...]}, :home {:dressedPlayers [...], :scratchedPlayers [...]}}}`
- Include `starting-lineup` field **only when `true`** (omit when `false`)
  - Use conditional field inclusion pattern (similar to `add-goal-strength`, `add-empty-net-flag`)
  - Create helper function: `add-starting-lineup-if-true` or similar
- Use `reject-empty-vals-except-for-keys` to exclude when not present
- Update `parse-game-scores` to pass roster data
- **Note**: Roster data will always be included when available. Phase 4 will add query parameter control.
- Add tests for response format (verify `startingLineup` only present when `true`)
- End-to-end integration tests
- Test with multiple games
- Test edge cases (missing rosters, parsing failures, etc.)
- Update API documentation
- Update README if needed

**Deliverables**:
- ✅ Roster data in API responses (including starting lineup when true)
- ✅ Format matches specification (`startingLineup` omitted when false)
- ✅ Starting lineup information preserved (especially goalies)
- ✅ Tests passing (verify field omission behavior)
- ✅ Comprehensive test coverage
- ✅ Documentation updated
- ✅ External API change: Roster data now always included in responses when available

**Release**: **External API change** - Roster data always included when available

---

### Phase 4: Query Parameter Infrastructure
**Goal**: Add query parameter control so roster data is only returned when requested

**Tasks**:
- Add string parameter parsing to `param_parser.clj`
- Support comma-separated values (e.g., `include=rosters,otherThing`)
- Update `core.clj` to parse `include` query parameter and check for "rosters"
- Wire up query parameter to `include-rosters` flag in fetcher (replacing hardcoded value from Phase 2)
- Only include roster data in API responses when `include=rosters` query parameter is present
- Add tests for parameter parsing
- Add tests for query parameter controlling roster inclusion

**Deliverables**:
- ✅ Parameter parsing functionality
- ✅ Query parameter controls roster inclusion
- ✅ Tests passing
- ✅ External API change: Roster data only returned when `?include=rosters` is present

**Release**: **External API change** - Roster data now optional via query parameter

## Edge Cases to Consider

1. **Missing roster link**:
   - Roster link may not be available for all games
   - Handle gracefully by not including roster data for that game
   - Don't fail the entire request

2. **Missing scratched players section**:
   - Some games may not have a scratched players section in the HTML
   - Handle gracefully by including empty scratchedPlayers arrays or omitting scratchedPlayers field for that team
   - Don't fail parsing if scratched players section is missing

3. **Starting lineup extraction**:
   - Need to correctly identify `class="bold"` on `<td>` elements
   - Handle variations like `class="bold + italic"` (captain in starting lineup)
   - Ensure starting goalie is correctly identified (critical information)
   - If starting lineup can't be determined, default to `false` (field will be omitted)
   - **Field inclusion**: Only include `starting-lineup` field when value is `true` (omit when `false`)
   - Follow existing pattern: `add-goal-strength`, `add-empty-net-flag` functions conditionally add fields

3. **Roster HTML parsing failures**:
   - Malformed HTML
   - Network errors when fetching roster
   - Handle gracefully - log error but don't fail request
   - Don't include roster data for that game

4. **Roster HTML structure changes**:
   - NHL may change HTML structure
   - Parsing may fail silently
   - Consider adding validation/logging

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
    "away": {
      "dressedPlayers": [
        {
          "name": "Connor Zary",
          "position": "C",
          "number": 47
        },
        {
          "name": "Rasmus Andersson",
          "position": "D",
          "number": 4
        },
        {
          "name": "Dan Vladar",
          "position": "G",
          "number": 80,
          "startingLineup": true
        },
        ...
      ],
      "scratchedPlayers": [
        {
          "name": "Dennis Gilbert",
          "position": "D",
          "number": 48
        },
        ...
      ]
    },
    "home": {
      "dressedPlayers": [
        {
          "name": "William Nylander",
          "position": "R",
          "number": 88
        },
        {
          "name": "Ilya Samsonov",
          "position": "G",
          "number": 60,
          "startingLineup": true
        },
        ...
      ],
      "scratchedPlayers": [
        {
          "name": "John Klingberg",
          "position": "D",
          "number": 3
        },
        ...
      ]
    }
  }
}
```

**Note**:
- `startingLineup` field is **only included when `true`** (omitted when `false`), similar to how `empty-net` and `strength` fields are handled in goal data.
- This makes it easy to identify starting players (especially starting goalies) - they're the only ones with the `startingLineup` field present.

**Note**: `rosters` field will only be present when `include=rosters` query parameter is provided. It will be omitted otherwise.

## API Endpoint Changes

**Phase 3**: Roster data is always included in responses when available (no query parameter needed).

**Phase 4**: Roster data is only included when `include=rosters` query parameter is present.

### `/api/scores/latest` (Phase 4)
- **New Query Parameter**: `include` (string, optional, comma-separated list)
- **Example**: `/api/scores/latest?include=rosters`
- **Future Example**: `/api/scores/latest?include=rosters,otherThing`

### `/api/scores` (Phase 4)
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

## Notes

- Roster HTML is only available for finished or live games
- Roster fetching adds 1 HTTP request per game when requested (roster HTML)
- Project already includes `enlive` HTML parsing library (no new dependency needed)
- Roster data is cached to avoid repeated fetching/parsing
- Query parameter is optional - existing API calls continue to work without changes
- `include` parameter supports comma-separated values for future extensibility (e.g., `include=rosters,otherThing`)
- Roster HTML structure has been analyzed (see `roster-2023020207.html` in test resources)

## Implementation Summary

### Completed Preparation Steps
1. ✅ Fetch a sample roster HTML file (`roster-2023020207.html` saved)
2. ✅ Determine the exact HTML structure and parsing strategy

### Next Steps: Phase-by-Phase Implementation

**Start with Phase 1**: Roster HTML Parser Module
- Focus on core functionality: parsing roster HTML
- Can be tested independently
- Sets foundation for integration phases

**Then proceed sequentially** through phases 2-4, each building on the previous.

### Phase Dependencies

```
Phase 1 (HTML Parser)
    ↓
Phase 2 (Fetcher - HTML)
    ↓
Phase 3 (API Response) ← External API change
    ↓
Phase 4 (Query Parameter) ← External API change
```

### Benefits of Phased Approach

1. **Incremental Progress**: Each phase is completable and testable independently
2. **Early Releases**: Phases 1-2 can be released without external API changes
3. **Risk Mitigation**: Issues can be caught and fixed in isolated phases
4. **Code Review**: Smaller, focused changes are easier to review
5. **Testing**: Each phase can be thoroughly tested before moving to next
6. **Rollback**: If issues arise, can rollback specific phases
