package com.qualcomm.robotcore.hardware;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * ContinuousRotationServoImpl provides an implementation of continuous
 * rotation servo functionality.
 */
public class CRServoImpl implements CRServo
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected ServoController controller    = null;
    protected int             portNumber    = -1;
    protected Direction       direction     = Direction.FORWARD;

    protected static final double apiPowerMin = -1.0;
    protected static final double apiPowerMax =  1.0;
    protected static final double apiServoPositionMin = 0.0;
    protected static final double apiServoPositionMax = 1.0;

    //------------------------------------------------------------------------------------------------
    // Construction
    //------------------------------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param controller Servo controller that this servo is attached to
     * @param portNumber physical port number on the servo controller
     */
    public CRServoImpl(ServoController controller, int portNumber)
        {
        this(controller, portNumber, Direction.FORWARD);
        }

    /**
     * Constructor
     *
     * @param controller Servo controller that this servo is attached to
     * @param portNumber physical port number on the servo controller
     * @param direction  FORWARD for normal operation, REVERSE to reverse operation
     */
    public CRServoImpl(ServoController controller, int portNumber, Direction direction)
        {
        this.direction = direction;
        this.controller = controller;
        this.portNumber = portNumber;
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice interface
    //----------------------------------------------------------------------------------------------

    @Override
    public Manufacturer getManufacturer()
        {
        return controller.getManufacturer();
        }

    @Override
    public String getDeviceName()
        {
        return AppUtil.getDefContext().getString(R.string.configTypeContinuousRotationServo);
        }

    @Override
    public String getConnectionInfo()
        {
        return controller.getConnectionInfo() + "; port " + portNumber;
        }

    @Override
    public int getVersion()
        {
        return 1;
        }

    @Override
    public synchronized void resetDeviceConfigurationForOpMode()
        {
        this.direction = Direction.FORWARD;
        }

    @Override
    public void close()
        {
        // take no action
        }

    //----------------------------------------------------------------------------------------------
    // ContinuousRotationServo interface
    //----------------------------------------------------------------------------------------------

    @Override
    public ServoController getController()
        {
        return this.controller;
        }

    @Override
    public int getPortNumber()
        {
        return this.portNumber;
        }

    @Override
    public synchronized void setDirection(Direction direction)
        {
        this.direction = direction;
        }

    @Override
    public synchronized Direction getDirection()
        {
        return this.direction;
        }

    @Override
    public void setPower(double power)
        {
        // For CR Servos on MR/HiTechnic hardware, internal positions relate to speed as follows:
        //
        //      0   == full speed reverse
        //      128 == stopped
        //      255 == full speed forward
        //
        if (this.direction == Direction.REVERSE) power = -power;
        power = Range.clip(power, apiPowerMin, apiPowerMax);
        power = Range.scale(power, apiPowerMin, apiPowerMax, apiServoPositionMin, apiServoPositionMax);
        this.controller.setServoPosition(this.portNumber, power);
        }

    @Override
    public double getPower()
        {
        double power = this.controller.getServoPosition(this.portNumber);
        power = Range.scale(power, apiServoPositionMin, apiServoPositionMax, apiPowerMin, apiPowerMax);
        if (this.direction == Direction.REVERSE) power = -power;
        return power;
        }
    }
