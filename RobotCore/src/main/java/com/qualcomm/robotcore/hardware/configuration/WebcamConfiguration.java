/*
Copyright (c) 2018 Robert Atkinson

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

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.util.SerialNumber;

import org.xmlpull.v1.XmlPullParser;

import java.util.LinkedList;

@SuppressWarnings("WeakerAccess")
public class WebcamConfiguration extends ControllerConfiguration<DeviceConfiguration>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String XMLATTR_AUTO_OPEN_CAMERA = "autoOpen";

    /** Whether we are to open the camera automatically on robot restart. Not currently
     * implemented; parsing support here is future-proofing. */
    protected boolean autoOpen;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    // Used only for XML deserialization
    public WebcamConfiguration()
        {
        this("", SerialNumber.createFake());
        }

    public WebcamConfiguration(String name, SerialNumber serialNumber)
        {
        this(name, serialNumber, false);
        }

    public WebcamConfiguration(String name, SerialNumber serialNumber, boolean autoOpen)
        {
        super(name, new LinkedList<DeviceConfiguration>(), serialNumber, BuiltInConfigurationType.WEBCAM);
        this.autoOpen = autoOpen;
        }

    //----------------------------------------------------------------------------------------------
    // Access
    //----------------------------------------------------------------------------------------------

    public boolean getAutoOpen()
        {
        return autoOpen;
        }

    private void setAutoOpen(boolean autoOpen)
        {
        this.autoOpen = autoOpen;
        }

    @Override
    protected void deserializeAttributes(XmlPullParser parser)
        {
        super.deserializeAttributes(parser);
        String autoOpenAttr = parser.getAttributeValue(null, XMLATTR_AUTO_OPEN_CAMERA);

        if (autoOpenAttr != null && !autoOpenAttr.isEmpty())
            {
            setAutoOpen(Boolean.parseBoolean(autoOpenAttr));
            }
        }
    }
