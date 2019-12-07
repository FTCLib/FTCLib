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
package com.qualcomm.robotcore.hardware.configuration;

import android.support.annotation.NonNull;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.util.SerialNumber;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * A Lynx USB Device contains one or more Lynx modules linked together over an
 * RS485 bus. The one of these which is connected externally to USB is termed
 * the 'parent'; the others are called 'children'.
 */
@SuppressWarnings("WeakerAccess")
public class LynxUsbDeviceConfiguration extends ControllerConfiguration<LynxModuleConfiguration>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String XMLATTR_PARENT_MODULE_ADDRESS = "parentModuleAddress";

    int parentModuleAddress = LynxConstants.DEFAULT_PARENT_MODULE_ADDRESS;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    // For use in deserializing from XML
    public LynxUsbDeviceConfiguration()
        {
        super("", new LinkedList<LynxModuleConfiguration>(), SerialNumber.createFake(), BuiltInConfigurationType.LYNX_USB_DEVICE);
        }

    public LynxUsbDeviceConfiguration(String name, List<LynxModuleConfiguration> modules, SerialNumber serialNumber)
        {
        super(name, new LinkedList<LynxModuleConfiguration>(modules), serialNumber, BuiltInConfigurationType.LYNX_USB_DEVICE);
        initialize();
        }

    @Override public void setSerialNumber(@NonNull SerialNumber serialNumber)
        {
        super.setSerialNumber(serialNumber);
        for (LynxModuleConfiguration module : getModules())
            {
            module.setUsbDeviceSerialNumber(serialNumber);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    /**
     * Returns the module address of the Lynx module which is directly USB connected
     */
    public int getParentModuleAddress()
        {
        return this.parentModuleAddress;
        }

    public void setParentModuleAddress(int moduleAddress)
        {
        this.parentModuleAddress = moduleAddress;
        }

    public List<LynxModuleConfiguration> getModules()
        {
        return this.getDevices();
        }

    @Override
    protected void deserializeAttributes(XmlPullParser parser)
        {
        super.deserializeAttributes(parser);
        String parentAddressString = parser.getAttributeValue(null, XMLATTR_PARENT_MODULE_ADDRESS);
        if (parentAddressString != null && !parentAddressString.isEmpty())
            {
            setParentModuleAddress(Integer.parseInt(parentAddressString));
            }
        }

    @Override
    protected void deserializeChildElement(ConfigurationType configurationType, XmlPullParser parser, ReadXMLFileHandler xmlReader) throws IOException, XmlPullParserException, RobotCoreException
        {
        super.deserializeChildElement(configurationType, parser, xmlReader);
        if (configurationType == BuiltInConfigurationType.LYNX_MODULE)
            {
            LynxModuleConfiguration moduleConfiguration = new LynxModuleConfiguration();
            moduleConfiguration.deserialize(parser, xmlReader);
            moduleConfiguration.setIsParent(moduleConfiguration.getModuleAddress() == parentModuleAddress);
            getModules().add(moduleConfiguration);
            }
        }

    @Override
    protected void onDeserializationComplete(ReadXMLFileHandler xmlReader)
        {
        initialize();
        super.onDeserializationComplete(xmlReader);
        }


    private void initialize()
        {
        // Sort in increasing order by module address
        Collections.sort(this.getModules(), new Comparator<DeviceConfiguration>()
            {
            @Override public int compare(DeviceConfiguration lhs, DeviceConfiguration rhs)
                {
                return lhs.getPort() - rhs.getPort();
                }
            });

        for (LynxModuleConfiguration module : getModules())
            {
            module.setUsbDeviceSerialNumber(getSerialNumber());
            if (module.isParent())
                {
                this.setParentModuleAddress(module.getModuleAddress());
                }
            }
        }
    }
