package com.arcrobotics.ftclib.purepursuit;

import com.arcrobotics.ftclib.purepursuit.waypoints.EndWaypoint;
import com.arcrobotics.ftclib.purepursuit.waypoints.GeneralWaypoint;
import com.arcrobotics.ftclib.purepursuit.waypoints.PointTurnWaypoint;
import com.arcrobotics.ftclib.purepursuit.waypoints.StartWaypoint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class UtilTest {

    Path m_path;

    @Test
    public void strafeMoveTest() {
        m_path = new Path(
                new StartWaypoint(0, 0),
                new GeneralWaypoint(200, 0, 0, 0.8, 0.8, 30),
                new EndWaypoint(400, 0, 0, 0.5, 0.5,
                        30, 0.8, 1));
        m_path.init();
        double[] motorPowers = m_path.loop(0, 0, 0);
        double[] expected = {1, 0, 0};

        assertArrayEquals(expected, motorPowers, 0.01);
    }

    @Test
    public void verticalMoveTest() {
        m_path = new Path(
                new StartWaypoint(0, 0),
                new GeneralWaypoint(0, 200, 0, 0.8, 0.8, 30),
                new EndWaypoint(0, 400, 0, 0.5, 0.5,
                        30, 0.8, 1));
        m_path.init();
        double[] motorPowers = m_path.loop(0, 0, 0);
        double[] expected = {0, 1, 0};

        assertArrayEquals(expected, motorPowers, 0.01);
    }

    @Test
    public void turnTest() {
        m_path = new Path(
                new StartWaypoint(0, 0),
                new PointTurnWaypoint(0, 0, -Math.PI, 0.8, 1,
                        30, 0.8, 1),
                new EndWaypoint(0, 0, -Math.PI, 0.5, 0.5,
                        30, 0.8, 1));
        m_path.init();
        double[] motorPowers = m_path.loop(0, 0, 0);
        double[] expected = {0, 0, 1};

        assertArrayEquals(expected, motorPowers, 0.01);
    }

}
