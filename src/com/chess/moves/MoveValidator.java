package com.chess.moves;

import java.util.List;

import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.model.Square;
import com.chess.model.pieces.Piece;

public class MoveValidator
{
    public boolean isValidMove(Board board, Move move)
    {
        Square from = move.getFrom();
        Square to = move.getTo();
        Piece movingPiece = move.getPiece();
        Piece capturedPiece = to.getOccupant();
        List<Move> legalMoves;
        if(movingPiece instanceof com.chess.model.pieces.King)
        {
            legalMoves = ((com.chess.model.pieces.King) move.getPiece()).getLegalMoves(board, this);
        }
        else
        {
            legalMoves = movingPiece.getLegalMoves(board);
        }

        for(Move legalMove : legalMoves)
        {
            if(legalMove.getTo().getRow() == move.getTo().getRow() && legalMove.getTo().getCol() == move.getTo().getCol())
            {
                to.setOccupant(movingPiece);
                from.setOccupant(null);
                movingPiece.setPosition(to);

                boolean inCheck = isInCheck(movingPiece.getColor(), board);

                from.setOccupant(movingPiece);
                to.setOccupant(capturedPiece);
                movingPiece.setPosition(from);

                return !inCheck;
            }
        }
        return false;
    }

    public boolean isSquareUnderAttack(Square square, Color color, Board board)
    {
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                Piece piece = board.getSquare(row, col).getOccupant();
                if (piece != null && piece.getColor() != color)
                {
                    List<Move> enemyMoves = piece.getLegalMoves(board);
                    for (Move enemyMove : enemyMoves)
                    {
                        if (enemyMove.getTo() == square)
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isInCheck(Color color, Board board)
    {
        Piece king = board.getKing(color);
        if(king == null)
        {
            return false;
        }
        return isSquareUnderAttack(king.getPosition(), color, board);
    }

    public boolean isCheckmate(Color color, Board board)
    {
        if (!isInCheck(color, board))
        {
            return false;
        }
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                Piece piece = board.getSquare(row, col).getOccupant();
                if (piece != null && piece.getColor() == color)
                {
                    List<Move> legalMoves;
                    if(piece instanceof com.chess.model.pieces.King)
                    {
                        legalMoves = ((com.chess.model.pieces.King) piece).getLegalMoves(board, this);
                    }
                    else
                    {
                        legalMoves = piece.getLegalMoves(board);
                    }

                    for (Move move : legalMoves)
                    {
                        if (isValidMove(board, move))
                        {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean isStalemate(Color color, Board board)
    {
        if (isInCheck(color, board))
        {
            return false;
        }
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                Piece piece = board.getSquare(row, col).getOccupant();
                if (piece != null && piece.getColor() == color)
                {
                    List<Move> legalMoves;
                    if(piece instanceof com.chess.model.pieces.King)
                    {
                        legalMoves = ((com.chess.model.pieces.King) piece).getLegalMoves(board, this);
                    }
                    else
                    {
                        legalMoves = piece.getLegalMoves(board);
                    }
                    for (Move move : legalMoves)
                    {
                        if (isValidMove(board, move))
                        {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public Move getMatchedMove(Board board, Move move)
    {
        Piece movingPiece = move.getPiece();
        List<Move> legalMoves;

        if(movingPiece instanceof com.chess.model.pieces.King)
        {
            legalMoves = ((com.chess.model.pieces.King) movingPiece).getLegalMoves(board, this);
        }
        else
        {
            legalMoves = movingPiece.getLegalMoves(board);
        }

        for(Move legalMove : legalMoves)
        {
            if(legalMove.getTo().getRow() == move.getTo().getRow() &&
            legalMove.getTo().getCol() == move.getTo().getCol())
            {
                return legalMove;
            }
        }
        return null;
    }

    public boolean isInsufficientMaterial(Board board, Color currentTurn)
    {
        String fen = board.getFEN(currentTurn);
        String pieces = fen.split(" ")[0].replaceAll("[/1-8]", "");

        int whiteCount = 0, blackCount = 0;

        for(char c : pieces.toCharArray())
        {
            if(Character.isUpperCase(c)) 
                whiteCount++;
            else blackCount++;
        }

        // King vs King
        if(whiteCount == 1 && blackCount == 1) 
            return true;

        // King + minor piece vs King
        if(whiteCount == 2 && blackCount == 1)
            return pieces.contains("B") || pieces.contains("N");
        if(blackCount == 2 && whiteCount == 1)
            return pieces.contains("b") || pieces.contains("n");

        // King + Bishop vs King + Bishop — same square color
        if(whiteCount == 2 && blackCount == 2 && pieces.contains("B") && pieces.contains("b"))
            return hasSameColoredBishops(board, currentTurn);

        return false;
    }
    

    private boolean hasSameColoredBishops(Board board, Color currentTurn)
    {
        String fen = board.getFEN(currentTurn);
        String placement = fen.split(" ")[0];

        int whiteBishopSquareColor = -1;
        int blackBishopSquareColor = -1;

        int row = 7; // FEN starts from rank 8
        int col = 0;

        for(char c : placement.toCharArray())
        {
            if(c == '/')
            {
                row--;
                col = 0;
            }
            else if(Character.isDigit(c))
            {
                col += c - '0';
            }
            else
            {
                if(c == 'B') whiteBishopSquareColor = (row + col) % 2;
                if(c == 'b') blackBishopSquareColor = (row + col) % 2;
                col++;
            }
        }
        return whiteBishopSquareColor != -1 && blackBishopSquareColor != -1 && whiteBishopSquareColor == blackBishopSquareColor;
    }
}