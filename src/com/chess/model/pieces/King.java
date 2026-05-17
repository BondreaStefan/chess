package com.chess.model.pieces;

import java.util.ArrayList;
import java.util.List;
import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.model.Square;
import com.chess.moves.Move;
import com.chess.moves.MoveValidator;

public class King extends Piece
{
    public King(Color color, Square position) 
    {
        super(color, position);
    }

    @Override
    public char getSymbol() 
    {
        return color == Color.WHITE ? 'K' : 'k';
    }

    @Override
    public List<Move> getLegalMoves(Board board)
    {
        List<Move> moves = new ArrayList<>();
        int[][] offsets = {
            { 1,  0},
            {-1,  0},
            { 0,  1},
            { 0, -1},
            { 1,  1},
            { 1, -1},
            {-1,  1},
            {-1, -1}
        };

        for(int[] offset : offsets)
        {
            int newRow = position.getRow() + offset[0];
            int newCol = position.getCol() + offset[1];
            if(isInBounds(newRow, newCol))
            {
                Square destination = board.getSquare(newRow, newCol);
                if(destination.isOccupied())
                {
                    if(isOpponent(destination.getOccupant()))
                    {
                        moves.add(new Move(this, this.position, destination, true));
                    }
                }
                else
                {
                    moves.add(new Move(this, this.position, destination, false));
                }
            }
        }
        return moves;
    }

    public List<Move> getLegalMoves(Board board, MoveValidator validator)
    {
        List<Move> moves = getLegalMoves(board);

        if(hasMoved() || validator.isInCheck(color, board))
        {
            return moves;
        }

        int row = position.getRow();
        
        Piece kingSideRook = board.getSquare(row, 7).getOccupant();
        Piece queenSideRook = board.getSquare(row, 0).getOccupant();

        if(kingSideRook != null && !kingSideRook.hasMoved())
        {
            Square fSquare = board.getSquare(row, 5);
            Square gSquare = board.getSquare(row, 6);
            if(!fSquare.isOccupied() && !gSquare.isOccupied() && !validator.isSquareUnderAttack(gSquare, color, board) 
            && !validator.isSquareUnderAttack(fSquare, color, board))
            {
                Move castlingMove = new Move(this, this.position, gSquare, false, true);
                moves.add(castlingMove);
            }
        }

        if(queenSideRook != null && !queenSideRook.hasMoved())
        {
            Square dSquare = board.getSquare(row, 3);
            Square cSquare = board.getSquare(row, 2);
            Square bSquare = board.getSquare(row, 1);
            if(!dSquare.isOccupied() && !cSquare.isOccupied() && !bSquare.isOccupied() && !validator.isSquareUnderAttack(cSquare, color, board) 
            && !validator.isSquareUnderAttack(dSquare, color, board))
            {
                Move castlingMove = new Move(this, this.position, cSquare, false, true);
                moves.add(castlingMove);
            }
        }

        return moves;
    }

}