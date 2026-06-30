package com.chess.hardware;

import com.chess.model.Board;

/**
 * Keeps the physical board in sync with the logical board model. Shared by every
 * handler that needs to confirm the pieces on the board match the game state
 * before acting (e.g. after castling or en passant, the human still has to move
 * the rook / remove the captured pawn physically).
 */
public class BoardSync
{
    private final BoardScanner scanner;
    private final LedController led;

    public BoardSync(BoardScanner scanner, LedController led)
    {
        this.scanner = scanner;
        this.led = led;
    }

    /** True if the given scan matches the logical board's occupancy. */
    public boolean matches(boolean[][] scan, Board board)
    {
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++)
                if (scan[row][col] != board.getSquare(row, col).isOccupied())
                    return false;
        return true;
    }

    /**
     * Blocks until the physical board matches the logical board, flashing every
     * mismatched square until it is corrected.
     */
    public void waitUntilMatches(Board board)
    {
        boolean[] flashing = new boolean[64];
        while (true)
        {
            boolean[][] now = scanner.scan();
            boolean ok = true;

            for (int row = 0; row < 8; row++)
            {
                for (int col = 0; col < 8; col++)
                {
                    int idx = row * 8 + col;
                    boolean mismatch = now[row][col] != board.getSquare(row, col).isOccupied();
                    if (mismatch)
                        ok = false;

                    if (mismatch && !flashing[idx])
                    {
                        led.showIllegal(board.getSquare(row, col));
                        flashing[idx] = true;
                    }
                    else if (!mismatch && flashing[idx])
                    {
                        led.clearSquare(board.getSquare(row, col));
                        flashing[idx] = false;
                    }
                }
            }

            if (ok)
                break;

            try { Thread.sleep(100); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        led.clearAll();
    }
}
