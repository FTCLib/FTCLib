package com.arcrobotics.ftclib.hardware.motors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncoderTest {

    private MockMotor motor;
    private MockMotor.MockEncoder encoder;

    public static final double CONSTANT = 10;

    @Test
    @BeforeEach
    public void testCreateEncoder() {
        motor = new MockMotor();
        encoder = motor.encoder.setDistancePerPulse(CONSTANT);
    }

    @Test
    public void testGetValue() {
        assertEquals(0, encoder.getPosition());
        assertEquals(1, encoder.getPosition());
        assertEquals(20, encoder.getDistance());
        assertEquals(3, motor.getCurrentPosition());
    }

    private class MockMotor {
        private int position;
        private MockEncoder encoder;

        private class MockEncoder {
            private Supplier<Integer> m_supplier;
            private double dpp;

            public MockEncoder(Supplier<Integer> supplier) {
                m_supplier = supplier;
            }

            public int getPosition() {
                return m_supplier.get();
            }

            public MockEncoder setDistancePerPulse(double distancePerPulse) {
                dpp = distancePerPulse;
                return this;
            }

            public double getDistance() {
                return dpp * getPosition();
            }
        }

        public MockMotor() {
            encoder = new MockEncoder(this::getCurrentPosition);
        }

        public int getCurrentPosition() {
            return position++;
        }
    }

}
