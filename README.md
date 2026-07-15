# ♟️ Chess — Electronic Chessboard

A full Java chess engine paired with a physical, sensor-driven chessboard. Moves made by hand on the board are detected via reed switches, validated by the engine, and reflected back through an addressable LED strip — legal moves glow, illegal moves flash a warning, and a king in check is highlighted directly on the square.

Built as the software/hardware core of a bachelor's thesis on an intelligent chessboard (Raspberry Pi 4 + Arduino Uno).

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Platform](https://img.shields.io/badge/platform-Raspberry%20Pi%204-c51a4a?logo=raspberrypi&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-blue)

## Features

- **Complete chess rules engine** — legal move generation per piece, check/checkmate detection, en passant, and king-safety validation, all from scratch (`model/`, `moves/`).
- **Physical board input** — 64 reed switches read through multiplexers detect piece lift/placement in real time (`hardware/BoardScanner`, `hardware/BoardSync`).
- **LED feedback on the board itself** — legal destinations, the selected piece, check warnings, and illegal-move flashes are all rendered on a WS2812B strip via a simple serial protocol to the Arduino.
- **Multiple play modes** — human vs. human on the console, human vs. Stockfish, or fully on the physical board (with or without the engine).
- **UCI engine integration** — plays against any UCI-compatible engine (tested with Stockfish).
- **Runs as a system service** — a `systemd` unit for unattended startup on the Raspberry Pi.

## How it works

```
┌─────────────────┐        serial (115200 baud)        ┌──────────────┐
│  Raspberry Pi 4  │ ───────────────────────────────────▶ │  Arduino Uno │
│                  │                                      │              │
│  Game engine     │◀──── piece lift / place events ─────│  Reed switch │
│  Move validator  │        (via multiplexers)            │  scan + LEDs │
│  Board state     │                                      └──────┬───────┘
└──────────────────┘                                             │
                                                          WS2812B LED strip
```

The Pi runs the game logic and talks to the Arduino over USB serial with a small command protocol:

| Command | Meaning                                   |
|---------|--------------------------------------------|
| `M:`    | Highlight legal destination squares         |
| `S:`    | Mark the currently selected piece           |
| `K:`    | Flag the king that is in check              |
| `W:`    | Flash a square orange for an illegal move   |
| `C:`    | Clear all LEDs                              |

## Project structure

```
chess/
├── deploy/
│   └── chess.service        # systemd unit for running on the Pi
├── lib/                      # Pi4J, jSerialComm, SLF4J (bundled JARs)
├── src/com/chess/
│   ├── Main.java              # Entry point — selects run mode
│   ├── Game.java               # Game loop
│   ├── hardware/               # Sensor scanning + LED control (Pi ↔ Arduino)
│   ├── input/                  # Console, sensor, engine, and startup-menu input handlers
│   ├── model/                  # Board, squares, colors, pieces
│   │   └── pieces/              # Piece-specific move logic
│   └── moves/                  # Move representation + validation
└── .vscode/
```

## Getting started

### Prerequisites

- Java 21 (JDK)
- A UCI engine on your `PATH` (e.g. [Stockfish](https://stockfishchess.org/)) if you want engine play
- For hardware mode: a Raspberry Pi 4, an Arduino Uno running the matching LED/sensor firmware, and the board wired per the thesis's hardware chapter

### Build

```bash
javac -cp "lib/*" -d out $(find src -name "*.java")
```

### Run

```bash
# Human vs. human, console board
java -cp "out:lib/*" com.chess.Main

# Human vs. Stockfish, console board
java -cp "out:lib/*" com.chess.Main --engine

# Human (physical board) vs. Stockfish
java -cp "out:lib/*" com.chess.Main --hardware-engine

# Fully on the physical board, with an on-board startup menu
java -cp "out:lib/*" com.chess.Main --hardware
```

> On Windows, replace `:` with `;` in the classpath.

Before running an engine mode, set `ENGINE_PATH` in `Main.java` to your engine binary's path.

### Deploying to the Raspberry Pi

```bash
sudo cp deploy/chess.service /etc/systemd/system/chess.service
sudo sed -i "s|__WORKDIR__|$(pwd)|; s|__JAVA__|$(which java)|" /etc/systemd/system/chess.service
sudo systemctl daemon-reload
sudo systemctl enable --now chess.service
```

View live logs with `journalctl -u chess.service -f`.

## License

Distributed under the MIT License. See [`LICENSE`](LICENSE) for details.
