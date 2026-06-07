package com.chess.input;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;

import com.chess.model.Board;
import com.chess.model.Color;
import com.chess.moves.Move;
import com.chess.moves.MoveValidator;
import com.chess.hardware.LedController;

public class SensorInputHandler implements InputHandler
{
    // pini de selectie
    static final int S0 = 23;
    static final int S1 = 22;
    static final int S2 = 27;
    static final int S3 = 17;

    // pini sig
    static final int[] SIG = {20, 19, 24, 5};

    // pini en
    static final int[] EN = {21, 26, 25, 6};

    private Context pi4j;
    private DigitalOutput s0, s1, s2, s3;
    private DigitalInput[] sig = new DigitalInput[4];
    private DigitalOutput[] en = new DigitalOutput[4];

    private MoveValidator moveValidator;
    private LedController ledController;

    public SensorInputHandler(MoveValidator moveValidator, LedController ledController)
    {
        this.moveValidator = moveValidator;
        this.ledController = ledController;
        initializePins();
    }

    private void initializePins()
    {
        pi4j = Pi4J.newAutoContext();

        // pini de selectie ca iesiri
        s0 = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
            .id("s0").address(S0).build());
        s1 = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
            .id("s1").address(S1).build());
        s2 = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
            .id("s2").address(S2).build());
        s3 = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
            .id("s3").address(S3).build());

        // pini semnal ca intrari, pini enable ca iesiri
        for(int i = 0; i < 4; i++)
        {
            sig[i] = pi4j.create(DigitalInput.newConfigBuilder(pi4j)
                .id("sig" + i).address(SIG[i])
                .pull(PullResistance.PULL_UP).build());
            en[i] = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
                .id("en" + i).address(EN[i]).build());
        }
    }

    private void selectChannel(int channel)
    {
        s0.setState((channel & 1) != 0);
        s1.setState((channel & 2) != 0);
        s2.setState((channel & 4) != 0);
        s3.setState((channel & 8) != 0);
    }

    private boolean[][] scanBoard()
    {
        boolean[][] state = new boolean[8][8];

        for(int mux = 0; mux < 4; mux++)
        {
            // dezactiveaza toate multiplexoarele
            for(int i = 0; i < 4; i++)
            {
                en[i].high();
            }
            // activeaza multiplexorul curent
            en[mux].low();

            // citeste cele 16 canale
            for(int channel = 0; channel < 16; channel++)
            {
                selectChannel(channel);

                try { Thread.sleep(2); }
                catch(InterruptedException e) { }

                // LOW = piesa prezenta (magnet detectat)
                boolean occupied = sig[mux].state() == DigitalState.LOW;

                // converteste mux+channel in coordonate
                int row = mux * 2 + channel / 8;
                int col = channel % 8;
                state[row][col] = occupied;
            }
        }

        return state;
    }

    @Override
    public Move getNextMove(Board board, Color currentTurn)
    {
        // de implementat — logica de detectie
        return null;
    }

    @Override
    public char getPromotionChoice()
    {
        // de implementat
        return 'Q';
    }
}
