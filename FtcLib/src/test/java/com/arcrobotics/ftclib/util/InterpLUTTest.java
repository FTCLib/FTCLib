package com.arcrobotics.ftclib.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class InterpLUTTest {
InterpLUT lut;
    @Test
    public void add() {

    }

    @Test
    public void createLUT() {
        lut = new InterpLUT();
        lut.add(1,1);
        lut.add(2,2);
        lut.add(3,3);
        lut.add(4,4);
        lut.createLUT();
    }

    @Test
    public void get() {
        InterpLUT lut = new InterpLUT();
        lut.add(1,1);
        lut.add(2,2);
        lut.add(3,3);
        lut.add(4,4);
        lut.createLUT();
        assertEquals(2,lut.get(2),0.000001);
    }
}