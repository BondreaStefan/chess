package com.chess.input;

import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.moves.Move;
import com.chess.model.Square;
import com.chess.model.pieces.Piece;

import java.util.Scanner;

public class ConsoleInputHandler implements InputHandler
{
    private Scanner scanner = new Scanner(System.in);

    @Override
    public Move getNextMove(Board board, Color currentTurn)
    {
        while(true)
        {
            System.out.println();
            System.out.print(currentTurn + " move: ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split("\\s+");
            
            if(parts.length != 2)
            {
                System.out.println("Invalid input, try again: ");
                continue;
            }
        
            int fromCol = parts[0].charAt(0) - 'a';
            int fromRow = parts[0].charAt(1) - '1';
            int toCol = parts[1].charAt(0) - 'a';
            int toRow = parts[1].charAt(1) - '1';

            if(fromCol > 7 || fromCol < 0 || fromRow > 7 || fromRow < 0 || toCol > 7 || toCol < 0 || toRow > 7 || toRow < 0)
            {
                System.out.println("Invalid move, out of bounds, try again: ");
                continue;
            }

            Square fromSquare = board.getSquare(fromRow, fromCol);
            Square toSquare = board.getSquare(toRow, toCol);
            Piece piece = fromSquare.getOccupant();

            if(!fromSquare.isOccupied())
            {
                System.out.println("Invalid move, square is empty, try again: ");
                continue;
            }

            if(fromSquare.getOccupant().getColor() != currentTurn)
            {
                System.out.println("Invalid move, wrong colored piece, try again: ");
                continue;
            }

            return new Move(piece, fromSquare, toSquare, toSquare.isOccupied());
        }
    }

    @Override
    public char getPromotionChoice()
    {
        while(true)
        {
            System.out.print("Promote pawn to (Q/R/B/N): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if(input.length() == 1 && "QRBN".contains(input))
            {
                return input.charAt(0);
            }
            System.out.println("Invalid choice, try again.");
        }
    }
}