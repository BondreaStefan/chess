package com.chess.input;

import com.chess.moves.Move;
import com.chess.model.Board;
import com.chess.model.Color;

public interface InputHandler
{
    Move getNextMove(Board board, Color currentTurn);
    char getPromotionChoice();
}