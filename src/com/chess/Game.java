package com.chess;

import com.chess.moves.MoveValidator;
import com.chess.view.ConsoleRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.chess.input.ConsoleInputHandler;
import com.chess.input.InputHandler;
import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.model.GameState;
import com.chess.model.pieces.Pawn;
import com.chess.moves.Move;

public class Game
{
    private Board board;
    private MoveValidator moveValidator;
    private InputHandler inputHandler;
    private ConsoleRenderer consoleRenderer;
    private Color currentTurn;
    private GameState state;

    private List<Move> moveHistory = new ArrayList<>();
    private Map<String, Integer> positionCount = new HashMap<>();
    private int halfMoveClock = 0;

    public Game()
    {
        board = new Board();
        board.setupInitialPosition();
        moveValidator = new MoveValidator();
        inputHandler = new ConsoleInputHandler();
        consoleRenderer = new ConsoleRenderer();
        currentTurn = Color.WHITE;
        state = GameState.PLAYING;
    }

    public void start()
    {
        consoleRenderer.render(board);
        while(state == GameState.PLAYING || state == GameState.CHECK)
        {
            Move move = inputHandler.getNextMove(board, currentTurn);
            Move matchedMove = moveValidator.getMatchedMove(board, move);

            if(matchedMove != null && moveValidator.isValidMove(board, matchedMove))
            {
                board.movePiece(matchedMove);
                moveHistory.add(matchedMove);
                updateHalfMoveClock(matchedMove);
                if(board.isPromotionPending())
                {
                    consoleRenderer.render(board);
                    char choice = inputHandler.getPromotionChoice();
                    board.promotePawn(board.getPromotionSquare(), currentTurn, choice);
                }
                currentTurn = currentTurn.opposite();
                recordSnapshot();
                updateGameState();
            }
            else
            {
                System.out.println("Invalid Move");
            }
            consoleRenderer.render(board);
        }
        printMoveHistory();
    }

    private void updateGameState()
    {
        if(moveValidator.isCheckmate(currentTurn, board))
        {
            state = GameState.CHECKMATE;
            System.out.println();
            System.out.println(currentTurn.opposite() + " wins!");
        }
        else if(moveValidator.isStalemate(currentTurn, board))
        {
            state = GameState.STALEMATE;
            System.out.println();
            System.out.println("Draw!");
        }
        else if(moveValidator.isInsufficientMaterial(board, currentTurn))
        {
            state = GameState.DRAW;
            System.out.println("Draw by insufficient material!");
        }
        else if(isThreefoldRepetition())
        {
            state = GameState.DRAW;
            System.out.println();
            System.out.println("Draw by threefold repetition!");
        }
        else if(halfMoveClock >= 100)
        {
            state = GameState.DRAW;
            System.out.println("Draw by fifty move rule!");
        }
        else if(moveValidator.isInCheck(currentTurn, board))
        {
            state = GameState.CHECK;
            System.out.println();
            System.out.println(currentTurn + " is in check!");
        }
        else
        {
            state = GameState.PLAYING;
        }
    }

    private void printMoveHistory()
    {
        System.out.println("\n--- Move History ---");
        for(int i = 0; i < moveHistory.size(); i++)
        {
            System.out.println((i + 1) + ". " + moveHistory.get(i));
        }
    }

    private void recordSnapshot()
    {
        String snapshot = board.getFEN(currentTurn);
        positionCount.put(snapshot, positionCount.getOrDefault(snapshot, 0) + 1);
        // System.out.println("FEN: " + snapshot + " count: " + positionCount.get(snapshot));
    }

    private boolean isThreefoldRepetition()
    {
        String snapshot = board.getFEN(currentTurn);
        return positionCount.getOrDefault(snapshot, 0) >= 3;
    }

    private void updateHalfMoveClock(Move move)
    {
        if(move.isCapture() || move.getPiece() instanceof Pawn)
        {
            halfMoveClock = 0;
        }
        else
        {
            halfMoveClock++;
        }
    }
}