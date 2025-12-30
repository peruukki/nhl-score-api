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

### 2. Parse Winning Goalie Data (`src/nhl_score_api/fetchers/nhl_api_web/game_scores.clj`)

**Action**: Create a function to parse winning goalie information from schedule game data and look up full first name.

**Details**:
- Create `parse-winning-goalie` function that:
  1. Extracts `player-id`, `first-initial`, and `last-name` from schedule game's `winningGoalie`
  2. Looks up the full first name using the `player-id` from the landing endpoint data
  3. Falls back to using `first-initial` if full name not found
- The landing endpoint is already fetched for each game (used for goal data), so we can search for the goalie's `player-id` in that data
- Format as: `{:player-id <id>, :player "<full-first-name> <last-name>"}`
- Handle cases where `winningGoalie` is missing (games not finished, or in progress)

**Lookup Strategy**:
- Search through landing endpoint's goal scorer and assist data (`summary.scoring[].goals[]` and `assists[]`) for matching `playerId`
- If found, use the full `firstName` from that entry
- **Note**: Goalies rarely score or assist, so this lookup may not always find the full name
- If not found, we have two options:
  1. **Fallback to initial**: Use `firstInitial` from schedule (e.g., "C. Lindgren")
  2. **Additional lookup**: Check if goalie appears in other landing data (e.g., threeStars, though that also uses abbreviated names)
  3. **Accept limitation**: Document that full first name may not always be available from the API

**Example Output**:
```clojure
{:player-id 8479292
 :player "Charlie Lindgren"}  ; Full first name if found, otherwise "C. Lindgren"
```

**Rationale**: Match the format used for goal scorers (full first name + last name) rather than using initials. The landing endpoint data is already available, so we can look up the full name without additional API calls.

### 3. Add Goalie to Game Details (`src/nhl_score_api/fetchers/nhl_api_web/game_scores.clj`)

**Action**: Include winning goalie information in the game details returned by `parse-game-details`.

**Details**:
- Call `parse-winning-goalie` in `parse-game-details` function
- Add `:winning-goalie` field to the game details map
- Only include this field when a winning goalie exists (finished games)
- Use `reject-empty-vals-except-for-keys` pattern to exclude empty goalie data for games without it

**Location in Code Flow**:
- Modify `parse-game-details` function around line 480
- Add goalie parsing after parsing team details but before adding stats

**Rationale**: Include goalie information at the top level of game details for easy access, similar to how `goals`, `scores`, and `teams` are structured.

### 4. Update Tests (`test/nhl_score_api/fetchers/nhl_api_web/game_scores_test.clj`)

**Action**: Add test cases to verify winning goalie parsing.

**Details**:
- Add test for parsing winning goalie from finished games
- Add test for missing winning goalie in live/preview games
- Verify goalie data structure matches expected format
- Test with multiple games to ensure consistency

**Test Cases**:
1. **Finished game with winning goalie**: Verify goalie data is present and correctly formatted
2. **Live game**: Verify no winning goalie field (or nil/empty)
3. **Preview game**: Verify no winning goalie field
4. **Multiple finished games**: Verify each has correct goalie data

**Test Data**: Use existing test resources:
- `schedule-2023-11-09-modified-for-validation.json` (has winning goalies)
- `schedule-2023-11-08-modified.json` (has multiple winning goalies)

### 5. Update Schedule Schema Tests (`test/nhl_score_api/fetchers/nhl_api_web/api/schedule_test.clj`)

**Action**: Verify schema validation accepts `winningGoalie` field.

**Details**:
- Ensure existing tests still pass with the updated schema
- Optionally add a test case that validates a schedule response with winning goalie data

**Rationale**: Ensure backward compatibility and proper validation of the new field.

## Implementation Order

1. **Step 1**: Update schema validation (schedule.clj)
2. **Step 2**: Implement parsing function (game_scores.clj)
3. **Step 3**: Integrate into game details (game_scores.clj)
4. **Step 4**: Add tests (game_scores_test.clj)
5. **Step 5**: Verify schema tests (schedule_test.clj)

## Edge Cases to Consider

1. **Missing `winningGoalie` field**: 
   - Games that haven't finished yet (LIVE, PREVIEW states)
   - Games that were canceled/postponed
   - Handle gracefully by not including the field

2. **Empty/Missing goalie data**:
   - Use `reject-empty-vals-except-for-keys` to exclude empty goalie fields
   - Similar pattern to how `goals` and `links` are handled

3. **Shootout games**:
   - Verify if `winningGoalie` is present for shootout games
   - May need special handling if goalie is determined differently

4. **Playoff games**:
   - Verify `winningGoalie` is present in playoff game responses
   - Ensure consistent behavior across regular season and playoffs

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

**Important**: The schedule endpoint only provides `firstInitial` for goalies, not the full first name. To get the full first name (matching the format used for goal scorers), we'll look up the goalie's `playerId` in the landing endpoint data that's already being fetched. If the goalie appears as a goal scorer or assist provider, we can extract their full `firstName` from there. Otherwise, we'll fall back to using the `firstInitial` from the schedule endpoint.

## Testing Strategy

1. **Unit Tests**: Test parsing function in isolation
2. **Integration Tests**: Test full game parsing with goalie data
3. **Edge Case Tests**: Test missing/null goalie scenarios
4. **Schema Validation**: Ensure API responses validate correctly

## Notes

- The NHL API only provides `winningGoalie`, not `losingGoalie`
- Goalie information is only available for finished games
- The schedule endpoint provides `firstInitial` (not full `firstName`) for goalies
- To get full first name: look up goalie's `playerId` in landing endpoint data (already fetched)
- Goal scorers use full "First Last" format - goalies should match this for consistency
- Fallback: if goalie's full name not found in landing data, use `firstInitial` from schedule
