package com.chess.hardware;

import com.fazecast.jSerialComm.SerialPort;
import com.chess.model.Square;
import com.chess.moves.Move;
import java.util.List;

public class LedController 
{
    private SerialPort serialPort;
    private String portName;

    public LedController(String portName)
    {
        this.portName = portName;
    }

    public void connect()
    {
        // deschide portul serial la 9600 sau 115200 baud
    }

    public void disconnect()
    {
        // închide portul serial
    }

    public void showLegalMoves(List<Move> moves)
    {
        StringBuilder cmd = new StringBuilder("M:");
        for(int i = 0; i < moves.size(); i++)
        {
            Square to = moves.get(i).getTo();
            cmd.append(squareToIndex(to.getRow(), to.getCol()));
            if(i < moves.size() - 1) 
                cmd.append(",");
        }
        sendCommand(cmd.toString());
    }

    public void showCheck(Square kingSquare)
    {
        sendCommand("K:" + squareToIndex(kingSquare.getRow(), kingSquare.getCol()));
    }

    public void showSelectedPiece(Square square)
    {
        sendCommand("S:" + squareToIndex(square.getRow(), square.getCol()));
    }

    public void clearAll()
    {
        sendCommand("C");
    }

    private void sendCommand(String command)
    {
        if(serialPort != null && serialPort.isOpen())
        {
            byte[] data = (command + "\n").getBytes();
            serialPort.writeBytes(data, data.length);
        }
    }

    private int squareToIndex(int row, int col)
    {
        if(row % 2 == 0)
        {
            return row * 8 + col;
        }
        else
        {
            return row * 8 + (7 - col);
        }
    }
}
