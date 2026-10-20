# DungeonEscapeV2 — Stage 2 Visual Foundation

Java 21 Swing project. No build tools, no external libraries. Stage 2 adds a
fixed-size dungeon view with an animated torch and a compact HUD on top of the
Stage 1 foundation. No gameplay yet.

## Requirements
- JDK 21 (verified with `javac 21.0.7`, Temurin)
- Windows, plain `javac` / `java` from the command line

## Project layout
```
DungeonEscapeV2/
├── README.md
├── out/                                  # compiled .class files (build output)
├── src/
│   └── com/dungeonescape/
│       ├── Main.java                     # entry point; diagnostics + Swing window
│       ├── asset/
│       │   ├── AssetLoader.java          # loads/validates images and audio
│       │   ├── AssetLoadException.java   # explicit failure type
│       │   ├── AudioInfo.java            # decoded-audio metadata
│       │   └── AssetDiagnostic.java      # per-file found/loaded report
│       └── ui/
│           ├── GameWindow.java           # fixed-size frame (HUD + dungeon)
│           ├── HudPanel.java             # title, level, placeholder score/lives
│           └── DungeonPanel.java         # tile dungeon + animated torches
├── assets/                               # PNG sprites (copied, unchanged)
└── audio/                                # WAV sound effects (copied, unchanged)
```

## How it works
- `Main` prints the Stage 1 asset report, then opens the window on the Swing
  Event Dispatch Thread. Stage 2 leaves `Main` and `AssetLoader` unchanged.
- `GameWindow` composes a `HudPanel` (north) above a `DungeonPanel` (center),
  then packs to a fixed, non-resizable size.
- `DungeonPanel` draws a 16×12 tile grid (`TILE = 48`, panel 768×576). The
  layout is a literal character grid: `#` wall, `.` floor, `T` torch. Outer
  walls, interior wall stubs, and a central block are drawn as beveled stone
  blocks that clearly contrast with the dark stone floor.
- Torch flames use the existing `assets/walltorchflameani1..7.png` frames. All
  frames are loaded once in the constructor through `AssetLoader`; a
  `javax.swing.Timer` advances the frame and repaints, so `paintComponent`
  never touches the file system. Missing/invalid torch frames surface as an
  `AssetLoadException` with the file name and reason.
- `HudPanel` is presentational only: title, level label, and placeholder
  score/lives. No game state is wired up.

## Compile (Windows CMD)
Run from the `DungeonEscapeV2` folder:
```cmd
if not exist out mkdir out
javac -d out -sourcepath src ^
  src\com\dungeonescape\Main.java ^
  src\com\dungeonescape\asset\AssetLoader.java ^
  src\com\dungeonescape\asset\AssetLoadException.java ^
  src\com\dungeonescape\asset\AudioInfo.java ^
  src\com\dungeonescape\asset\AssetDiagnostic.java ^
  src\com\dungeonescape\ui\GameWindow.java ^
  src\com\dungeonescape\ui\HudPanel.java ^
  src\com\dungeonescape\ui\DungeonPanel.java
```

## Run (Windows CMD)
```cmd
java -cp out com.dungeonescape.Main
```

The console prints the same Stage 1 asset report (for example
`images found=47, loaded=47, failed=0 | audio found=7, decoded=7, failed=0`),
and a window titled "Dungeon Escape V2 - Stage 2" opens showing the HUD and
the torch-lit dungeon.

## Notes
- The old project `DungeonEscape` is not used, modified, or referenced.
- The copied `assets/` and `audio/` files are left exactly as they were.
- No sound is played and no gameplay systems exist in this stage.
