package com.chess.input;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;

import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.model.Square;
import com.chess.model.pieces.King;
import com.chess.model.pieces.Piece;
import com.chess.moves.Move;
import com.chess.moves.MoveValidator;
import com.chess.hardware.LedController;

import java.util.ArrayList;
import java.util.List;

public class SensorInputHandler implements InputHandler
{
    // pini de selectie
    static final int S0 = 23;
    static final int S1 = 22;
    static final int S2 = 27;
    static final int S3 = 17;

    // pini sig
    static final int[] SIG = {20, 19, 24, 5};

    // pini en
    static final int[] EN = {21, 26, 25, 6};

    private Context pi4j;
    private DigitalOutput s0, s1, s2, s3;
    private DigitalInput[] sig = new DigitalInput[4];
    private DigitalOutput[] en = new DigitalOutput[4];

    private MoveValidator moveValidator;
    private LedController ledController;

    private enum InputState
    {
        IDLE,
        PIECE_SELECTED,
        CAPTURE_PENDING,
        WRONG_TURN_LOCKED
    }

    public SensorInputHandler(MoveValidator moveValidator, LedController ledController)
    {
        this.moveValidator = moveValidator;
        this.ledController = ledController;
        initializePins();
    }

    private void initializePins()
    {
        pi4j = Pi4J.newAutoContext();

        // pini de selectie ca iesiri
        s0 = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
            .id("s0").address(S0).build());
        s1 = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
            .id("s1").address(S1).build());
        s2 = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
            .id("s2").address(S2).build());
        s3 = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
            .id("s3").address(S3).build());

        // pini semnal ca intrari, pini enable ca iesiri
        for(int i = 0; i < 4; i++)
        {
            sig[i] = pi4j.create(DigitalInput.newConfigBuilder(pi4j)
                .id("sig" + i).address(SIG[i])
                .pull(PullResistance.PULL_UP).build());
            en[i] = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
                .id("en" + i).address(EN[i]).build());
        }
    }

    private void selectChannel(int channel)
    {
        s0.setState((channel & 1) != 0);
        s1.setState((channel & 2) != 0);
        s2.setState((channel & 4) != 0);
        s3.setState((channel & 8) != 0);
    }

    private boolean[][] scanBoard()
    {
        boolean[][] state = new boolean[8][8];

        for(int mux = 0; mux < 4; mux++)
        {
            // dezactiveaza toate multiplexoarele
            for(int i = 0; i < 4; i++)
            {
                en[i].high();
            }
            // activeaza multiplexorul curent
            en[mux].low();

            // citeste cele 16 canale
            for(int channel = 0; channel < 16; channel++)
            {
                selectChannel(channel);

                try { Thread.sleep(2); }
                catch(InterruptedException e) { }

                // LOW = piesa prezenta (magnet detectat)
                boolean occupied = sig[mux].state() == DigitalState.LOW;

                // converteste mux+channel in coordonate
                int row = mux * 2 + channel / 8;
                int col = channel % 8;
                state[row][col] = occupied;
            }
        }

        return state;
    }

    @Override
    public Move getNextMove(Board board, Color currentTurn)
    {
        boolean[][] previousScan = scanBoard();

        // Before accepting input, ensure the physical board matches the logical state.
        // This catches knocked-off pieces from the previous move (or en passant / castling
        // residue that the player still needs to tidy up physically).
        if (!matchesLogicalBoard(previousScan, board))
        {
            waitForBoardRestore(board);
            previousScan = scanBoard();
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

            boolean[][] currentScan = scanBoard();

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
        boolean restored = false;
        while (!restored)
        {
            boolean[][] physical = scanBoard();
            restored = matchesLogicalBoard(physical, board);

            if (!restored)
            {
                for (int row = 0; row < 8; row++)
                {
                    for (int col = 0; col < 8; col++)
                    {
                        if (physical[row][col] != board.getSquare(row, col).isOccupied())
                            ledController.showIllegal(board.getSquare(row, col));
                    }
                }
            }

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
