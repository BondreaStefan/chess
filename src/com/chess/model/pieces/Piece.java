package com.chess.model.pieces;

import java.util.List;

import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.model.Square;
import com.chess.moves.Move;

public abstract class Piece
{
    protected final Color color;
    protected Square position;
    protected boolean hasMoved = false;

    public Piece(Color color, Square position) 
    {
        this.color = color;
        this.position = position;
    }

    protected boolean isInBounds(int row, int col) 
    {
        return row >= 0 && row <= 7 && col >= 0 && col <= 7;
    }

    public boolean isOpponent(Piece other) 
    {
        return other != null && this.color != other.color;
    }

    public abstract List<Move> getLegalMoves(Board board);
    {
        //Override
    }
    public abstract char getSymbol();
    {
        //Override
    }

    public Color getColor() 
    {
        return color;
    }

    public void setPosition(Square position) 
    {
        this.position = position;
    }

    public Square getPosition() 
    {
        return position;
    }

    public void setHasMoved() 
    {
        hasMoved = true;
    }

    public boolean hasMoved() 
    {
        return hasMoved;
    }

    @Override
    public String toString() 
    {
        return color + " " + getClass().getSimpleName() + " at " + position;
    }
}