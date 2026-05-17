package com.chess.model;

import com.chess.model.pieces.Piece;
import com.chess.model.pieces.Queen;
import com.chess.model.pieces.Rook;
import com.chess.model.pieces.Bishop;
import com.chess.model.pieces.Knight;
import com.chess.model.pieces.Pawn;
import com.chess.model.pieces.King;
import com.chess.moves.Move;

public class Board
{
    private final Square[][] squares;
    private King whiteKing;
    private King blackKing;
    private boolean promotionPending;
    private Square promotionSquare;
    private Pawn enPassantTarget;

    public Board() {
        squares = new Square[8][8];
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col] = new Square(row, col);
            }
        }
    }

    public Square getSquare(int row, int col)
    {
        return squares[row][col];
    }

    public void setupInitialPosition()
    {
        // Place pawns
        for (int col = 0; col < 8; col++) 
        {
            squares[1][col].setOccupant(new Pawn(Color.WHITE, squares[1][col]));
            squares[6][col].setOccupant(new Pawn(Color.BLACK, squares[6][col]));
        }

        // Place other pieces
        Color[] colors = {Color.WHITE, Color.BLACK};
        int[] backRank = {0, 7};
        for (int i = 0; i < backRank.length; i++) 
        {
            Color color = colors[i];
            int row = backRank[i];
            squares[row][0].setOccupant(new Rook(color, squares[row][0]));
            squares[row][1].setOccupant(new Knight(color, squares[row][1]));
            squares[row][2].setOccupant(new Bishop(color, squares[row][2]));
            squares[row][3].setOccupant(new Queen(color, squares[row][3]));
            squares[row][5].setOccupant(new Bishop(color, squares[row][5]));
            squares[row][6].setOccupant(new Knight(color, squares[row][6]));
            squares[row][7].setOccupant(new Rook(color, squares[row][7]));
        }

        //Place kings
        whiteKing = new King(Color.WHITE, squares[0][4]);
        blackKing = new King(Color.BLACK, squares[7][4]);
        squares[0][4].setOccupant(whiteKing);
        squares[7][4].setOccupant(blackKing);
    }

    public King getKing(Color color) 
    {
        return color == Color.WHITE ? whiteKing : blackKing;
    }

    public Pawn getEnPassantTarget()
    {
        return enPassantTarget;
    }

    public boolean isPromotionPending() 
    { 
        return promotionPending; 
    }
    public Square getPromotionSquare() 
    { 
        return promotionSquare; 
    }
    

    public void movePiece(Move move)
    {
        Piece piece = move.getPiece();
        Square from = move.getFrom();
        Square to = move.getTo();

        to.setOccupant(piece);
        from.setOccupant(null);
        piece.setPosition(to);
        piece.setHasMoved();

        if(move.isCastling())
        {
            int row = from.getRow();

            if(to.getCol() == 6) // King-side castling
            {
                Piece rook = squares[row][7].getOccupant();
                squares[row][5].setOccupant(rook);
                squares[row][7].setOccupant(null);
                rook.setPosition(squares[row][5]);
                rook.setHasMoved();
            }
            else
            {
                Piece rook = squares[row][0].getOccupant();
                squares[row][3].setOccupant(rook);
                squares[row][0].setOccupant(null);
                rook.setPosition(squares[row][3]);
                rook.setHasMoved();
            }
        }

        if(piece instanceof Pawn && Math.abs(to.getRow() - from.getRow()) == 2)
        {
            enPassantTarget = (Pawn) piece;
        }
        else
        {
            enPassantTarget = null;
        }

        if(move.isEnPassant())
        {
            squares[from.getRow()][to.getCol()].setOccupant(null);
        }

        if(piece instanceof Pawn)
        {
            int backRank = piece.getColor() == Color.WHITE ? 7 : 0;
            if(to.getRow() == backRank)
            {
                promotionPending = true;
                promotionSquare = to;
            }
            else
            {
                promotionPending = false;
                promotionSquare = null;        
            }
        }
        else
        {
            promotionPending = false;
            promotionSquare = null;        
        }   
    }

    public void promotePawn(Square square, Color color, char choice)
    {
        Piece promoted;
        switch(choice)
        {
            case 'Q': promoted = new Queen(color, square); break;
            case 'R': promoted = new Rook(color, square); break;
            case 'B': promoted = new Bishop(color, square); break;
            case 'N': promoted = new Knight(color, square); break;
            default:  promoted = new Queen(color, square); break;
        }
        square.setOccupant(promoted);
    }

    public String getFEN(Color currentTurn)
    {
        StringBuilder fen = new StringBuilder();

        // 1. piece placement — rank 8 down to rank 1
        for(int row = 7; row >= 0; row--)
        {
            int emptyCount = 0;
            for(int col = 0; col < 8; col++)
            {
                Piece piece = squares[row][col].getOccupant();
                if(piece == null)
                {
                    emptyCount++;
                }
                else
                {
                    if(emptyCount > 0)
                    {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(piece.getSymbol());
                }   
            }
            if(emptyCount > 0)
            {
                fen.append(emptyCount);
            }
            if(row > 0)
            {
                fen.append('/');
            }
        }

        // 2. active color
        fen.append(currentTurn == Color.WHITE ? " w " : " b ");

        // 3. castling availability
        StringBuilder castling = new StringBuilder();
        if(!whiteKing.hasMoved())
        {
            Piece kingSideRook = squares[0][7].getOccupant();
            Piece queenSideRook = squares[0][0].getOccupant();
            if(kingSideRook != null && !kingSideRook.hasMoved()) castling.append('K');
            if(queenSideRook != null && !queenSideRook.hasMoved()) castling.append('Q');
        }
        if(!blackKing.hasMoved())
        {
            Piece kingSideRook = squares[7][7].getOccupant();
            Piece queenSideRook = squares[7][0].getOccupant();
            if(kingSideRook != null && !kingSideRook.hasMoved()) castling.append('k');
            if(queenSideRook != null && !queenSideRook.hasMoved()) castling.append('q');
        }
        fen.append(castling.length() > 0 ? castling : "-");

        // 4. en passant target square
        if(enPassantTarget != null)
        {
            int epRow = enPassantTarget.getColor() == Color.WHITE ? 5 : 2;
            int epCol = enPassantTarget.getPosition().getCol();
            char file = (char)('a' + epCol);
            fen.append(" " + file + (epRow + 1));
        }
        else
        {
            fen.append(" -");
        }

        return fen.toString();
    }
}