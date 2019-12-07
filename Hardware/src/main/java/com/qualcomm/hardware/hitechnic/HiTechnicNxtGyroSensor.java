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

import com.qualcomm.robotcore.hardware.AnalogSensor;
import com.qualcomm.robotcore.hardware.GyroSensor;
import com.qualcomm.robotcore.hardware.Gyroscope;
import com.qualcomm.robotcore.hardware.LegacyModule;
import com.qualcomm.robotcore.hardware.LegacyModulePortDeviceImpl;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.Statistics;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.Axis;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link HiTechnicNxtGyroSensor} supports the legacy HiTechnic gyro sensor. This sensor
 * is an analog sensor that must be connected using a Core Legacy Module.
 *
 * <p>"The NXT Gyro Sensor contains a single axis gyroscopic sensor that detects rotation and
 * returns a value representing the number of degrees per second of rotation. The Gyro Sensor
 * can measure up to +/- 360° per second of rotation."</p>
 *
 * <p>"The rotation rate can be read up to approximately 300 times per second"</p>
 *
 * <p>Note that the value coming out of the sensor is the rotation rate in the <em>clockwise</em>
 * direction.</p>
 *
 * @see <a href="https://www.hitechnic.com/cgi-bin/commerce.cgi?preadd=action&key=NGY1044">HiTechnic
 * Gyro Sensor</a>
 */
public class HiTechnicNxtGyroSensor extends LegacyModulePortDeviceImpl implements GyroSensor, Gyroscope, AnalogSensor {

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  public static final String TAG = "HiTechnicNxtGyroSensor";

  /** The voltage indicting zero angular rotation */
  protected double biasVoltage;
  /** The conversion factor between voltage and angular rotation rate */
  protected double degreesPerSecondPerVolt;
  /** whether or not the gyroscope is currently calibrating */
  protected boolean isCalibrating         = false;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public HiTechnicNxtGyroSensor(LegacyModule legacyModule, int physicalPort) {
    super(legacyModule, physicalPort);
    this.biasVoltage = getDefaultBiasVoltage();
    this.degreesPerSecondPerVolt = getDefaultDegreesPerSecondPerVolt();
    finishConstruction();
  }

  @Override
  protected void moduleNowArmedOrPretending() {
    module.enableAnalogReadMode(physicalPort);
    }

  //------------------------------------------------------------------------------------------------
  // Accessing
  //------------------------------------------------------------------------------------------------

  public double getBiasVoltage() {
    return this.biasVoltage;
  }
  public void setBiasVoltage(double biasVoltage) {
    this.biasVoltage = biasVoltage;
    RobotLog.vv(TAG, "biasVoltage=%.3f", this.biasVoltage);
  }

  public double getDegreesPerSecondPerVolt() {
    return this.degreesPerSecondPerVolt;
  }
  public void setDegreesPerSecondPerVolt(double degreesPerSecondPerVolt) {
    this.degreesPerSecondPerVolt = degreesPerSecondPerVolt;
  }

  public double getDefaultDegreesPerSecondPerVolt() {
    // In the RobotC world, A/D was reported as units in [0,1023)
    double robotCUnitRange = 1024;
    // In that reported range, a unit value was a single degree per second
    double robotCUnitsPerDegreesPerSecond = 1.0;
    // Now it's just math
    double robotCUnitsPerVolt = robotCUnitRange / getMaxVoltage();
    return robotCUnitsPerVolt / robotCUnitsPerDegreesPerSecond;
  }

  public double getDefaultBiasVoltage() {
    return 2.908; // experimentally determined
  }

  //------------------------------------------------------------------------------------------------
  // Gyroscope
  //------------------------------------------------------------------------------------------------

  @Override public Set<Axis> getAngularVelocityAxes() {
    Set<Axis> result = new HashSet<Axis>();
    result.add(Axis.Z);
    return result;
  }

  @Override public AngularVelocity getAngularVelocity(AngleUnit unit) {
    return new AngularVelocity(unit, 0, 0, getAngularZVelocity(unit), System.nanoTime());
    }

  protected float getAngularZVelocity(AngleUnit unit) {
    double voltage = readRawVoltage() - biasVoltage;
    double degsPerSecond = voltage * this.degreesPerSecondPerVolt;
    degsPerSecond = -degsPerSecond; // convert to usual Cartesian orientation
    double result = unit.fromUnit(AngleUnit.DEGREES, degsPerSecond);
    return (float)result;
    }

  //------------------------------------------------------------------------------------------------
  // Operations
  //------------------------------------------------------------------------------------------------

  @Override
  public String toString() {
    return String.format("Gyro: %3.1f", getAngularZVelocity(AngleUnit.DEGREES));
  }

  /**
   * Calibrates the HiTechnic gyroscope attempting to establish a reasonable value for
   * its internal bias using default calibration parameters.
   */
  @Override
  public synchronized void calibrate() {
    calibrate(2500, 50);
    }

  /**
   * Calibrates the HiTechnic gyroscope attempting to establish a reasonable value for
   * its internal bias using specified calibration parameters
   * @param msCalibrationDuration   the amount of time to spend on calibration
   * @param msCalibrationInterval   the interval, in ms, to pause between each
   */
  public void calibrate(int msCalibrationDuration, int msCalibrationInterval) {
    calibrate(msCalibrationDuration, msCalibrationInterval, 500);
  }

  /**
   * Calibrates the HiTechnic gyroscope attempting to establish a reasonable value for
   * its internal bias using specified calibration parameters
   * @param msCalibrationDuration   the amount of time to spend on calibration
   * @param msCalibrationInterval   the interval, in ms, to pause between each
   * @param msSettlingTime          the initial duration, in ms, to allow the gyro to settle
   */
  public synchronized void calibrate(int msCalibrationDuration, int msCalibrationInterval, int msSettlingTime) {
    int calibrationCount = (msCalibrationDuration-msSettlingTime) / msCalibrationInterval;
    isCalibrating = true;
    sleep(msSettlingTime); // let things settle
    try {
      Statistics statistics = new Statistics();
      for (int i = 0; !Thread.currentThread().isInterrupted() && i < calibrationCount; i++) {
        if (i > 0 && msCalibrationInterval > 0) sleep(msCalibrationInterval);
        double voltage = readRawVoltage();
        statistics.add(voltage);
      }
      setBiasVoltage(statistics.getMean());
    } finally {
      isCalibrating = false;
      }
  }

  protected void sleep(int ms) {
    try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
  }

  /**
   * Returns whether or not the gyro is currently calibrating
   * @return whether or not the gyro is currently calibrating
   */
  @Override
  public boolean isCalibrating() {
    return isCalibrating;
  }

  @Override
  public synchronized double getRotationFraction() {
    return readRawVoltage() / getMaxVoltage();
  }

  @Override
  public synchronized double readRawVoltage()  {
    return module.readAnalogVoltage(physicalPort);
  }

  public double getMaxVoltage() {
    // The sensor itself is a 5v sensor. As it is always accessed here through a Core
    // Legacy Module, that is the maximum value that we will always see.
    final double sensorMaxVoltage = 5.0;
    return sensorMaxVoltage;
  }

  /**
   * Method not supported by hardware.
   * @return nothing
   * @throws UnsupportedOperationException
   */
  @Override
  public int getHeading() {
    notSupported();
    return 0;
  }

  /**
   * Method not supported by hardware.
   * @return nothing
   * @throws UnsupportedOperationException
   */
  @Override
  public int rawX() {
    notSupported();
    return 0;
  }

  /**
   * Method not supported by hardware.
   * @return nothing
   * @throws UnsupportedOperationException
   */
  @Override
  public int rawY() {
    notSupported();
    return 0;
  }

  /**
   * Method not supported by hardware.
   * @return nothing
   * @throws UnsupportedOperationException
   */
  @Override
  public int rawZ() {
    notSupported();
    return 0;
  }

  /**
   * Method not supported by hardware.
   * @return nothing
   * @throws UnsupportedOperationException
   */
  @Override
  public void resetZAxisIntegrator() {
    // nothing to do
  }

  @Override
  public String status() {
    return String.format("NXT Gyro Sensor, connected via device %s, port %d",
        module.getSerialNumber(), physicalPort);
  }

  @Override public Manufacturer getManufacturer() {
    return Manufacturer.HiTechnic;
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(com.qualcomm.robotcore.R.string.configTypeHTGyro);
  }

  @Override
  public String getConnectionInfo() {
    return module.getConnectionInfo() + "; port " + physicalPort;
  }

  @Override
  public int getVersion() {
    return 1;
  }

  @Override
  public void resetDeviceConfigurationForOpMode() {
    // nothing to do
  }

  @Override
  public void close() {
    // take no action
  }

  protected void notSupported() {
    throw new UnsupportedOperationException("This method is not supported for " + getDeviceName());
  }

}
