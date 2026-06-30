package com.chess.input;

import com.chess.hardware.BoardScanner;
import com.chess.hardware.LedController;
import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.model.Square;

/**
 * Board-driven startup menu. The player chooses the game mode and their color by
 * placing king(s) on the two highlighted centre squares:
 *   - white king on e4   -> play White vs the engine
 *   - black king on d5   -> play Black vs the engine
 *   - both kings placed  -> human vs human
 * After the first king is placed, a 5s window waits for the second king; if it
 * doesn't arrive, the game is human vs engine with the placed king's color.
 */
public class StartupMenu
{
    public enum Mode { HUMAN_VS_HUMAN, HUMAN_VS_ENGINE }

    public static class Result
    {
        public final Mode mode;
        public final Color humanColor;   // only meaningful for HUMAN_VS_ENGINE

        public Result(Mode mode, Color humanColor)
        {
            this.mode = mode;
            this.humanColor = humanColor;
        }
    }

    // e4 = (row 3, col 4) -> white king target
    // d5 = (row 4, col 3) -> black king target
    private static final int E4_ROW = 3, E4_COL = 4;
    private static final int D5_ROW = 4, D5_COL = 3;
    private static final long SECOND_KING_WAIT_MS = 5000;

    private final BoardScanner scanner;
    private final LedController led;
    private final Board reference;
    private final boolean[][] initialOccupancy;

    public StartupMenu(BoardScanner scanner, LedController led)
    {
        this.scanner = scanner;
        this.led = led;
        this.reference = new Board();
        this.reference.setupInitialPosition();
        this.initialOccupancy = new boolean[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                initialOccupancy[r][c] = reference.getSquare(r, c).isOccupied();
    }

    public Result run()
    {
        // 1. Ensure the board is set up in the standard starting position.
        System.out.println("[menu] Set up all pieces in the starting position...");
        checkBoardState();
        System.out.println("[menu] Board ready.");

        // 2. Let the player choose via the kings.
        System.out.println("[menu] Choose: white king -> e4 (play White), "
            + "black king -> d5 (play Black), or both kings = human vs human.");
        Result result = selectViaKings();
        System.out.println("[menu] Selected: " + result.mode
            + (result.mode == Mode.HUMAN_VS_ENGINE ? " (human plays " + result.humanColor + ")" : ""));

        // 3. Put the kings back — the board must be standard before the game starts.
        System.out.println("[menu] Put the kings back; game starts when the board is set up.");
        checkBoardState();

        return result;
    }

    // Blocks until the physical board matches the standard starting position,
    // flashing every mismatched square until it is corrected.
    private void checkBoardState()
    {
        boolean[] flashing = new boolean[64];
        while (true)
        {
            boolean[][] now = scanner.scan();
            boolean ok = true;

            for (int r = 0; r < 8; r++)
            {
                for (int c = 0; c < 8; c++)
                {
                    int idx = r * 8 + c;
                    boolean mismatch = now[r][c] != initialOccupancy[r][c];
                    if (mismatch)
                        ok = false;

                    if (mismatch && !flashing[idx])
                    {
                        led.showIllegal(reference.getSquare(r, c));
                        flashing[idx] = true;
                    }
                    else if (!mismatch && flashing[idx])
                    {
                        led.clearSquare(reference.getSquare(r, c));
                        flashing[idx] = false;
                    }
                }
            }

            if (ok)
                break;

            sleep(100);
        }
        led.clearAll();
    }

    private Result selectViaKings()
    {
        Square e1 = reference.getSquare(0, 4);          // white king home (lift this)
        Square e8 = reference.getSquare(7, 4);          // black king home (lift this)
        Square e4 = reference.getSquare(E4_ROW, E4_COL); // white king target
        Square d5 = reference.getSquare(D5_ROW, D5_COL); // black king target

        long firstKingTime = 0;   // millis when the first king landed; 0 = none yet

        while (true)
        {
            // Re-assert the guidance every cycle. A single send right after the
            // board-check clear can be dropped by the Arduino while it refreshes
            // the strip, so re-sending keeps the four squares reliably lit:
            // the two kings to lift (e1, e8) and where to place them (e4, d5).
            led.showSelectedPiece(e1);
            led.showSelectedPiece(e8);
            led.showSelectedPiece(e4);
            led.showSelectedPiece(d5);

            boolean[][] now = scanner.scan();
            boolean white = now[E4_ROW][E4_COL];
            boolean black = now[D5_ROW][D5_COL];

            if (white && black)
            {
                led.clearAll();
                return new Result(Mode.HUMAN_VS_HUMAN, Color.WHITE);
            }

            if (white || black)
            {
                // One king is down — start (or check) the 5s window for the second.
                if (firstKingTime == 0)
                    firstKingTime = System.currentTimeMillis();
                else if (System.currentTimeMillis() - firstKingTime >= SECOND_KING_WAIT_MS)
                {
                    led.clearAll();
                    return new Result(Mode.HUMAN_VS_ENGINE, white ? Color.WHITE : Color.BLACK);
                }
            }
            else
            {
                // Both squares empty again (player removed the king) — reset the wait.
                firstKingTime = 0;
            }

            sleep(100);
        }
    }

    private void sleep(long ms)
    {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
