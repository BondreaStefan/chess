package com.chess;

import com.chess.hardware.LedController;
import com.chess.input.ConsoleInputHandler;
import com.chess.input.EngineInputHandler;
import com.chess.input.InputHandler;
import com.chess.input.SensorInputHandler;
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

        LedController ledController = null;
        EngineInputHandler engine = null;
        InputHandler whiteHandler;
        InputHandler blackHandler;

        if (mode.equals("--hardware"))
        {
            // Physical board: two players move pieces by hand
            ledController = new LedController(SERIAL_PORT);
            ledController.connect();
            InputHandler sensor = new SensorInputHandler(new MoveValidator(), ledController);
            whiteHandler = sensor;
            blackHandler = sensor;
        }
        else if (mode.equals("--engine"))
        {
            // Console test: human plays White, Stockfish plays Black
            engine = new EngineInputHandler(ENGINE_PATH, ENGINE_MOVETIME_MS);
            engine.start();
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

        Game game = new Game(whiteHandler, blackHandler);
        game.start();

        if (ledController != null)
            ledController.disconnect();
        if (engine != null)
            engine.stop();
    }
}
