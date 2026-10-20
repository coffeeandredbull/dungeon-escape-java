# DungeonEscapeV2 — Stage 1 Project Foundation

Java 21 Swing project. No build tools, no external libraries. Stage 1 adds only
the minimal foundation: a Swing entry point, a reusable asset loader, and a
startup asset diagnostic. No gameplay yet.

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
│           └── GameWindow.java           # minimal titled window
├── assets/                               # PNG sprites (copied, unchanged)
└── audio/                                # WAV sound effects (copied, unchanged)
```

## How it works
- `Main` prints the asset report, then opens the window on the Swing Event
  Dispatch Thread.
- `AssetLoader` locates the project root by walking up from the working
  directory until it finds a folder containing both `assets/` and `audio/`.
  Paths passed to it are project-root-relative, e.g. `assets/door_closed.png`
  or `audio/drink.wav`.
- Every asset is either reported as loaded/decoded or as failed with the exact
  reason (missing, not a regular file, unreadable, or unsupported format).
  Failures are never silently ignored and an asset is never reported as loaded
  when it failed to load.
- Audio is decoded only to verify the container/format. **No sound is played at
  startup**, and successful decoding is not a claim that playback works.

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
  src\com\dungeonescape\ui\GameWindow.java
```

## Run (Windows CMD)
```cmd
java -cp out com.dungeonescape.Main
```

A titled window opens, and the console prints a per-file report followed by a
summary such as:
```
Summary: images found=47, loaded=47, failed=0 | audio found=7, decoded=7, failed=0
```

## Notes
- The old project `DungeonEscape` is not used, modified, or referenced.
- The copied `assets/` and `audio/` files are left exactly as they were.
