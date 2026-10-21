# Dungeon Escape

A Java 21 dungeon escape game built with Java Swing. Explore dangerous dungeons, avoid monsters, collect items, and progress through three levels.

## Features

- **Three playable levels** with level progression.
- **Player movement and collision detection** against walls and dungeon boundaries.
- **Enemy encounters** with animated monster sprites.
- **Dungeon objects:** keys, chests, doors, potions, torches, and obstacles.
- **Game HUD** displaying gameplay information.
- **Audio system** managed through a dedicated audio manager.
- **Title and result screens** with replay support.
- **Top-10 leaderboard** implemented using an array and insertion sort.
- **Player-name search** using a hand-written linear search.
- **Recursive DFS reachability validation** to detect inaccessible objectives when levels are built.
- **Move-history stack** supporting one-tile undo with the `Z` key.
- **FIFO game-event queue** implemented as a circular buffer for pending audio cues.
- **Persistent score history** saved locally in CSV format, including scores outside the top ten.
- **Automated tests** using a dependency-free Java test harness.

## Technology

| Component | Technology |
|---|---|
| Language | Java 21 |
| Interface | Java Swing and AWT |
| Audio | Java Sound API |
| Compilation | Plain `javac` |
| Testing | Custom dependency-free Java test harness |
| External dependencies | None |

## Controls

| Action | Key |
|---|---|
| Move up | `W` or `↑` |
| Move down | `S` or `↓` |
| Move left | `A` or `←` |
| Move right | `D` or `→` |
| Undo the last tile move | `Z` |

Undo restores the player's previous position. It does not rewind score changes, collected items, lives, or monster positions.

## Project Structure

```text
DungeonEscapeV2/
├── src/
│   └── com/dungeonescape/
│       ├── Main.java
│       ├── asset/
│       │   ├── AssetLoader.java
│       │   ├── AssetLoadException.java
│       │   ├── AssetDiagnostic.java
│       │   ├── AudioInfo.java
│       │   └── AudioManager.java
│       ├── game/
│       │   ├── Dungeon.java
│       │   ├── DungeonReachability.java
│       │   ├── GameEventQueue.java
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
├── assets/
├── audio/
└── README.md
```

## Data Structures and Algorithms

This project uses hand-written data structures and algorithms rather than library implementations for its core DSA features.

### 1. Leaderboard — Array and Insertion Sort

`HighScoreBoard.java` stores the ten highest qualifying runs in a fixed-capacity array.

- Scores are ordered from highest to lowest using insertion sort.
- Equal scores preserve their recording order.
- When the board is full, a new score must exceed the lowest score to qualify.
- Duplicate player names are allowed.

**Complexity:** Insertion sort takes up to \(O(n^2)\) time in the general case. The leaderboard has a maximum of ten entries.

### 2. Player Search — Linear Search

`HighScoreBoard.java` and `ScoreHistory.java` implement player-name searches through the shared `SearchableScores` interface.

- Names are matched case-insensitively.
- Leading and trailing spaces are ignored.
- Multiple runs by the same player can be displayed in score order.
- The result-screen search uses the complete saved history, not just the visible top ten.

**Complexity:** \(O(n)\) time for a search across \(n\) stored entries.

### 3. Recursive DFS — Reachability Validation

`DungeonReachability.java` implements recursive depth-first search using a flood-fill algorithm.

- Explores walkable tiles in four directions.
- Tracks visited tiles to prevent infinite recursion.
- Treats walls and blocked tiles as impassable.
- Checks that keys, chests, potions, and exit doors are reachable when levels are created.
- Rejects invalid layouts through fail-fast validation.

**Complexity:** \(O(R \times C)\) time and auxiliary space for a grid with \(R\) rows and \(C\) columns.

### 4. Stack — Move History and Undo

`MoveHistory.java` implements a LIFO stack using a fixed array.

- Records the player's previous position when they move onto a new tile.
- The `Z` key restores the previous recorded position.
- Blocked movement does not create an undo entry.
- History is cleared when a new game starts or the player changes levels.

**Complexity:** Push and pop are \(O(1)\) normally. Removing the oldest entry when the stack is full requires shifting entries.

### 5. Queue — Pending Game Events

`GameEventQueue.java` implements a FIFO queue using a circular array.

- Preserves the order in which audio cues are generated.
- Reuses array slots through circular indexing.
- Rejects new events when full instead of overwriting queued events.
- Safely handles empty queues and invalid events.

**Complexity:** Enqueue and dequeue are \(O(1)\) time, with \(O(c)\) storage for capacity \(c\).

### 6. Persistent Score History — CSV

`HighScoreStorage.java` saves completed runs to a local CSV file.

- Every completed run is retained, even if it does not reach the top ten.
- The full history is loaded when the game starts.
- The visible leaderboard is rebuilt from the saved history.
- Player names containing commas, quotes, or line breaks are escaped and parsed.
- Malformed records are skipped without discarding other valid records.

**Save location:** `dungeon-escape-scores.csv` in the user's home directory.

The file is local to the computer. No database, server, or network connection is required.

## Getting Started

### Requirements

- JDK 21 or a compatible newer JDK.
- Windows CMD or another terminal that supports Java commands.
- Git is optional if you download the project directly.

Verify Java is installed:

```cmd
java -version
javac -version
```

### Compile

Open CMD in the project directory:

```cmd
cd /d C:\Users\www\Desktop\DungeonEscapeV2
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -Xlint:all -d out @sources.txt
```

### Run the Game

```cmd
java -cp out com.dungeonescape.Main
```

Keep the `assets` and `audio` directories alongside the project so the game can load its resources.

## Run the Tests

Compile the test classes:

```cmd
if not exist test_out mkdir test_out
dir /s /b test\*.java > test_sources.txt
javac -Xlint:all -cp out -d test_out @test_sources.txt
```

Run all thirteen test classes:

```cmd
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
```

Each test class reports its result in the terminal. A successful run ends with `All tests passed.`

The tests cover movement, gameplay sessions, audio, keyboard bindings, screen interactions, leaderboard sorting and searching, CSV persistence, recursive reachability, undo history, and FIFO event delivery.

## Project Scope

Dungeon Escape is a standalone Java game created to demonstrate object-oriented programming, Java Swing, data structures, algorithms, persistence, and automated testing.

It requires no third-party runtime libraries, database, or server.

The earlier `DungeonEscape` project is separate from this `DungeonEscapeV2` repository.
