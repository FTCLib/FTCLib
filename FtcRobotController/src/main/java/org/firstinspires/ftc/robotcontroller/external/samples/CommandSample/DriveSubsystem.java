package org.firstinspires.ftc.robotcontroller.external.samples.CommandSample;

import com.arcrobotics.ftclib.command.SubsystemBase;
import com.arcrobotics.ftclib.drivebase.DifferentialDrive;
import com.arcrobotics.ftclib.hardware.motors.EncoderEx;
import com.arcrobotics.ftclib.hardware.motors.SimpleMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class DriveSubsystem extends SubsystemBase {

    private final DifferentialDrive m_drive;

    private final EncoderEx m_leftEncoder, m_rightEncoder;

    private final double WHEEL_DIAMETER;

    /**
     * Creates a new DriveSubsystem.
     */
    public DriveSubsystem(SimpleMotorEx leftMotor, SimpleMotorEx rightMotor, final double diameter) {
        m_leftEncoder = new EncoderEx(leftMotor);
        m_rightEncoder = new EncoderEx(rightMotor);

        WHEEL_DIAMETER = diameter;

        m_drive = new DifferentialDrive(leftMotor, rightMotor);
    }

    /**
     * Creates a new DriveSubsystem with the hardware map and configuration names.
     */
    public DriveSubsystem(HardwareMap hMap, final String leftMotorName, String rightMotorName,
                          final double cpr, final double diameter) {
        this(new SimpleMotorEx(leftMotorName, hMap, cpr), new SimpleMotorEx(rightMotorName, hMap, cpr), diameter);
    }

    /**
     * Drives the robot using arcade controls.
     *
     * @param fwd the commanded forward movement
     * @param rot the commanded rotation
     */
    public void drive(double fwd, double rot) {
        m_drive.arcadeDrive(fwd, rot);
    }

    public double getLeftEncoderVal() {
        return m_leftEncoder.getCurrentTicks();
    }

    public double getLeftEncoderDistance() {
        return m_leftEncoder.getNumRevolutions() * WHEEL_DIAMETER * Math.PI;
    }

    public double getRightEncoderVal() {
        return m_rightEncoder.getCurrentTicks();
    }

    public double getRightEncoderDistance() {
        return m_rightEncoder.getNumRevolutions() * WHEEL_DIAMETER * Math.PI;
    }

    public void resetEncoders() {
        m_leftEncoder.resetEncoderCount();
        m_rightEncoder.resetEncoderCount();
    }

    public double getAverageEncoderDistance() {
        return (getLeftEncoderDistance() + getRightEncoderDistance()) / 2.0;
    }

}
