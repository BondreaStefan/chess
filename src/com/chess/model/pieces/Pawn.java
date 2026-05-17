package com.chess.model.pieces;

import java.util.ArrayList;
import java.util.List;
import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.model.Square;
import com.chess.moves.Move;

public class Pawn extends Piece
{
    public Pawn(Color color, Square position) 
    {
        super(color, position);
     }

    @Override
    public char getSymbol() 
    {
        return color == Color.WHITE ? 'P' : 'p';
    }

    @Override
    public List<Move> getLegalMoves(Board board) 
    {
        List<Move> moves = new ArrayList<>();
        int direction = color == Color.WHITE ? 1 : -1;
        boolean hasMoved = false;

        // Forward move
        int newRow = position.getRow() + direction;
        if(isInBounds(newRow, position.getCol()) && !board.getSquare(newRow, position.getCol()).isOccupied())
        {
            moves.add(new Move(this, this.position, board.getSquare(newRow, position.getCol()), false));
            if(!hasMoved)
            {
                newRow += direction;
                if(isInBounds(newRow, position.getCol()) && !board.getSquare(newRow, position.getCol()).isOccupied())
                {
                    moves.add(new Move(this, this.position, board.getSquare(newRow, position.getCol()), false));
                }
            }
        }

        // Captures
        int[][] offsets = {{direction, -1}, {direction, 1}};
        for(int[] offset : offsets)
        {
            newRow = position.getRow() + offset[0];
            int newCol = position.getCol() + offset[1];
            if(isInBounds(newRow, newCol))
            {
                Square destination = board.getSquare(newRow, newCol);
                if(destination.isOccupied() && isOpponent(destination.getOccupant()))
                {
                    moves.add(new Move(this, this.position, destination, true));
                }
            }
        }

        Pawn enPassantTarget = board.getEnPassantTarget();
        if(enPassantTarget != null)
        {
            if(enPassantTarget.getPosition().getRow() == position.getRow() && 
            Math.abs(enPassantTarget.getPosition().getCol() - position.getCol()) == 1)
            {
                int enPassantCol = enPassantTarget.getPosition().getCol();
                Square destination = board.getSquare(position.getRow() + direction, enPassantCol);
                moves.add(new Move(this, position, destination, true, false, true));
            }
        }
 
        return moves;
    }
        
}