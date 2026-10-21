# Dungeon Escape

A Java 21 dungeon escape game built with Java Swing. Explore dangerous dungeons, avoid monsters, collect items, and progress through three levels.

## Features

- **Three playable levels** with level progression.
- **Player movement and collision detection** with walls and dungeon boundaries.
- **Enemy encounters**, including animated monster sprites.
- **Dungeon items and objects**, including keys, chests, doors, potions, and torches.
- **Animated sprites** for the player and monsters.
- **Audio effects** managed through a dedicated audio manager.
- **Game HUD** displaying gameplay information.
- **Title and result screens**, including starting and replaying the game.
- **High-score leaderboard**: the top ten runs kept in a fixed-capacity array, ordered with a hand-written insertion sort and queried with a hand-written linear search by player name; the result screen has a name-search box that uses those same search methods.
- **Recursive reachability checker**: a recursive DFS flood fill applied when levels are built, failing fast if a layout ever strands the key, chest, potion, or exit door, and reinforced by tests that verify real layouts stay fully connected.
- **Move history and undo**: a hand-written array-based LIFO stack of the player's previous positions, with a Z-key undo that steps back one tile move.
- **FIFO game-event queue**: a hand-written circular-buffer queue that carries pending gameplay audio cues from the game rules to the sound layer in strict first-in, first-out order.
- **Leaderboard persistence**: every completed run is saved to a local CSV file after the run (the visible top-10 leaderboard is rebuilt from it at startup), with proper CSV escaping for special player names and a complete persistent history the result screen can search.
- **Automated tests** using a dependency-free Java test harness.
- **No external libraries or build tools required.**

## Requirements

- JDK 21 or a compatible newer JDK.
- Windows CMD or another terminal that supports Java commands.
- Git, if you want to clone or contribute to the repository.

## Project Structure

```text
DungeonEscapeV2/
├── src/
│   └── com/dungeonescape/
│       ├── Main.java
│       ├── asset/
│       │   ├── AssetLoader.java
│       │   ├── AssetLoadException.java
│       │   ├── AudioInfo.java
│       │   ├── AssetDiagnostic.java
│       │   └── AudioManager.java
│       ├── game/
│       │   ├── Dungeon.java
│       │   ├── GameEventQueue.java
│       │   ├── DungeonReachability.java
│       │   ├── GameObject.java
│       │   ├── GameSession.java
│       │   ├── HighScoreBoard.java
│       │   ├── HighScoreStorage.java
│       │   ├── InputState.java
│       │   ├── Level.java
│       │   ├── Levels.java
│       │   ├── MoveHistory.java
│       │   ├── Player.java
│       │   ├── ScoreHistory.java
│       │   └── SearchableScores.java
│       └── ui/
│           ├── DungeonPanel.java
│           ├── GameFlowListener.java
│           ├── GameWindow.java
│           ├── HudPanel.java
│           ├── ResultPanel.java
│           └── TitlePanel.java
├── test/
│   └── com/dungeonescape/
│   └── com/dungeonescape/
│       ├── asset/
│       │   └── AudioManagerTest.java
│       ├── game/
│       │   ├── DungeonReachabilityTest.java
│       │   ├── GameEventQueueTest.java
│       │   ├── GameSessionTest.java
│       │   ├── HighScoreBoardTest.java
│       │   ├── HighScoreStorageTest.java
│       │   ├── LevelsReachabilityTest.java
│       │   ├── MoveHistoryTest.java
│       │   ├── PlayerTest.java
│       │   └── ScoreHistoryTest.java
│       └── ui/
│           ├── InputBindingTest.java
│           ├── ResultSearchTest.java
│           └── ScreensTest.java

## Controls

| Action | Controls |
|---|---|
| Move up | W or Up Arrow |
| Move down | S or Down Arrow |
| Move left | A or Left Arrow |
| Move right | D or Right Arrow |
| Undo last step | Z |

The game uses keyboard bindings and collision detection to prevent the player from walking through solid walls or outside the dungeon boundaries.

## High-score leaderboard

`src/com/dungeonescape/game/HighScoreBoard.java` is pure game logic with no Swing dependency. Each finished run (victory or game over) is recorded once from the result screen, together with the name typed on the title screen.

Implementation, all hand-written with no library sorting or searching:

- **Storage**: a fixed-capacity plain array `ScoreEntry[10]`, where each `ScoreEntry` holds a player name and a score. The board never grows past `MAX_ENTRIES = 10`.
- **Insertion sort**: `record()` reorders the used array slots from highest to lowest score with its own insertion loop after every record. The comparison is a strict `<`, so the sort is stable: equal scores keep the order in which the runs were recorded.
- **Linear search**: `findBestScoreByName(String)` scans the array from index 0 and returns the first matching row, which is safe because rows are kept score-ordered, so the first match is that player's best result. `findScoresByName(String)` scans the whole array and returns every matching row, best first. Both compare names case-insensitively (Locale.ROOT) and ignore leading/trailing spaces.
- **Full-board rule**: when the array is full, a new score is accepted only if it is strictly higher than the current lowest row, which it then displaces. A score merely equal to the lowest is rejected.
- **Name handling**: names are stored trimmed. A null or blank name is stored as `Player`. Duplicate names are allowed and kept as separate rows.
- **Display**: the result screen shows the message plus the top rows from the board; the title screen has an optional name box used when recording.
- **Search from the result screen**: the result screen also has a player-name search box next to a SEARCH button. It reuses the board's existing linear search methods exactly (`findBestScoreByName` and `findScoresByName`) — no second search algorithm is implemented anywhere. Queries are matched case-insensitively and ignoring surrounding whitespace because the board normalizes them itself. The first result line reports the player's best score; when the name appears more than once, every matching row is listed below it, best first. A blank query shows a prompt and touches nothing; an unknown name shows `No leaderboard entry for '<name>'.` The search output replaces the top-rows area until the next result screen restores the leaderboard display.

The linear search runs in **O(n)** time for a board of n rows (n is at most `CAPACITY = 10`) and uses **O(1)** auxiliary space: it is a plain front-to-back array scan with one string comparison per row. Deliberately, the name search never uses binary search — the array is sorted by score, not by player name, so a binary search over it would be incorrect.

Completed runs whose scores do not qualify for these ten rows are not lost: the persistent score history (see the persistence section below) keeps every run, and the result screen searches that history instead of the top-10 display.

## Leaderboard persistence (score history + top 10)

`src/com/dungeonescape/game/HighScoreStorage.java` saves scores to a local CSV file and restores them at application start. No database, no external libraries, no network.

**Two score containers, one CSV file, distinct roles:**

- **`HighScoreBoard` — the displayed top-10 leaderboard.** Unchanged: fixed `ScoreEntry[10]` array, hand-written descending insertion sort, full-board qualification rule. It shows and ranks the best ten runs only.
- **`ScoreHistory` — the complete score history.** Hand-written growing array (explicit capacity-doubling copy loop, no collection classes) that keeps **every** completed run — including runs whose scores do not qualify for the top 10 and repeated runs by the same player. It implements the same `SearchableScores` interface as the board (`findBestScoreByName` / `findScoresByName`, linear scans, case-insensitive matching ignoring surrounding whitespace), so the result screen's search box searches the history: a player whose best score is buried outside the displayed top 10 is still found after a restart. Multiple matches are listed best score first, equal scores keeping run order, and the best-score answer is the player's highest run of all time because every run is in the history.
- **`SearchableScores`** is the small interface both containers implement; `ResultPanel` depends on it only, so the search UI is identical either way and no second search algorithm exists in the UI.

**Save-file location**: `dungeon-escape-scores.csv` inside the user's home directory (`System.getProperty("user.home")` — e.g. `C:\Users\<name>\dungeon-escape-scores.csv`); see `HighScoreStorage.defaultFile()`.

**File format**: a `player,score` header followed by one row per completed run, all of them — not just the top 10. Nothing is ever lost when adding a new run: startup loads the whole file into the history, the once-per-run record appends one row, and the save rewrites the file from the complete history.

**When it runs**: loading happens once in `GameWindow`'s constructor — first the history is restored, then the top-10 board is rebuilt from it by calling `HighScoreBoard.record` per run, so the existing insertion sort, capacity rule, and full-board threshold reproduce the ranking. Saving happens once per finished run, immediately after the histories record inside `showResult` — a path reached only after gameplay stops, never while the player is moving. The window records exactly once per run (the game view notifies its flow listener once), so refreshing or re-showing the result card never duplicates rows or saves twice.

**CSV escaping**: a player name containing a comma, quote, or line break is wrapped in double quotes with embedded quotes doubled (`"` → `""`), and the loader parses quoted fields back correctly, so names with special characters survive round trips unchanged.

**Failure safety**: a missing file loads as an empty history (and then an empty top 10). Malformed records — blank rows, wrong column counts, non-numeric scores, the header itself — are skipped while valid rows around them still load. I/O errors on load return 0 and on save return `false` (with a message on the error stream) without any exception escaping to the game.

## Recursive reachability check (DFS)

`src/com/dungeonescape/game/DungeonReachability.java` is a pure, Swing-free algorithm utility over the dungeon tile grid.

Purpose and API:

- `isReachable(Dungeon, startCol, startRow, targetCol, targetRow)` — true when a walkable path connects two tiles.
- `isFullyConnected(Dungeon, startCol, startRow)` — true when every walkable tile can be reached from one tile, i.e. the layout is a single connected region.
- The core recursion is the package-private `floodFill(Dungeon, boolean[][] visited, col, row)`, visible to the test class so its behaviour can be asserted directly.

How the recursion works: `floodFill` marks its tile in the `visited` matrix, then calls itself once for each of the four orthogonal neighbours (right, left, down, up). Each call first checks its base cases — coordinate outside the grid, tile already visited, or tile solid (`'#'` wall or `'T'` torch block) — and returns without recursing in those cases. Walls, torches, and out-of-bounds coordinates are therefore treated as blocked.

How visited tiles prevent infinite recursion: without the visited markers, a grid with cycles (any open rectangular region has loops) would let the same tile be entered endlessly and overflow the call stack. Marking a tile before recursing guarantees each tile is entered at most once, so the search terminates on any grid. The tests demonstrate this directly: a second `floodFill` call over an already-marked region returns a count of 0 newly marked tiles.

Complexity for a grid of R rows and C columns: DFS runs in **O(R × C)** time because each tile is processed at most once. Auxiliary space is also **O(R × C)**: the `boolean[R][C]` visited matrix plus the worst-case recursion stack, which can hold roughly R × C frames when the walkable region is one long corridor.

Relationship to gameplay: the checker **is consulted by the running game**. `Levels.create()` calls `Levels.validateReachableObjectives(...)` on every level it builds, scanning each level's key, chest, potion, and exit-door tiles with the recursive flood fill and throwing `IllegalStateException`, in the same fail-fast style as the dungeon layout validator, if an objective sits in a region the player can never reach. For current layouts this always passes (verified in `DungeonReachabilityTest` and `LevelsReachabilityTest`), so gameplay behaviour is unchanged; the wire-up guarantees future layout edits that strand objectives are caught at game start, not discovered mid-run. `DungeonReachabilityTest` additionally proves every real level stays **fully connected** via `isFullyConnected`.

## Undo and move history (stack)

`src/com/dungeonescape/game/MoveHistory.java` is a hand-written, explicitly array-based LIFO stack — deliberately not `java.util.Stack`, a `Deque`, or any collection wrapper, so the stack mechanics live in project source.

- **Storage**: one plain `Position[64]` array (nested `Position` class holds the player's x/y pixels) with an explicit `top` index. The capacity constant is `MoveHistory.CAPACITY = 64`.
- **Operations**: `push(x, y)` and `pop()` both run in **O(1)**. When full, the stack keeps the most recent positions: the oldest entry falls out of slot 0 because an explicit loop shifts every remaining entry one slot toward index 0 (O(capacity), rare). `pop()` returns `null` on an empty stack, keeping an undo on empty history safe — no crash, no unwanted movement.
- **Recording rule**: `GameSession` captures the player's position before each movement tick and pushes it only when that movement carried the player onto a **new tile**. Holding a key into a wall, or any blocked attempt, leaves the tile unchanged and records nothing. Movement inside a single tile — such as wall-hugging slides — is also deliberately not recorded, so undo granularity is tile-level.
- **Undo (Z key)**: `GameSession.undoLastMove()` pops the most recent entry, restores that exact previous position by replacing the player object, and returns whether anything was restored. **Only the position is rewound**: score, held key, opened chest/door, lives, invulnerability, and monster positions are deliberately untouched, so undo cannot re-earn chest gold or rewind rule outcomes. Undo is refused while the exit-door opening animation runs or after the run has ended (game over / victory), because stepping back then would skip level state.
- **Safety**: every recorded position is one the player actually occupied during normal play — by construction collision-free and inside the dungeon — so restoring it can never put the player inside a wall or out of bounds (asserted by tests).
- **History clearing**: history is cleared on game start and on every **level transition**, because coordinates are only valid within one layout. It is kept across a same-level death respawn, where every recorded position is still a legal tile of the same level.
- **Wire-up**: the Z binding lives on the window's root pane (`GameWindow.installUndoKeyBinding`), so the dungeon panel's own tested key bindings are untouched. The title screen shows a small "Press Z to undo your last step" hint.

## Pending audio cues (FIFO queue)

`src/com/dungeonescape/game/GameEventQueue.java` is a hand-written FIFO queue implemented as a circular buffer over one fixed-size plain array with explicit `front`, `rear`, and `size` fields — deliberately not `java.util.Queue`, a `LinkedList`, or any collection wrapper.

- **Operations**: `enqueue` and `dequeue` both run in **O(1)**; the indexing `(index + 1) % capacity` wraps around the array so slots freed by dequeues are reused. Space is one fixed allocation: **O(capacity)**, never growing.
- **Bounded**: default capacity 16 (`GameEventQueue.CAPACITY`). When full, `enqueue` silently rejects the newest cue (returns `false`) and leaves every stored cue untouched — no corruption, no overwriting of entries already waiting. Empty `dequeue` safely returns `null`; null/blank cues are rejected too.
- **Integration — why audio cues**: `GameSession` already produced an ordered list of one-shot sound cue names (`"footstep"`, `"chest"`, `"key"`, `"potion"`, `"door"`, `"monster"`, `"gameover"`, `"victory"`) inside a single update tick. Routing them through the FIFO preserves that generation order exactly, separates rule logic from sound delivery, and bounds memory. Delivery (`GameSession.deliverPendingEvents`) drains the whole queue at the end of each tick — and after the early-return paths that record a cue (fatal blow, door-open animation) — so no cue survives a tick undelivered. Delivery is a small batch of non-blocking `Clip.start()` calls on the EDT the loop already runs on: no extra threads, nothing blocks, and player input handling is untouched. No gameplay, collision, scoring, monster, leaderboard, search, undo, or reachability logic changed.
- **Tests**: `GameEventQueueTest` covers the empty queue, FIFO order, interleaved enqueue/dequeue, circular wraparound, the full-queue rejection policy, size/empty accuracy, null/blank rejection, 1000 repeat cycles, and the session-level FIFO delivery of cues (including game-over and victory ordering).

## Build

Open Windows CMD in the project directory:

cd /d C:\Users\www\Desktop\DungeonEscapeV2

Compile all application source files:


if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -Xlint:all -d out @sources.txt


## Run

After compiling, launch the game:


java -cp out com.dungeonescape.Main


The game opens in a window with a title screen. Start a game and progress through the available levels.

Keep the `assets/` and `audio/` directories alongside the project files so the game can load its images and sound effects.

## Run the tests

Compile the test source files:

if not exist test_out mkdir test_out
dir /s /b test\*.java > test_sources.txt
javac -Xlint:all -cp out -d test_out @test_sources.txt


Run all thirteen test classes:

java -cp "out;test_out" com.dungeonescape.asset.AudioManagerTest
java -cp "out;test_out" com.dungeonescape.game.DungeonReachabilityTest
java -cp "out;test_out" com.dungeonescape.game.GameEventQueueTest
java -cp "out;test_out" com.dungeonescape.game.GameSessionTest
java -cp "out;test_out" com.dungeonescape.game.HighScoreBoardTest
java -cp "out;test_out" com.dungeonescape.game.HighScoreStorageTest
java -cp "out;test_out" com.dungeonescape.game.LevelsReachabilityTest
java -cp "out;test_out" com.dungeonescape.game.MoveHistoryTest
java -cp "out;test_out" com.dungeonescape.game.PlayerTest
java -cp "out;test_out" com.dungeonescape.game.ScoreHistoryTest
java -cp "out;test_out" com.dungeonescape.ui.InputBindingTest
java -cp "out;test_out" com.dungeonescape.ui.ResultSearchTest
java -cp "out;test_out" com.dungeonescape.ui.ScreensTest


Each test class reports its results in the terminal. A successful run ends with `All tests passed.`

The tests cover audio management, player movement, gameplay session behavior, keyboard bindings, title/result screen interactions, the high-score leaderboard's array storage, insertion sort ordering, capacity limit, linear search by player name, leaderboard CSV persistence (round trips, special characters, missing and malformed files), the complete score history keeping every run (even outside the top 10) searchable, the result-screen leaderboard search reuse, the level-build reachability gate over dungeon grids, the recursive DFS reachability checker, the move-history stack with its undo action, and the FIFO game-event queue and its audio-cue delivery.

## Technology

- Java 21
- Java Swing and AWT
- Java Sound API
- Plain `javac` compilation
- Dependency-free test harness

## Project scope

Dungeon Escape is a standalone Java game project. It does not require a database, server, or third-party runtime library.

The earlier `DungeonEscape` project is separate from this `DungeonEscapeV2` repository.
