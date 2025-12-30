# Plan: Adding Winning Goalie Information to API Responses

## Overview
The NHL Web API schedule endpoint (`/v1/schedule/{date}`) includes `winningGoalie` information for finished games. This plan outlines the steps to extract and include this data in the application's API responses.

## Current State

### Data Source
- **Endpoint**: `https://api-web.nhle.com/v1/schedule/{date}`
- **Field**: `winningGoalie` (present in schedule game objects for finished games)
- **Structure**:
  ```json
  {
    "winningGoalie": {
      "playerId": 8479292,
      "firstInitial": {
        "default": "C."
      },
      "lastName": {
        "default": "Lindgren"
      }
    }
  }
  ```

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

## Implementation Plan

### 1. Update Schema Validation (`src/nhl_score_api/fetchers/nhl_api_web/api/schedule.clj`)

**Action**: Add `winningGoalie` to the `GameSchema` validation schema.

**Details**:
- Add an optional `winning-goalie` field to `GameSchema`
- Create a new `WinningGoalieSchema` with:
  - `player-id`: integer
  - `first-initial`: Localized string schema
  - `last-name`: Localized string schema

**Rationale**: Ensure the API response structure is validated correctly when parsing schedule data.

### 2. Fetch and Parse Roster HTML (`src/nhl_score_api/fetchers/nhl_api_web/roster_parser.clj` - NEW FILE)

**Action**: Create a new module to fetch and parse the roster HTML report to extract full goalie names.

**Details**:
- Create `fetch-roster-html` function to fetch the HTML from the rosters URL
- The rosters URL is available in the right-rail data: `(:rosters gamecenter-data)`
- Parse HTML to extract goalie information (full names, player IDs)
- Use an HTML parsing library (e.g., `clojure.data.xml` or `hickory` for HTML parsing)
- Extract goalie data by:
  - Finding goalie rows/sections (typically marked with position "G")
  - Matching by `playerId` from schedule's `winningGoalie`
  - Extracting full first name and last name

**HTML Parsing Strategy**:
- The roster HTML is likely a structured table with player information
- Need to identify the structure (table rows, cells, etc.)
- Match goalies by `playerId` to find the correct player
- Extract full name from the HTML structure

**Caching**: Cache parsed roster data similar to other API responses to avoid repeated HTML parsing.

**Rationale**: The roster HTML report contains full player names and is already linked in the right-rail data. This provides a reliable source for full goalie names.

### 3. Parse Winning Goalie Data (`src/nhl_score_api/fetchers/nhl_api_web/game_scores.clj`)

**Action**: Create a function to parse winning goalie information from schedule game data and enrich with full name from roster.

**Details**:
- Create `parse-winning-goalie` function that:
  1. Extracts `player-id`, `first-initial`, and `last-name` from schedule game's `winningGoalie`
  2. Looks up the full first name from parsed roster HTML data using `player-id`
  3. Falls back to using `first-initial` if roster data not available or goalie not found
- Format as: `{:player-id <id>, :player "<full-first-name> <last-name>"}`
- Handle cases where `winningGoalie` is missing (games not finished, or in progress)

**Example Output**:
```clojure
{:player-id 8479292
 :player "Charlie Lindgren"}  ; Full first name from roster, fallback to "C. Lindgren" if not found
```

**Rationale**: Match the format used for goal scorers (full first name + last name). The roster HTML provides a reliable source for full names.

### 4. Integrate Roster Fetching (`src/nhl_score_api/fetchers/nhl_api_web/fetcher.clj`)

**Action**: Fetch roster HTML when fetching gamecenter data for games with winning goalies.

**Details**:
- Modify `fetch-gamecenters` or create a parallel function to fetch roster HTML
- Only fetch roster HTML for finished games (those with `winningGoalie` in schedule)
- Parse roster HTML and store parsed data alongside gamecenter data
- Pass parsed roster data to `parse-game-details` function

**Caching**: Cache parsed roster data to avoid repeated HTML fetching/parsing.

**Rationale**: Roster HTML needs to be fetched and parsed before we can use it to enrich goalie names.

### 5. Add Goalie to Game Details (`src/nhl_score_api/fetchers/nhl_api_web/game_scores.clj`)

**Action**: Include winning goalie information in the game details returned by `parse-game-details`.

**Details**:
- Call `parse-winning-goalie` in `parse-game-details` function
- Pass parsed roster data to `parse-winning-goalie` for name lookup
- Add `:winning-goalie` field to the game details map
- Only include this field when a winning goalie exists (finished games)
- Use `reject-empty-vals-except-for-keys` pattern to exclude empty goalie data for games without it

**Location in Code Flow**:
- Modify `parse-game-details` function around line 480
- Add goalie parsing after parsing team details but before adding stats
- Update function signature to accept roster data parameter

**Rationale**: Include goalie information at the top level of game details for easy access, similar to how `goals`, `scores`, and `teams` are structured.

### 6. Add Roster Parser Tests (`test/nhl_score_api/fetchers/nhl_api_web/roster_parser_test.clj` - NEW FILE)

**Action**: Add tests for roster HTML parsing.

**Details**:
- Test parsing goalie information from roster HTML
- Test matching goalie by playerId
- Test fallback when goalie not found in roster
- Test handling of malformed or missing roster HTML

**Test Data**: May need to create sample roster HTML files or use actual roster HTML from test games.

### 7. Update Game Scores Tests (`test/nhl_score_api/fetchers/nhl_api_web/game_scores_test.clj`)

**Action**: Add test cases to verify winning goalie parsing with roster data.

**Details**:
- Add test for parsing winning goalie from finished games with roster data
- Add test for missing winning goalie in live/preview games
- Add test for fallback to initial when roster data unavailable
- Verify goalie data structure matches expected format (full name)
- Test with multiple games to ensure consistency

**Test Cases**:
1. **Finished game with winning goalie and roster**: Verify goalie data is present with full name
2. **Finished game without roster data**: Verify fallback to initial
3. **Live game**: Verify no winning goalie field (or nil/empty)
4. **Preview game**: Verify no winning goalie field
5. **Multiple finished games**: Verify each has correct goalie data

**Test Data**: Use existing test resources:
- `schedule-2023-11-09-modified-for-validation.json` (has winning goalies)
- `schedule-2023-11-08-modified.json` (has multiple winning goalies)
- May need to fetch/create sample roster HTML for test games

### 8. Update Schedule Schema Tests (`test/nhl_score_api/fetchers/nhl_api_web/api/schedule_test.clj`)

**Action**: Verify schema validation accepts `winningGoalie` field.

**Details**:
- Ensure existing tests still pass with the updated schema
- Optionally add a test case that validates a schedule response with winning goalie data

**Rationale**: Ensure backward compatibility and proper validation of the new field.

## Implementation Order

1. **Step 1**: Update schema validation (schedule.clj)
2. **Step 2**: Create roster parser module (roster_parser.clj - NEW)
   - Add HTML parsing dependency if needed (check project.clj)
   - Implement roster HTML fetching and parsing
   - Extract goalie information by playerId
3. **Step 3**: Integrate roster fetching (fetcher.clj)
   - Fetch roster HTML for games with winning goalies
   - Parse and cache roster data
4. **Step 4**: Implement goalie parsing function (game_scores.clj)
   - Parse winning goalie from schedule
   - Look up full name from roster data
   - Fallback to initial if roster unavailable
5. **Step 5**: Add goalie to game details (game_scores.clj)
   - Integrate into `parse-game-details`
   - Update function signatures to pass roster data
6. **Step 6**: Add tests (roster_parser_test.clj, game_scores_test.clj)
7. **Step 7**: Verify schema tests (schedule_test.clj)

## Edge Cases to Consider

1. **Missing `winningGoalie` field**: 
   - Games that haven't finished yet (LIVE, PREVIEW states)
   - Games that were canceled/postponed
   - Handle gracefully by not including the field

2. **Missing roster HTML**:
   - Roster link may not be available for all games
   - Fallback to using `firstInitial` from schedule
   - Document this limitation

3. **Goalie not found in roster**:
   - PlayerId mismatch or roster parsing error
   - Fallback to using `firstInitial` from schedule

4. **Roster HTML parsing failures**:
   - Malformed HTML
   - Network errors when fetching roster
   - Handle gracefully with fallback

5. **Empty/Missing goalie data**:
   - Use `reject-empty-vals-except-for-keys` to exclude empty goalie fields
   - Similar pattern to how `goals` and `links` are handled

6. **Shootout games**:
   - Verify if `winningGoalie` is present for shootout games
   - May need special handling if goalie is determined differently

7. **Playoff games**:
   - Verify `winningGoalie` is present in playoff game responses
   - Ensure consistent behavior across regular season and playoffs

8. **HTML parsing library**:
   - Project already includes `enlive` (line 13 in project.clj)
   - Use `enlive` for parsing roster HTML
   - No additional dependency needed

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

### After
```json
{
  "status": {...},
  "startTime": "...",
  "goals": [...],
  "scores": {...},
  "teams": {...},
  "winningGoalie": {
    "playerId": 8479292,
    "player": "C. Lindgren"
  }
}
```

**Note**: `winningGoalie` will only be present for finished games. It will be omitted for live/preview games.

**Important**: The schedule endpoint only provides `firstInitial` for goalies, not the full first name. To get the full first name (matching the format used for goal scorers), we'll fetch and parse the roster HTML report that's linked in the right-rail data. The roster HTML contains full player names and can be matched by `playerId` to get the winning goalie's full name. If the roster HTML is unavailable or parsing fails, we'll fall back to using the `firstInitial` from the schedule endpoint.

## Testing Strategy

1. **Unit Tests**: Test parsing function in isolation
2. **Integration Tests**: Test full game parsing with goalie data
3. **Edge Case Tests**: Test missing/null goalie scenarios
4. **Schema Validation**: Ensure API responses validate correctly

## Notes

- The NHL API only provides `winningGoalie`, not `losingGoalie`
- Goalie information is only available for finished games
- The schedule endpoint provides `firstInitial` (not full `firstName`) for goalies
- The roster HTML report (linked in right-rail data) contains full player names
- Roster HTML needs to be fetched and parsed (HTML, not JSON)
- Goal scorers use full "First Last" format - goalies should match this for consistency
- Fallback: if roster HTML unavailable or parsing fails, use `firstInitial` from schedule
- Project already includes `enlive` HTML parsing library (no new dependency needed)
- Roster HTML fetching adds an additional HTTP request per game (but can be cached)
