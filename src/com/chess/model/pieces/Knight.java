package com.chess.model.pieces;

import java.util.ArrayList;
import java.util.List;
import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.model.Square;
import com.chess.moves.Move;

public class Knight extends Piece
{
    public Knight(Color color, Square position) 
    {
        super(color, position);
    }

    @Override
    public char getSymbol() 
    {
        return color == Color.WHITE ? 'N' : 'n';
    }

    @Override
    public List<Move> getLegalMoves(Board board) 
    {
        List<Move> moves = new ArrayList<>();
        int[][] offsets = { 
            { 2,  1}, { 2, -1},
            {-2,  1}, {-2, -1},
            { 1,  2}, { 1, -2},
            {-1,  2}, {-1, -2}
        };

        for(int[] offset : offsets) 
        {
            int newRow = position.getRow() + offset[0];
            int newCol = position.getCol() + offset[1];
            if (isInBounds(newRow, newCol)) 
            {
                Square destination = board.getSquare(newRow, newCol);
                if (destination.isOccupied()) 
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
}