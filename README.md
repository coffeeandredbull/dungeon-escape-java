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
- **Automated tests** using a dependency-free Java test harness.
- **No external libraries or build tools required.**

## Requirements

- JDK 21 or a compatible newer JDK.
- Windows CMD or another terminal that supports Java commands.
- Git, if you want to clone or contribute to the repository.

## Project structure

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
│       │   ├── GameObject.java
│       │   ├── GameSession.java
│       │   ├── InputState.java
│       │   ├── Level.java
│       │   ├── Levels.java
│       │   └── Player.java
│       └── ui/
│           ├── DungeonPanel.java
│           ├── GameFlowListener.java
│           ├── GameWindow.java
│           ├── HudPanel.java
│           ├── ResultPanel.java
│           └── TitlePanel.java
├── test/
│   └── com/dungeonescape/
│       ├── asset/AudioManagerTest.java
│       ├── game/GameSessionTest.java
│       ├── game/PlayerTest.java
│       └── ui/
│           ├── InputBindingTest.java
│           └── ScreensTest.java
├── assets/
├── audio/
└── README.md


## Controls

| Action | Controls |
|---|---|
| Move up | W or Up Arrow |
| Move down | S or Down Arrow |
| Move left | A or Left Arrow |
| Move right | D or Right Arrow |

The game uses keyboard bindings and collision detection to prevent the player from walking through solid walls or outside the dungeon boundaries.

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


Run all five test classes:

java -cp "out;test_out" com.dungeonescape.asset.AudioManagerTest
java -cp "out;test_out" com.dungeonescape.game.GameSessionTest
java -cp "out;test_out" com.dungeonescape.game.PlayerTest
java -cp "out;test_out" com.dungeonescape.ui.InputBindingTest
java -cp "out;test_out" com.dungeonescape.ui.ScreensTest


Each test class reports its results in the terminal. A successful run ends with `All tests passed.`

The tests cover audio management, player movement, gameplay session behavior, keyboard bindings, and title/result screen interactions.

## Technology

- Java 21
- Java Swing and AWT
- Java Sound API
- Plain `javac` compilation
- Dependency-free test harness

## Project scope

Dungeon Escape is a standalone Java game project. It does not require a database, server, or third-party runtime library.

The earlier `DungeonEscape` project is separate from this `DungeonEscapeV2` repository.
