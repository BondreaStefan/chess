package com.chess;

import com.chess.hardware.LedController;
import com.chess.input.ConsoleInputHandler;
import com.chess.input.InputHandler;

public class Main
{
    // Serial port the Arduino is connected to (e.g. "/dev/ttyUSB0" on Linux)
    private static final String SERIAL_PORT = "/dev/ttyUSB0";

    public static void main(String[] args)
    {
        boolean useHardware = args.length > 0 && args[0].equals("--hardware");

        LedController ledController = null;
        InputHandler inputHandler;

        if (useHardware)
        {
            ledController = new LedController(SERIAL_PORT);
            ledController.connect();
            inputHandler = new com.chess.input.SensorInputHandler(
                new com.chess.moves.MoveValidator(), ledController);
        }
        else
        {
            inputHandler = new ConsoleInputHandler();
        }

        Game game = new Game(inputHandler);
        game.start();

        if (ledController != null)
        {
            ledController.disconnect();
        }
    }
}