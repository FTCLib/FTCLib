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

import com.qualcomm.robotcore.util.RobotLog;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/**
 * {@link LynxI2cDeviceConfiguration} need to specify the bus number in addition to the port.
 * On Lynx, the latter is only used in the configuration UI to maintain the sort order.
 */
@SuppressWarnings("WeakerAccess")
public class LynxI2cDeviceConfiguration extends DeviceConfiguration
    {
    public static final String TAG = "LynxI2cDeviceConfiguration";
    public static final String XMLATTR_BUS = "bus";

    protected int bus = 0;

    public LynxI2cDeviceConfiguration()
        {
        super();
        }

    public int getBus()
        {
        return bus;
        }

    public void setBus(int bus)
        {
        this.bus = bus;
        }

    @Override public I2cChannel getI2cChannel()
        {
        return new I2cChannel(getBus());
        }

    @Override public void serializeXmlAttributes(XmlSerializer serializer)
        {
        try {
            super.serializeXmlAttributes(serializer);
            serializer.attribute("", XMLATTR_BUS, String.valueOf(this.getBus()));
            }
        catch (Exception e)
            {
            RobotLog.ee(TAG, e, "exception serializing");
            throw new RuntimeException(e);
            }
        }

    @Override public void deserializeAttributes(XmlPullParser parser)
        {
        super.deserializeAttributes(parser);

        // Read the bus. If there is no bus (the legacy case), then we use the port
        String busAttr = parser.getAttributeValue(null, LynxI2cDeviceConfiguration.XMLATTR_BUS);
        int bus = busAttr == null ? this.getPort(): Integer.parseInt(busAttr);
        this.setBus(bus);
        }
    }
