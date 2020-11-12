package com.arcrobotics.ftclib.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PIDInputOutputTest {
    private PIDController m_controller;

    @BeforeEach
    void setUp() {
        m_controller = new PIDController(0, 0, 0);
    }

    @Test
    void proportionalGainOutputTest() {
        m_controller.setP(4);

        assertEquals(-0.1, m_controller.calculate(0.025, 0), 1e-5);
    }

    @Test
    void integralGainOutputTest() {
        m_controller.setI(4);
        m_controller.setIntegrationBounds(-180,180);

        double out = 0;

        for (int i = 0; i < 5; i++) {
            out = m_controller.calculate(0.025, 0);
        }

        assertEquals(-0.5 * m_controller.getPeriod(), out, 1e-5);
    }

    @Test
    void derivativeGainOutputTest() {
        m_controller.setD(4);

        assertEquals(0, m_controller.calculate(0,0));

        assertEquals(m_controller.calculate(0.025, 0), -0.1 / m_controller.getPeriod(), 0.05);
    }
}