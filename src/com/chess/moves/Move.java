package com.chess.moves;

import com.chess.model.Square;
import com.chess.model.pieces.Piece;

public class Move
{
    private final Piece piece;
    private final Square from;
    private final Square to;
    private final boolean isCapture;
    private final boolean isCastling;
    private final boolean isEnPassant;

    public Move(Piece piece, Square from, Square to, boolean isCapture) 
    {
        this.piece = piece;
        this.from = from;
        this.to = to;
        this.isCapture = isCapture;
        this.isCastling = false;
        this.isEnPassant = false;
    }

    public Move(Piece piece, Square from, Square to, boolean isCapture, boolean isCastling) 
    {
        this.piece = piece;
        this.from = from;
        this.to = to;
        this.isCapture = isCapture;
        this.isCastling = isCastling;
        this.isEnPassant = false;
    }

    public Move(Piece piece, Square from, Square to, boolean isCapture, boolean isCastling, boolean isEnPassant)
    {
        this.piece = piece;
        this.from = from;
        this.to = to;
        this.isCapture = isCapture;
        this.isCastling = isCastling;
        this.isEnPassant = isEnPassant;
    }
    
    public Piece getPiece() 
    {
        return piece;
    }

    public Square getFrom() 
    {
        return from;
    }
    
    public Square getTo() 
    {
        return to;
    }

    public boolean isCapture() 
    {
        return isCapture;
    }

    public boolean isCastling() 
    {
        return isCastling;
    }

    public boolean isEnPassant() 
    {
        return isEnPassant;
    }

    @Override
    public String toString()
    {
        return piece.getColor() + " " + piece.getClass().getSimpleName() 
            + ": " + from + " -> " + to
            + (isCapture ? " (capture)" : "")
            + (isCastling ? " (castling)" : "")
            + (isEnPassant ? " (en passant)" : "");
    }
}