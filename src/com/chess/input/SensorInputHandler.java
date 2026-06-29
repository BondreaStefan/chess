package com.chess.input;

import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.model.Square;
import com.chess.model.pieces.King;
import com.chess.model.pieces.Piece;
import com.chess.moves.Move;
import com.chess.moves.MoveValidator;
import com.chess.hardware.BoardScanner;
import com.chess.hardware.LedController;

import java.util.ArrayList;
import java.util.List;

public class SensorInputHandler implements InputHandler
{
    private BoardScanner scanner;
    private MoveValidator moveValidator;
    private LedController ledController;

    private enum InputState
    {
        IDLE,
        PIECE_SELECTED,
        CAPTURE_PENDING,
        WRONG_TURN_LOCKED
    }

    public SensorInputHandler(BoardScanner scanner, MoveValidator moveValidator, LedController ledController)
    {
        this.scanner = scanner;
        this.moveValidator = moveValidator;
        this.ledController = ledController;
    }

    @Override
    public Move getNextMove(Board board, Color currentTurn)
    {
        boolean[][] previousScan = scanner.scan();

        // Before accepting input, ensure the physical board matches the logical state.
        // This catches knocked-off pieces from the previous move (or en passant / castling
        // residue that the player still needs to tidy up physically).
        if (!matchesLogicalBoard(previousScan, board))
        {
            waitForBoardRestore(board);
            previousScan = scanner.scan();
        }

        // If the player to move is in check, light up their king in red.
        // This stays lit (LED commands are additive) until the move completes
        // and clearAll() is sent.
        if (moveValidator.isInCheck(currentTurn, board))
        {
            ledController.showCheck(board.getKing(currentTurn).getPosition());
        }

        InputState state = InputState.IDLE;
        Square selectedFrom        = null;  // square the moving piece was lifted from
        List<Move> legalMoves      = null;  // fully-legal moves for the selected piece
        Square captureTarget       = null;  // opponent square lifted during a capture sequence
        Square wrongTurnSquare     = null;  // square locked during a wrong-turn violation
        Square illegalLanding      = null;  // square where piece was placed illegally

        while (true)
        {
            try { Thread.sleep(50); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            boolean[][] currentScan = scanner.scan();

            // Collect squares that changed since the last scan
            List<Square> disappeared = new ArrayList<>();
            List<Square> appeared    = new ArrayList<>();

            for (int row = 0; row < 8; row++)
            {
                for (int col = 0; col < 8; col++)
                {
                    if (previousScan[row][col] && !currentScan[row][col])
                        disappeared.add(board.getSquare(row, col));
                    else if (!previousScan[row][col] && currentScan[row][col])
                        appeared.add(board.getSquare(row, col));
                }
            }

            switch (state)
            {
                case IDLE:
                {
                    // Expect exactly one square to go empty (piece lifted)
                    if (disappeared.size() == 1 && appeared.isEmpty())
                    {
                        Square lifted = disappeared.get(0);
                        Piece piece   = lifted.getOccupant();

                        if (piece != null && piece.getColor() == currentTurn)
                        {
                            // Correct color lifted — select the piece
                            selectedFrom = lifted;
                            legalMoves   = getFullyLegalMoves(piece, board);
                            ledController.showSelectedPiece(selectedFrom);
                            ledController.showLegalMoves(legalMoves);
                            state = InputState.PIECE_SELECTED;
                        }
                        else if (piece != null)
                        {
                            // Wrong color lifted — flash and lock until restored
                            wrongTurnSquare = lifted;
                            ledController.showIllegal(wrongTurnSquare);
                            state = InputState.WRONG_TURN_LOCKED;
                        }
                    }
                    break;
                }

                case PIECE_SELECTED:
                {
                    if (appeared.size() == 1 && disappeared.isEmpty())
                    {
                        Square placed = appeared.get(0);

                        if (placed == selectedFrom)
                        {
                            // Piece put back on its origin — cancel selection (no-op, same turn)
                            ledController.clearAll();
                            // Re-show the check indicator wiped by clearAll(), so a player
                            // who only peeked at their moves still sees their king in check.
                            if (moveValidator.isInCheck(currentTurn, board))
                            {
                                ledController.showCheck(board.getKing(currentTurn).getPosition());
                            }
                            selectedFrom  = null;
                            legalMoves    = null;
                            illegalLanding = null;
                            state = InputState.IDLE;
                        }
                        else
                        {
                            Move matched = findMoveToSquare(legalMoves, placed);
                            if (matched != null)
                            {
                                // Legal destination — complete the move
                                ledController.clearAll();
                                return matched;
                            }
                            else
                            {
                                // Illegal destination — flash that square, keep waiting
                                illegalLanding = placed;
                                ledController.clearAll();
                                ledController.showSelectedPiece(selectedFrom);
                                ledController.showLegalMoves(legalMoves);
                                ledController.showIllegal(placed);
                            }
                        }
                    }
                    else if (disappeared.size() == 1 && appeared.isEmpty())
                    {
                        Square lifted = disappeared.get(0);

                        if (illegalLanding != null && lifted == illegalLanding)
                        {
                            // Player lifted piece back off the illegal square — back in hand
                            illegalLanding = null;
                            ledController.clearAll();
                            ledController.showSelectedPiece(selectedFrom);
                            ledController.showLegalMoves(legalMoves);
                        }
                        else if (illegalLanding == null)
                        {
                            // While piece is in hand, check if an opponent piece was lifted
                            // for a capture (Option A sequence: lift yours → lift theirs → place)
                            Piece liftedPiece = lifted.getOccupant();
                            if (liftedPiece != null && liftedPiece.getColor() != currentTurn)
                            {
                                Move captureMove = findMoveToSquare(legalMoves, lifted);
                                if (captureMove != null)
                                {
                                    // Valid capture target lifted — wait for placement
                                    captureTarget = lifted;
                                    state = InputState.CAPTURE_PENDING;
                                }
                                // else: accidental lift of a non-capturable opponent piece — ignore
                            }
                        }
                    }
                    break;
                }

                case CAPTURE_PENDING:
                {
                    // Waiting for the moving piece to be placed on the capture square
                    if (appeared.size() == 1 && disappeared.isEmpty())
                    {
                        Square placed = appeared.get(0);
                        if (placed == captureTarget)
                        {
                            // Piece placed on the capture square — move complete
                            Move matched = findMoveToSquare(legalMoves, captureTarget);
                            ledController.clearAll();
                            return matched;
                        }
                        else
                        {
                            // Wrong square during capture — flash it and keep waiting
                            ledController.showIllegal(placed);
                        }
                    }
                    break;
                }

                case WRONG_TURN_LOCKED:
                {
                    // Board is locked — only unlock once the wrongly-lifted piece is put back
                    if (appeared.size() == 1 && appeared.get(0) == wrongTurnSquare && disappeared.isEmpty())
                    {
                        ledController.clearAll();
                        wrongTurnSquare = null;
                        state = InputState.IDLE;
                    }
                    break;
                }
            }

            previousScan = currentScan;
        }
    }

    // Returns only moves that are fully legal (don't leave own king in check)
    private List<Move> getFullyLegalMoves(Piece piece, Board board)
    {
        List<Move> pseudoLegal = piece instanceof King
            ? ((King) piece).getLegalMoves(board, moveValidator)
            : piece.getLegalMoves(board);

        List<Move> legal = new ArrayList<>();
        for (Move move : pseudoLegal)
        {
            if (moveValidator.isValidMove(board, move))
                legal.add(move);
        }
        return legal;
    }

    // Finds the move in the list whose destination matches the given square
    private Move findMoveToSquare(List<Move> moves, Square target)
    {
        for (Move move : moves)
        {
            if (move.getTo() == target)
                return move;
        }
        return null;
    }

    // Returns true if the physical sensor state matches the logical board
    private boolean matchesLogicalBoard(boolean[][] physical, Board board)
    {
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                if (physical[row][col] != board.getSquare(row, col).isOccupied())
                    return false;
            }
        }
        return true;
    }

    // Blocks until the physical board matches the logical board.
    // Flashes any displaced squares to guide the player.
    private void waitForBoardRestore(Board board)
    {
        // Tracks which squares we've currently lit as mismatched, so we only
        // send a command when a square's state actually changes — this lets the
        // flash cycle run smoothly and turns off each square the moment it's fixed.
        boolean[] flashing = new boolean[64];

        while (true)
        {
            boolean[][] physical = scanner.scan();
            boolean restored = true;

            for (int row = 0; row < 8; row++)
            {
                for (int col = 0; col < 8; col++)
                {
                    int idx = row * 8 + col;
                    boolean mismatch = physical[row][col] != board.getSquare(row, col).isOccupied();

                    if (mismatch)
                    {
                        restored = false;
                        if (!flashing[idx])
                        {
                            ledController.showIllegal(board.getSquare(row, col));
                            flashing[idx] = true;
                        }
                    }
                    else if (flashing[idx])
                    {
                        // Square was wrong, now correct — turn off just this LED
                        ledController.clearSquare(board.getSquare(row, col));
                        flashing[idx] = false;
                    }
                }
            }

            if (restored)
                break;

            try { Thread.sleep(100); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        ledController.clearAll();
    }

    @Override
    public char getPromotionChoice()
    {
        return 'Q'; // auto-promote to queen; physical button selection to be added later
    }
}
