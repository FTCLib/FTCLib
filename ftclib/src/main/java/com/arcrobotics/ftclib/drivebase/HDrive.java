package com.arcrobotics.ftclib.drivebase;

        import com.arcrobotics.ftclib.hardware.Motor;

public class HDrive extends RobotDrive
{
    Motor[] motors;

    /**
     * Constructor for the H-Drive class, which requires at least three motors:
     * @param m1 one of the necessary primary drive motors
     * @param m2 one of the necessary primary drive motors
     * @param slide the necessary slide motor for the use of h-drive
     * @param motor the rest of the motors, potentially the other motors if its 4wd
     */
    public HDrive(Motor m1, Motor m2, Motor slide, Motor... motor)
    {
        this.motors = new Motor[motor.length + 3];
        System.arraycopy(motor, 0, this.motors, 0, motor.length);
        this.motors[motor.length] = m1;
        this.motors[motor.length + 1] = m2;
        this.motors[motor.length + 2] = slide;
    }

    /**
     * Sets the range of the input, see RobotDrive for more info.
     *
     * @param min The minimum value of the range.
     * @param max The maximum value of the range.
     */
    public void setRange(double min, double max) {
        super.setRange(min, max);
    }

    /**
     * Sets the max speed of the drivebase, see RobotDrive for more info.
     *
     * @param value The maximum output speed.
     */
    public void setMaxSpeed(double value) {
        super.setMaxSpeed(value);
    }

    public void stopMotor(){
        for (Motor x : motors) {
            x.stopMotor();
        }
    }

    public void driveFieldCentric(double xSpeed, double ySpeed, double turn, double heading)
    {

    }
}
