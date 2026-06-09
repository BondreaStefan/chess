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
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(115200);
        if (!serialPort.openPort())
        {
            System.err.println("Failed to open serial port: " + portName);
        }
    }

    public void disconnect()
    {
        if (serialPort != null && serialPort.isOpen())
        {
            serialPort.closePort();
        }
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

    public void showIllegal(Square square)
    {
        sendCommand("W:" + squareToIndex(square.getRow(), square.getCol()));
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
