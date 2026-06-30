package com.chess.input;

import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.model.Square;
import com.chess.moves.Move;
import com.chess.hardware.BoardScanner;
import com.chess.hardware.BoardSync;
import com.chess.hardware.LedController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

/**
 * An InputHandler that delegates move selection to a UCI chess engine
 * (e.g. Stockfish) running as a child process.
 */
public class EngineInputHandler implements InputHandler
{
    private final String enginePath;
    private final int moveTimeMs;

    // Optional hardware collaborators. When present (hardware mode), the engine's
    // move is shown on the board and the human is guided to execute it physically.
    // When null (console mode), getNextMove just returns the computed move.
    private final BoardScanner scanner;
    private final LedController ledController;
    private final BoardSync boardSync;

    private Process process;
    private BufferedReader reader;
    private Writer writer;

    // Promotion piece parsed from the last bestmove (e.g. 'Q' from "e7e8q").
    // Game queries this separately, after the move, via getPromotionChoice().
    private char promotionChoice = 'Q';

    /** Console mode — the engine just computes moves. */
    public EngineInputHandler(String enginePath, int moveTimeMs)
    {
        this(enginePath, moveTimeMs, null, null);
    }

    /** Hardware mode — the engine also displays its move and waits for the human to play it. */
    public EngineInputHandler(String enginePath, int moveTimeMs, BoardScanner scanner, LedController ledController)
    {
        this.enginePath = enginePath;
        this.moveTimeMs = moveTimeMs;
        this.scanner = scanner;
        this.ledController = ledController;
        this.boardSync = (scanner != null && ledController != null)
            ? new BoardSync(scanner, ledController) : null;
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

    /** Resets the engine's internal state for a fresh game. */
    public void newGame()
    {
        try
        {
            send("ucinewgame");
            send("isready");
            waitFor("readyok");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Engine newgame failed", e);
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
        // On the physical board, make sure the human finished executing their own
        // move (e.g. the castling rook, or removing an en-passant-captured pawn)
        // before the engine acts on the position.
        if (boardSync != null && !boardSync.matches(scanner.scan(), board))
            boardSync.waitUntilMatches(board);

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
            Move move = parseMove(best, board);

            // On the physical board, light the move and wait for the human to play it
            if (scanner != null && ledController != null)
                waitForEngineMoveExecution(board, move);

            return move;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Engine communication failed", e);
        }
    }

    // Lights the engine's move (from = blue, to = green) and blocks until the human
    // physically performs it. A piece placed on a square that should stay empty is
    // flashed as an error until removed.
    private void waitForEngineMoveExecution(Board board, Move move)
    {
        boolean[][] start = scanner.scan();
        boolean[][] expected = computeExpected(start, move);

        // Guidance: lift from the blue square, place on the green square
        ledController.showSelectedPiece(move.getFrom());
        ledController.showLegalMoves(List.of(move));

        Square to = move.getTo();
        // On a capture the destination is occupied before and after, so occupancy
        // alone can't confirm the swap. Require the destination to be seen empty at
        // some point first (captured piece removed) before accepting completion.
        // For non-captures it starts empty, so this is satisfied immediately.
        boolean destinationCleared = !start[to.getRow()][to.getCol()];

        boolean[] flashing = new boolean[64];

        while (true)
        {
            boolean[][] now = scanner.scan();

            if (!now[to.getRow()][to.getCol()])
                destinationCleared = true;

            boolean allMatch = true;

            for (int row = 0; row < 8; row++)
            {
                for (int col = 0; col < 8; col++)
                {
                    int idx = row * 8 + col;

                    if (now[row][col] != expected[row][col])
                        allMatch = false;

                    // A piece sitting where it should be empty, that was empty at the
                    // start, is a wrong placement.
                    boolean wrongPlacement = now[row][col] && !expected[row][col] && !start[row][col];

                    // A piece removed from a square that should stay occupied is a wrong
                    // removal (e.g. a piece bumped off by accident). The destination is
                    // excluded since it legitimately goes empty mid-capture.
                    boolean wrongRemoval = !now[row][col] && expected[row][col] && start[row][col]
                        && !(row == to.getRow() && col == to.getCol());

                    boolean wrong = wrongPlacement || wrongRemoval;

                    if (wrong && !flashing[idx])
                    {
                        ledController.showIllegal(board.getSquare(row, col));
                        flashing[idx] = true;
                    }
                    else if (!wrong && flashing[idx])
                    {
                        ledController.clearSquare(board.getSquare(row, col));
                        flashing[idx] = false;
                    }
                }
            }

            if (allMatch && destinationCleared)
                break;

            try { Thread.sleep(100); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        ledController.clearAll();
    }

    // Builds the expected occupancy grid after the move, starting from the current
    // physical state and applying the move's effects (incl. en passant and castling).
    private boolean[][] computeExpected(boolean[][] start, Move move)
    {
        boolean[][] expected = new boolean[8][8];
        for (int row = 0; row < 8; row++)
            expected[row] = start[row].clone();

        Square from = move.getFrom();
        Square to   = move.getTo();
        expected[from.getRow()][from.getCol()] = false;
        expected[to.getRow()][to.getCol()] = true;

        if (move.isEnPassant())
            expected[from.getRow()][to.getCol()] = false;

        if (move.isCastling())
        {
            int row = from.getRow();
            if (to.getCol() == 6)   // king-side: rook h -> f
            {
                expected[row][7] = false;
                expected[row][5] = true;
            }
            else                    // queen-side: rook a -> d
            {
                expected[row][0] = false;
                expected[row][3] = true;
            }
        }
        return expected;
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
