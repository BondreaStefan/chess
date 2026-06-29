package com.chess.input;

import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.model.Square;
import com.chess.moves.Move;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * An InputHandler that delegates move selection to a UCI chess engine
 * (e.g. Stockfish) running as a child process.
 */
public class EngineInputHandler implements InputHandler
{
    private final String enginePath;
    private final int moveTimeMs;

    private Process process;
    private BufferedReader reader;
    private Writer writer;

    // Promotion piece parsed from the last bestmove (e.g. 'Q' from "e7e8q").
    // Game queries this separately, after the move, via getPromotionChoice().
    private char promotionChoice = 'Q';

    public EngineInputHandler(String enginePath, int moveTimeMs)
    {
        this.enginePath = enginePath;
        this.moveTimeMs = moveTimeMs;
    }

    /** Launches the engine and performs the UCI handshake. */
    public void start()
    {
        try
        {
            process = new ProcessBuilder(enginePath)
                .redirectErrorStream(true)
                .start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            writer = new OutputStreamWriter(process.getOutputStream());

            send("uci");
            waitFor("uciok");
            send("isready");
            waitFor("readyok");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to start engine: " + enginePath, e);
        }
    }

    /** Tells the engine to quit and tears down the process. */
    public void stop()
    {
        try
        {
            if (writer != null)
                send("quit");
        }
        catch (IOException e)
        {
            // ignore — we're shutting down anyway
        }
        if (process != null)
            process.destroy();
    }

    @Override
    public Move getNextMove(Board board, Color currentTurn)
    {
        try
        {
            send("position fen " + board.getFEN(currentTurn));
            send("go movetime " + moveTimeMs);

            String best = null;
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith("bestmove"))
                {
                    best = line.split("\\s+")[1];
                    break;
                }
            }

            if (best == null || best.equals("(none)"))
                return null;

            System.out.println("Engine plays: " + best);
            return parseMove(best, board);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Engine communication failed", e);
        }
    }

    // Converts a UCI move string ("e2e4", "e7e8q", "e1g1") into a rough Move.
    // Game's MoveValidator.getMatchedMove() then resolves the proper flags
    // (capture / castling / en passant).
    private Move parseMove(String uci, Board board)
    {
        int fromCol = uci.charAt(0) - 'a';
        int fromRow = uci.charAt(1) - '1';
        int toCol   = uci.charAt(2) - 'a';
        int toRow   = uci.charAt(3) - '1';

        promotionChoice = (uci.length() >= 5)
            ? Character.toUpperCase(uci.charAt(4))
            : 'Q';

        Square from = board.getSquare(fromRow, fromCol);
        Square to   = board.getSquare(toRow, toCol);
        return new Move(from.getOccupant(), from, to, to.isOccupied());
    }

    @Override
    public char getPromotionChoice()
    {
        return promotionChoice;
    }

    private void send(String command) throws IOException
    {
        writer.write(command + "\n");
        writer.flush();
    }

    // Reads engine output until a line containing the given token appears.
    private void waitFor(String token) throws IOException
    {
        String line;
        while ((line = reader.readLine()) != null)
        {
            if (line.contains(token))
                return;
        }
    }
}
