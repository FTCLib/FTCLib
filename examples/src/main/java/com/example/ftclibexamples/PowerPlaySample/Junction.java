package com.example.ftclibexamples.PowerPlaySample;

public enum Junction {
    NONE(0),
    GROUND(100),
    LOW(800),
    MEDIUM(1200),
    HIGH(1600);

    private final int tick;
    Junction(int tick) {
        this.tick = tick;
    }

    public int getTick() {
        return tick;
    }
}
