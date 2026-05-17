package com.chess.model;

import com.chess.model.pieces.Piece;

public class Square
{
    private final int row;
    private final int col;
    private Piece occupant;

    public Square(int row, int col) 
    {
        this.row = row;
        this.col = col;
        this.occupant = null;
    }

    public boolean isOccupied() 
    {
        return occupant != null;
    }

    public Piece getOccupant() 
    {
        return occupant;
    }

    public void setOccupant(Piece occupant) 
    {
        this.occupant = occupant;
    }

    public int getRow() 
    {
        return row;
    }

    public int getCol() 
    {
        return col;
    }

    @Override
    public String toString() 
    {
        char file = (char) ('a' + col);
        int rank = row + 1;
        return "" + file + rank;
    }
}