/*
Copyright (c) 2016 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

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
package com.qualcomm.ftccommon.configuration;

import android.content.Context;

import com.qualcomm.robotcore.hardware.DeviceManager;
import com.qualcomm.robotcore.hardware.ScannedDevices;
import com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationUtility;
import com.qualcomm.robotcore.hardware.configuration.ControllerConfiguration;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.system.Misc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * {@link RobotConfigMap} represents the loaded and parsed state of an XML robot configuration.
 * It contains the set of {@link ControllerConfiguration}s in the configuration, accessible by
 * serial number.
 */
@SuppressWarnings("javadoc")
public class RobotConfigMap implements Serializable
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    Map<SerialNumber, ControllerConfiguration>  map = new HashMap<SerialNumber, ControllerConfiguration>();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public RobotConfigMap(Collection<ControllerConfiguration> controllerConfigurations)
        {
        for (ControllerConfiguration controllerConfiguration : controllerConfigurations)
            {
            this.put(controllerConfiguration.getSerialNumber(), controllerConfiguration);
            }
        }

    public RobotConfigMap(Map<SerialNumber, ControllerConfiguration> map)
        {
        this.map = new HashMap<SerialNumber, ControllerConfiguration>(map);
        }

    public RobotConfigMap(RobotConfigMap him)
        {
        this(him.map);
        }

    public RobotConfigMap()
        {
        super();
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public boolean contains(SerialNumber serialNumber)
        {
        return this.map.containsKey(serialNumber);
        }

    public ControllerConfiguration get(SerialNumber serialNumber)
        {
        return this.map.get(serialNumber);
        }

    public void put(SerialNumber serialNumber, ControllerConfiguration controllerConfiguration)
        {
        this.map.put(serialNumber, controllerConfiguration);
        }

    public boolean remove(SerialNumber serialNumber)
        {
        return this.map.remove(serialNumber) != null;
        }

    public int size()
        {
        return this.map.size();
        }

    public Collection<SerialNumber> serialNumbers()
        {
        return this.map.keySet();
        }

    public Collection<ControllerConfiguration> controllerConfigurations()
        {
        return this.map.values();
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    // a debugging utility
    public void writeToLog(String tag, String message)
        {
        RobotLog.vv(tag, "robotConfigMap: %s", message);
        for (ControllerConfiguration controllerConfiguration : this.controllerConfigurations())
            {
            RobotLog.vv(tag, "   serial=%s id=0x%08x name='%s' ", controllerConfiguration.getSerialNumber(), controllerConfiguration.hashCode(), controllerConfiguration.getName());
            }
        }
    // a debugging utility
    public void writeToLog(String tag, String message, ControllerConfiguration controllerConfiguration)
        {
        writeToLog(tag, message);
        RobotLog.vv(tag, "  :serial=%s id=0x%08x name='%s' ", controllerConfiguration.getSerialNumber(), controllerConfiguration.hashCode(), controllerConfiguration.getName());
        }

    //----------------------------------------------------------------------------------------------
    // Auto configuration
    //----------------------------------------------------------------------------------------------

    /**
     * Answers as to whether all the controllers in this map have real USB devices associated
     * with them or not
     */
    boolean allControllersAreBound()
        {
        for (ControllerConfiguration controllerConfiguration : this .controllerConfigurations())
            {
            if (controllerConfiguration.getSerialNumber().isFake())
                {
                return false;
                }
            }
        return true;
        }

    /**
     * For each controller in this map that currently lacks a real serial number, try to choose
     * an unused selection from the scanned devices to associate with same.
     */
    public void bindUnboundControllers(ScannedDevices scannedDevices)
        {
        // First, find out whom we have to choose from that's not already used
        ScannedDevices extraDevices = new ScannedDevices(scannedDevices);
        for (ControllerConfiguration controllerConfiguration : this.controllerConfigurations())
            {
            extraDevices.remove(controllerConfiguration.getSerialNumber());
            }

        // Invert the map, so we can easily lookup (ConfigurationType -> extra controllers)
        Map<ConfigurationType, List<SerialNumber>> extraByType = new HashMap<ConfigurationType, List<SerialNumber>>();
        for (Map.Entry<SerialNumber,DeviceManager.UsbDeviceType> pair : extraDevices.entrySet())
            {
            ConfigurationType configurationType = BuiltInConfigurationType.fromUSBDeviceType(pair.getValue());
            if (configurationType != BuiltInConfigurationType.UNKNOWN)
                {
                List<SerialNumber> list = extraByType.get(configurationType);
                if (list == null)
                    {
                    list = new LinkedList<SerialNumber>();
                    extraByType.put(configurationType, list);
                    }
                list.add(pair.getKey());
                }
            }

        // Figure out who's missing, and assign. Be careful about updating while iterating.
        for (ControllerConfiguration controllerConfiguration : this.controllerConfigurations())
            {
            if (controllerConfiguration.getSerialNumber().isFake())
                {
                List<SerialNumber> list = extraByType.get(controllerConfiguration.getConfigurationType());
                if (list != null && !list.isEmpty())
                    {
                    // Use the first available controller of the right type, and bind to it
                    SerialNumber newSerialNumber = list.remove(0);
                    controllerConfiguration.setSerialNumber(newSerialNumber);
                    }
                }
            }

        // Make sure we're accurate on the way out
        Collection<ControllerConfiguration> controllers = new ArrayList<ControllerConfiguration>(this.controllerConfigurations());
        map.clear();
        for (ControllerConfiguration controllerConfiguration : controllers)
            {
            put(controllerConfiguration.getSerialNumber(), controllerConfiguration);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Swapping
    //----------------------------------------------------------------------------------------------

    /** Changes a serial number of a controller known to be in this configuration */
    public void setSerialNumber(ControllerConfiguration controllerConfiguration, SerialNumber serialNumber)
        {
        this.remove(controllerConfiguration.getSerialNumber());
        controllerConfiguration.setSerialNumber(serialNumber);
        this.put(serialNumber, controllerConfiguration);
        }

    /** Swaps the serial numbers (and attachment status) of two controllers both known to be in this configuration */
    public void swapSerialNumbers(ControllerConfiguration a, ControllerConfiguration b)
        {
        SerialNumber aSerialNumber = a.getSerialNumber();
        a.setSerialNumber(b.getSerialNumber());
        b.setSerialNumber(aSerialNumber);

        this.put(a.getSerialNumber(), a);
        this.put(b.getSerialNumber(), b);

        boolean knownToBeAttached = a.isKnownToBeAttached();
        a.setKnownToBeAttached(b.isKnownToBeAttached());
        b.setKnownToBeAttached(knownToBeAttached);
        }

    public boolean isSwappable(ControllerConfiguration target, ScannedDevices scannedDevices, Context context)
        {
        return !getEligibleSwapTargets(target, scannedDevices, context).isEmpty();
        }

    /**
     * Returns a list of the candidate configurations with which the target may be swapped.
     * Candidates must be of the same configuration type as the target but must not be the
     * target itself. We pull candidates both from what's in this {@link RobotConfigMap} and
     * what's currently attached to the USB bus: those are possibly intersecting sets, but each
     * may have members which are not in the other.
     */
    public List<ControllerConfiguration> getEligibleSwapTargets(ControllerConfiguration target, ScannedDevices scannedDevices, Context context)
        {
        List<ControllerConfiguration> result = new LinkedList<ControllerConfiguration>();

        // Only our USB-attached devices are swappable
        ConfigurationType type = target.getConfigurationType();
        if (!(type==BuiltInConfigurationType.MOTOR_CONTROLLER
                || type==BuiltInConfigurationType.SERVO_CONTROLLER
                || type==BuiltInConfigurationType.DEVICE_INTERFACE_MODULE
                || type==BuiltInConfigurationType.LEGACY_MODULE_CONTROLLER))
            return result;

        if (target.getSerialNumber().isFake())
            {
            return result;
            }

        // First snarf candidates that are already in this robot configuration
        for (ControllerConfiguration other : this.controllerConfigurations())
            {
            SerialNumber serialNumber = other.getSerialNumber();

            if (serialNumber.isFake()) continue;
            if (serialNumber.equals(target.getSerialNumber())) continue;
            if (containsSerialNumber(result, serialNumber)) continue;   // shouldn't need this test, but it's harmless
            if (other.getConfigurationType() == target.getConfigurationType())
                {
                result.add(other);
                }
            }

        // Then add others we know about from scanning but haven't added yet
        for (Map.Entry<SerialNumber, DeviceManager.UsbDeviceType> entry : scannedDevices.entrySet())
            {
            SerialNumber serialNumber = entry.getKey();

            if (serialNumber.isFake()) continue;
            if (serialNumber.equals(target.getSerialNumber())) continue;
            if (containsSerialNumber(result, serialNumber)) continue;
            if (entry.getValue() == target.toUSBDeviceType())
                {
                String name = generateName(context, target.getConfigurationType(), result);
                ControllerConfiguration controllerConfiguration = ControllerConfiguration.forType(name, entry.getKey(), target.getConfigurationType());
                controllerConfiguration.setKnownToBeAttached(scannedDevices.containsKey(controllerConfiguration.getSerialNumber()));
                result.add(controllerConfiguration);
                }
            }

        return result;
        }

    /**
     * Generates a name that's unique across both this whole configuration and the candidate swaps
     * that have been produced so far.
     */
    protected String generateName(Context context, ConfigurationType type, List<ControllerConfiguration> resultSoFar)
        {
        for (int i = ConfigurationUtility.firstNamedDeviceNumber; ; i++)
            {
            String name = Misc.formatForUser("%s %d", type.getDisplayName(ConfigurationType.DisplayNameFlavor.Normal), i);
            if (!nameExists(name, resultSoFar))
                {
                return name;
                }
            }
        }
    protected boolean nameExists(String name, List<ControllerConfiguration> resultSoFar)
        {
        for (ControllerConfiguration controllerConfiguration : resultSoFar)
            {
            if (controllerConfiguration.getName().equalsIgnoreCase(name)) return true;
            }
        for (ControllerConfiguration controllerConfiguration : this.controllerConfigurations())
            {
            if (controllerConfiguration.getName().equalsIgnoreCase(name)) return true;
            }
        return false;
        }

    protected boolean containsSerialNumber(List<ControllerConfiguration> list, SerialNumber serialNumber)
        {
        for (ControllerConfiguration controllerConfiguration : list)
            {
            if (controllerConfiguration.getSerialNumber().equals(serialNumber))
                {
                return true;
                }
            }
        return false;
        }
    }
