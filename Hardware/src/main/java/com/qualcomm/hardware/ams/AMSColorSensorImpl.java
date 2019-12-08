/*
Copyright (c) 2016 Robert Atkinson & Steve Geffner

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the names of Robert Atkinson nor Steve Geffner nor the names of their contributors
may be used to endorse or promote products derived from this software without specific
prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.qualcomm.hardware.ams;

import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cAddrConfig;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDeviceWithParameters;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchSimple;
import com.qualcomm.robotcore.hardware.I2cWaitControl;
import com.qualcomm.robotcore.hardware.Light;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.Deadline;

import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

/**
 * AMSColorSensorImpl is used to support the Adafruit color sensor:
 *      http://adafru.it/1334
 *      https://www.adafruit.com/products/1334?&main_page=product_info&products_id=1334
 *      https://github.com/adafruit/Adafruit_TCS34725
 *
 * More generally, there is a family of color sensors from AMS which this could support
 *      http://ams.com/eng/Support/Demoboards/Light-Sensors/(show)/145298
 *
 * This implementation sits on top of I2cDeviceSynchSimple instead of I2cDevice.
 * That said, if an I2cDeviceSynch is provided instead, advantage will be taken thereof.
 *
 * This class is declared abstract. Subclasses in user code should add @I2cSensor annotations
 * to allow proper registration and life cycle management.
 */
@SuppressWarnings("WeakerAccess")
public abstract class AMSColorSensorImpl extends I2cDeviceSynchDeviceWithParameters<I2cDeviceSynchSimple, AMSColorSensor.Parameters>
        implements AMSColorSensor, I2cAddrConfig, Light
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "AMSColorSensorImpl";

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected AMSColorSensorImpl(AMSColorSensor.Parameters params, I2cDeviceSynchSimple deviceClient, boolean isOwned)
        {
        super(deviceClient, isOwned, params);
        this.deviceClient.setLogging(this.parameters.loggingEnabled);
        this.deviceClient.setLoggingTag(this.parameters.loggingTag);

        // We ask for an initial call back here; that will eventually call internalInitialize()
        this.registerArmingStateCallback(true);

        this.engage();
        }

    //----------------------------------------------------------------------------------------------
    // Initialization
    //----------------------------------------------------------------------------------------------

    @Override
    protected synchronized boolean internalInitialize(@NonNull Parameters parameters)
        {
        RobotLog.vv(TAG, "internalInitialize()...");
        try {
            if (this.parameters.deviceId != parameters.deviceId)
                {
                throw new IllegalArgumentException(String.format("can't change device types (modify existing params instead): old=%d new=%d", this.parameters.deviceId, parameters.deviceId));
                }

            // Remember the parameters for future use
            this.parameters = parameters.clone();

            // Make sure we're talking to the correct I2c device
            this.setI2cAddress(parameters.i2cAddr);

            // Can't do anything if we're not really talking to the hardware
            if (!this.deviceClient.isArmed())
                {
                return false;
                }

            // Verify that that's a color sensor!
            byte id = this.getDeviceID();
            if (id != parameters.deviceId)
                {
                RobotLog.ee(TAG, "unexpected AMS color sensor chipid: found=%d expected=%d", id, parameters.deviceId);
                return false;
                }

            // sanity check: before
            dumpState();

            // Turn off the integrator so that we can change parameters
            disable();

            // Set the gain and integration time. Note that we don't seem to be
            // able to successfully set these values AFTER we enable, so we need to
            // do so before.
            setIntegrationTime(parameters.atime);
            setGain(parameters.gain);
            setPDrive(parameters.ledDrive);

            if (is3782() && parameters.useProximityIfAvailable)
                {
                setProximityPulseCount(parameters.proximityPulseCount);
                }

            // Enable the device
            enable();

            // sanity check: after
            dumpState();

            // Set up a read-ahead, if supported and requested
            if (this.deviceClient instanceof I2cDeviceSynch && parameters.readWindow != null)
                {
                I2cDeviceSynch synch = ((I2cDeviceSynch)this.deviceClient);
                synch.setReadWindow(parameters.readWindow);
                }

            return true;
            }
        finally
            {
            RobotLog.vv(TAG, "...internalInitialize()");
            }
        }

    protected void dumpState()
        {
        int cb = 0x19;
        RobotLog.logBytes(TAG, "state", read(Register.ENABLE, cb), cb);
        }

    protected synchronized void enable()
        {
        byte enabled = readEnable();
        RobotLog.vv(TAG, "enable() enabled=0x%02x...", enabled);

        boolean needPON = !testBits(enabled, Enable.PON);
        boolean wantAEN = true;
        boolean needAEN = wantAEN && (needPON || !testBits(enabled, Enable.AEN));
        boolean wantPEN = is3782() && parameters.useProximityIfAvailable;
        boolean needPEN = wantPEN && !testBits(enabled, Enable.PEN);

        if (needPON)
            {
            writeEnable(Enable.PON.bVal);
            }

        // Paranoia: just in case PON only recently auto-started. Spec says we must wait
        // 2.4 ms after that occurs. Simplest if we just always do that.
        //  "There is a 2.4 ms warm-up delay if PON is enabled"
        delay(3);

        if (needAEN || needPEN)
            {
            writeEnable(Enable.PON.bVal | (needAEN ? Enable.AEN.bVal : 0) | (needPEN ? Enable.PEN.bVal : 0));
            }

        enabled = readEnableAfterWrite();

        RobotLog.vv(TAG, "...enable() enabled=0x%02x", enabled);
        }

    protected synchronized void disable()
        {
        byte enabled = readEnable();
        RobotLog.vv(TAG, "disable() enabled=0x%02x...", enabled);

        writeEnable(enabled & ~(Enable.PON.bVal | Enable.AEN.bVal | Enable.PEN.bVal));
        enabled = readEnableAfterWrite();

        RobotLog.vv(TAG, "...disable() enabled=0x%02x", enabled);
        }

    /** Return whether we know we are enabled and still actively able to talk to the device */
    protected boolean isConnectedAndEnabled()
        {
        // If we're physically disconnected, then readEnable() will return fake data. Which
        // will all be zero. Which won't have the PON bit set.
        byte enabled = readEnable();
        return testBits(enabled, Enable.PON);
        }

    protected boolean testBits(byte value, byte desired)
        {
        return testBits(value, desired, desired);
        }
    protected boolean testBits(byte value, byte mask, byte desired)
        {
        return (value & mask) == desired;
        }
    protected boolean testBits(byte value, Enable desired)
        {
        return testBits(value, desired, desired);
        }
    protected boolean testBits(byte value, Enable mask, Enable desired)
        {
        return testBits(value, mask.bVal, desired.bVal);
        }

    protected void writeEnable(int value)
        {
        // Disable interrupt bits, as we do not use same (they're not plumbed in our hardware).
        // Also make sure reserved bits write as zero.
        int reserved  = (is3782() ? (Enable.PIEN.bVal) : 0) | Enable.RES7.bVal | Enable.RES6.bVal;
        int weDontUse = Enable.PIEN.bVal | Enable.AIEN.bVal;
        value = value & ~(weDontUse | reserved);
        write8(Register.ENABLE, value);
        }

    protected byte readEnable()
        {
        return read8(Register.ENABLE);
        }

    protected byte readEnableAfterWrite()
        {
        delay(5);   // paranoia
        return readEnable();
        }

    protected void setIntegrationTime(int atime)
        {
        RobotLog.vv(TAG, "setIntegrationTime(0x%02x)", atime);
        write8(Register.ATIME, atime);
        }

    /**
     * From the TMD3782 datasheet:
     *
     * "When the proximity detection feature is enabled (PEN), the
     * state machine transitions through the Prox Accum, Prox Wait,
     * and Prox ADC states. The Prox Wait time is a fixed 2.4 ms,
     * whereas the Prox Accum time is determined by the number of
     * proximity LED pulses (PPULSE) and the Prox ADC time is
     * determined by the integration time (PTIME). The formulas to
     * determine the Prox Accum and Prox ADC times are given in the
     * associated boxes in Figure 25. If an interrupt is generated as a
     * result of the proximity cycle, it will be asserted at the end of the
     * Prox ADC state."
     *
     * Note: the reference to PTIME in the above seems to be an error,
     * as there is no such register. Though there MIGHT be, but the documentation
     * has been pulled for some reason: mysteriously, register=x02 is missing.
     * Perhaps it was there, and is now undocumented?
     *
     * If PTIME is in fact register two, then it's reset value seems to be 0xff.
     * Which should be the shortest possible integration time.
     */
    protected void setProximityPulseCount(int proximityPulseCount)
        {
        RobotLog.vv(TAG, "setProximityPulseCount(0x%02x)", proximityPulseCount);
        write8(Register.PPLUSE, proximityPulseCount);
        }

    protected boolean is3782()
        {
        return this.parameters.deviceId==AMS_TMD37821_ID || this.parameters.deviceId==AMS_TMD37823_ID;
        }

    protected void setGain(Gain gain)
        {
        RobotLog.vv(TAG, "setGain(%s)", gain);
        updateControl(Gain.MASK.bVal, gain.bVal);
        }

    protected void setPDrive(LEDDrive ledDrive)
        {
        RobotLog.vv(TAG, "setPDrive(%s)", ledDrive);
        updateControl(LEDDrive.MASK.bVal, ledDrive.bVal);
        }

    protected void updateControl(int mask, int value)
        {
        int control = read8(Register.CONTROL);

        // 3782 must set bit 5 as 1. On 3472, that must be written as zero. Ugh.
        // We believe this ensures that the 'IR Diode' is in use.
        if (is3782()) control |= 0x20;

        control = (control & ~mask) | (value & mask);
        write8(Register.CONTROL, control);
        }

    public byte getDeviceID()
        {
        return this.read8(Register.DEVICE_ID);
        }

    //----------------------------------------------------------------------------------------------
    // Interfaces
    //----------------------------------------------------------------------------------------------

    /** In this implementation, the {@link Color} methods return 16 bit unsigned values. */

    @Override
    public synchronized int red()
        {
        return normalToUnsignedShort(getNormalizedColors().red);
        }

    @Override
    public synchronized int green()
        {
        return normalToUnsignedShort(getNormalizedColors().green);
        }

    @Override
    public synchronized int blue()
        {
        return normalToUnsignedShort(getNormalizedColors().blue);
        }

    @Override
    public synchronized int alpha()
        {
        return normalToUnsignedShort(getNormalizedColors().alpha);
        }

    protected int normalToUnsignedShort(float normal)
        {
        return (int)(normal * parameters.getMaximumReading());
        }

    @Override
    public synchronized @ColorInt int argb()
        {
        return getNormalizedColors().toColor();
        }

    @Override
    public NormalizedRGBA getNormalizedColors()
        {
        // Wait for data to be valid. But don't wait forever: it's basically never
        // a good idea to wait forever. Be efficient and only use one I2c transaction
        // in the case where the data is already valid, which is the common case.
        //
        Deadline deadline = new Deadline(2, TimeUnit.SECONDS);
        byte[] data = null;
        for (;;)
            {
            // Read STATUS, ALPHA, RED, GREEN & BLUE
            final int cbRead = Register.PDATA.bVal - Register.STATUS.bVal;
            data = read(Register.STATUS, cbRead);

            // Is the data valid? Carry on if so
            if (testBits(data[0], Status.AVALID.bVal)) break;

            // Get out of here if we should; otherwise, briefly wait then try again
            if (Thread.currentThread().isInterrupted() || !isConnectedAndEnabled() || deadline.hasExpired())
                {
                // Return fake data and get out of here
                return new NormalizedRGBA();
                }
            delay(3);
            }

        final int dib = 1;
        int alpha = TypeConversion.unsignedShortToInt(TypeConversion.byteArrayToShort(data, dib + 0, ByteOrder.LITTLE_ENDIAN));
        int red   = TypeConversion.unsignedShortToInt(TypeConversion.byteArrayToShort(data, dib + 2, ByteOrder.LITTLE_ENDIAN));
        int green = TypeConversion.unsignedShortToInt(TypeConversion.byteArrayToShort(data, dib + 4, ByteOrder.LITTLE_ENDIAN));
        int blue  = TypeConversion.unsignedShortToInt(TypeConversion.byteArrayToShort(data, dib + 6, ByteOrder.LITTLE_ENDIAN));

        float colorNormalizationFactor = 1.0f / parameters.getMaximumReading();

        NormalizedRGBA result = new NormalizedRGBA();
        result.alpha = alpha * colorNormalizationFactor;
        result.red   = red   * colorNormalizationFactor;
        result.green = green * colorNormalizationFactor;
        result.blue  = blue  * colorNormalizationFactor;
        return result;
        }

    @Override
    public synchronized void enableLed(boolean enable)
    // We can't directly control the LED with I2C; it's always on
        {
        // ignore; used to throw an error, but the default opmode can try to turn off
        // (for range sensor variant) so got constant exceptions
        }

    @Override public boolean isLightOn()
        {
        return true;
        }

    @Override
    public synchronized I2cAddr getI2cAddress()
        {
        return this.deviceClient.getI2cAddress();
        }

    @Override
    public synchronized void setI2cAddress(I2cAddr i2cAddr)
        {
        this.parameters.i2cAddr = i2cAddr;
        this.deviceClient.setI2cAddress(i2cAddr);
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override
    public String getDeviceName()
        {
        return "AMS I2C Color Sensor";
        }

    @Override
    public Manufacturer getManufacturer()
        {
        return Manufacturer.AMS;
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected int readUnsignedByte(final Register reg)
        {
        return TypeConversion.unsignedByteToInt(read8(reg));
        }

    protected int readUnsignedShort(final Register reg, ByteOrder byteOrder)
        {
        byte[] data = read(reg, 2);
        return TypeConversion.unsignedShortToInt(TypeConversion.byteArrayToShort(data, 0, byteOrder));
        }

    @Override
    public synchronized byte read8(final Register reg)
        {
        return deviceClient.read8(reg.bVal | AMS_COLOR_COMMAND_BIT);
        }

    @Override
    public synchronized byte[] read(final Register reg, final int cb)
        {
        return deviceClient.read(reg.bVal | AMS_COLOR_COMMAND_BIT, cb);
        }

    @Override
    public synchronized void write8(Register reg, int data)
        {
        this.deviceClient.write8(reg.bVal | AMS_COLOR_COMMAND_BIT, data, I2cWaitControl.WRITTEN);
        }

    @Override public void write(Register reg, byte[] data)
        {
        this.deviceClient.write(reg.bVal | AMS_COLOR_COMMAND_BIT | AMS_COLOR_COMMAND_TYPE_AUTO_INCREMENT, data, I2cWaitControl.WRITTEN);
        }

    protected void delay(int ms)
        {
        try
            {
            Thread.sleep(ms);
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        }
    }
