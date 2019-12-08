/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.qualcomm.hardware.hitechnic;

import android.graphics.Color;
import androidx.annotation.ColorInt;

import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.I2cWaitControl;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cAddressableDevice;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.SwitchableLight;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * {@link HiTechnicNxtColorSensor} provides support for the HiTechnic color sensor.
 *
 * @see <a href="https://www.hitechnic.com/cgi-bin/commerce.cgi?preadd=action&key=NCO1038">HiTechnic color sensor</a>
 */
public class HiTechnicNxtColorSensor extends I2cDeviceSynchDevice<I2cDeviceSynch>
        implements ColorSensor, NormalizedColorSensor, SwitchableLight, I2cAddressableDevice {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  public final static I2cAddr ADDRESS_I2C = I2cAddr.create8bit(2);

  public enum Register
    {
      COMMAND(0x41),
      COLOR_NUMBER(0x42),
      RED(0x43),
      GREEN(0x44),
      BLUE(0x45),
      READ_WINDOW_FIRST(RED.bVal),
      READ_WINDOW_LAST(BLUE.bVal);
    public byte bVal;
    Register(int value) { this.bVal = (byte) value; }
    }

  public enum Command
    {
      ACTIVE_LED(0x00),
      PASSIVE_LED(0x01);
    public byte bVal;
    Command(int value) { this.bVal = (byte) value; }
    }

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  protected final float colorNormalizationFactor = 1.0f / 256.0f; // color values are unsigned bytes

  protected boolean isLightOn = false;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public HiTechnicNxtColorSensor(I2cDeviceSynch deviceClient) {
    super(deviceClient, true);

    I2cDeviceSynch.ReadWindow readWindow = new I2cDeviceSynch.ReadWindow(
          Register.READ_WINDOW_FIRST.bVal,
          Register.READ_WINDOW_LAST.bVal - Register.READ_WINDOW_FIRST.bVal + 1,
          I2cDeviceSynch.ReadMode.REPEAT);
    this.deviceClient.setReadWindow(readWindow);
    this.deviceClient.setI2cAddress(ADDRESS_I2C);
    this.deviceClient.engage();

    this.registerArmingStateCallback(false);
  }

  @Override
  protected synchronized boolean doInitialize() {
    enableLed(true);
    return true;
    }

  //------------------------------------------------------------------------------------------------
  // Utility
  //------------------------------------------------------------------------------------------------

  public byte read8(Register reg) {
      return this.deviceClient.read8(reg.bVal);
      }
  public void write8(Register reg, byte value) {
      this.deviceClient.write8(reg.bVal, value);
      }

  public int readColorByte(Register register) {
      return TypeConversion.unsignedByteToInt(read8(register));
      }

  public void writeCommand(Command command) {
      this.deviceClient.waitForWriteCompletions(I2cWaitControl.ATOMIC);    // avoid overwriting previous command
      this.write8(Register.COMMAND, command.bVal);
      }

  //------------------------------------------------------------------------------------------------
  // Color interfaces
  //------------------------------------------------------------------------------------------------

  @Override
  public String toString() {
    return String.format("argb: 0x%08x", argb());
  }

  @Override
  public int red() {
    return readColorByte(Register.RED);
  }

  @Override
  public int green() {
    return readColorByte(Register.GREEN);
  }

  @Override
  public int blue() {
    return readColorByte(Register.BLUE);
  }

  @Override
  public int alpha() {
    return 0;
  }

  @Override
  public @ColorInt int argb() {
    return Color.argb(alpha(), red(), green(), blue());
  }

  @Override public NormalizedRGBA getNormalizedColors() {
    NormalizedRGBA result = new NormalizedRGBA();
    result.red   = red()   * colorNormalizationFactor;
    result.green = green() * colorNormalizationFactor;
    result.blue  = blue()  * colorNormalizationFactor;
    // We make up an alpha that at least somewhat approximates what an actual alpha-measuring sensor
    // could reasonably read given the red, green, and blue values.
    result.alpha = Math.max(Math.max(result.red, result.green), result.blue);
    return result;
  }

  @Override
  public synchronized void enableLed(boolean enable) {
    writeCommand(enable ? Command.ACTIVE_LED : Command.PASSIVE_LED);
    this.isLightOn = enable;
  }

  @Override
  public void enableLight(boolean enable) {
    enableLed(enable);
  }

  @Override public boolean isLightOn() {
    return this.isLightOn;
  }

@Override
  public void setI2cAddress(I2cAddr newAddress) {
    // Necessary because of legacy considerations
    throw new UnsupportedOperationException("setI2cAddress is not supported.");
  }

  @Override
  public I2cAddr getI2cAddress() {
    return ADDRESS_I2C;
  }

  //------------------------------------------------------------------------------------------------
  // HardwareDevice
  //------------------------------------------------------------------------------------------------

  @Override public Manufacturer getManufacturer() {
    return Manufacturer.HiTechnic;
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(com.qualcomm.robotcore.R.string.configTypeHTColorSensor);
  }

  @Override
  public int getVersion() {
    return 2;
  }
}
