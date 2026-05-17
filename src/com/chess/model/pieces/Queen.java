package com.chess.model.pieces;

import java.util.ArrayList;
import java.util.List;
import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.model.Square;
import com.chess.moves.Move;

public class Queen extends Piece
{
    public Queen(Color color, Square position) 
    {
        super(color, position);
    }

    @Override
    public char getSymbol() 
    {
        return color == Color.WHITE ? 'Q' : 'q';
    }

    @Override
    public List<Move> getLegalMoves(Board board)
    {
        List<Move> moves = new ArrayList<>();
        int[][] directions = {
            { 1,  0},
            {-1,  0},
            { 0,  1},
            { 0, -1},
            { 1,  1},
            { 1, -1},
            {-1,  1},
            {-1, -1}
        };

        for(int[] dir : directions)
        {
            int newRow = position.getRow() + dir[0];
            int newCol = position.getCol() + dir[1];
            while(isInBounds(newRow, newCol))
            {
                Square destination = board.getSquare(newRow, newCol);
                if(destination.isOccupied())
                {
                    if(isOpponent(destination.getOccupant()))
                    {
                        moves.add(new Move(this, this.position, destination, true));
                    }
                    break;
                }
                else
                {
                    moves.add(new Move(this, this.position, destination, false));
                }
                newRow += dir[0];
                newCol += dir[1];
            }
        }
        return moves;
    }
}