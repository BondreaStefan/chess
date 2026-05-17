package com.chess.view;

import com.chess.model.Board;
import com.chess.model.Square;

public class ConsoleRenderer
{
    public void render(Board board)
    {
        for(int row = 7; row >= 0; row--)
        {
            System.out.print((row + 1) + "  ");
            for(int col = 0; col < 8; col++)
            {
                Square square = board.getSquare(row, col);
                if(square.isOccupied())
                {
                    System.out.print(square.getOccupant().getSymbol() + " ");
                }
                else
                {
                    System.out.print(". ");
                }
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("   a b c d e f g h");
    }
}