package com.chess;

import com.chess.hardware.BoardScanner;
import com.chess.hardware.LedController;
import com.chess.input.ConsoleInputHandler;
import com.chess.input.EngineInputHandler;
import com.chess.input.InputHandler;
import com.chess.input.SensorInputHandler;
import com.chess.input.StartupMenu;
import com.chess.model.Color;
import com.chess.moves.MoveValidator;

public class Main
{
    // Serial port the Arduino is connected to
    private static final String SERIAL_PORT = "/dev/ttyUSB0";

    // Path to the Stockfish (or any UCI) binary. "stockfish" works if it's on
    // the PATH (e.g. after `apt install stockfish` on the Pi). On Windows, set
    // the full path, e.g. "C:\\stockfish\\stockfish.exe".
    private static final String ENGINE_PATH = "C:\\stockfish-windows-x86-64-avx2\\stockfish\\stockfish-windows-x86-64-avx2.exe";
    private static final int ENGINE_MOVETIME_MS = 1000;

    public static void main(String[] args)
    {
        String mode = args.length > 0 ? args[0] : "";

        if (mode.equals("--hardware"))
        {
            // Physical board with the on-board menu — runs games back-to-back.
            runHardware();
            return;
        }

        // Single-game modes (console / direct hardware-vs-engine)
        LedController ledController = null;
        EngineInputHandler engine = null;
        InputHandler whiteHandler;
        InputHandler blackHandler;

        if (mode.equals("--hardware-engine"))
        {
            // Physical board vs engine: human plays White on the board,
            // Stockfish plays Black and the human moves its pieces for it
            ledController = new LedController(SERIAL_PORT);
            ledController.connect();
            BoardScanner scanner = new BoardScanner();
            whiteHandler = new SensorInputHandler(scanner, new MoveValidator(), ledController);
            engine = new EngineInputHandler(ENGINE_PATH, ENGINE_MOVETIME_MS, scanner, ledController);
            blackHandler = engine;
        }
        else if (mode.equals("--engine"))
        {
            // Console test: human plays White, Stockfish plays Black
            engine = new EngineInputHandler(ENGINE_PATH, ENGINE_MOVETIME_MS);
            whiteHandler = new ConsoleInputHandler();
            blackHandler = engine;
        }
        else
        {
            // Default: human vs human in the console
            InputHandler console = new ConsoleInputHandler();
            whiteHandler = console;
            blackHandler = console;
        }

        if (engine != null)
            engine.start();

        new Game(whiteHandler, blackHandler).start();

        if (ledController != null)
            ledController.disconnect();
        if (engine != null)
            engine.stop();
    }

    // Physical board: play games back-to-back. After each game ends, control
    // returns to the startup menu, whose board-state check guides the player to
    // reset the pieces to the starting position before the next game begins.
    private static void runHardware()
    {
        LedController led = new LedController(SERIAL_PORT);
        led.connect();
        BoardScanner scanner = new BoardScanner();
        MoveValidator validator = new MoveValidator();
        InputHandler sensor = new SensorInputHandler(scanner, validator, led);
        EngineInputHandler engine = new EngineInputHandler(ENGINE_PATH, ENGINE_MOVETIME_MS, scanner, led);
        boolean engineStarted = false;

        while (true)
        {
            StartupMenu.Result sel = new StartupMenu(scanner, led).run();

            InputHandler whiteHandler;
            InputHandler blackHandler;

            if (sel.mode == StartupMenu.Mode.HUMAN_VS_HUMAN)
            {
                whiteHandler = sensor;
                blackHandler = sensor;
            }
            else
            {
                // Start the engine the first time it's needed, then reuse it.
                if (!engineStarted)
                {
                    engine.start();
                    engineStarted = true;
                }
                engine.newGame();

                if (sel.humanColor == Color.WHITE)
                {
                    whiteHandler = sensor;
                    blackHandler = engine;
                }
                else
                {
                    whiteHandler = engine;
                    blackHandler = sensor;
                }
            }

            new Game(whiteHandler, blackHandler).start();
            System.out.println("[game] Game over — returning to the startup menu.");
        }
    }
}
