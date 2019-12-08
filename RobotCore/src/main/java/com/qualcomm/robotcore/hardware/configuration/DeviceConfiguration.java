/* Copyright (c) 2014, 2015 Qualcomm Technologies Inc, Noah Andrews

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.robotcore.hardware.configuration;

import androidx.annotation.CallSuper;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.util.RobotLog;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DeviceConfiguration implements Serializable, Comparable<DeviceConfiguration> {

    public static final String            TAG = "DeviceConfiguration";
    public static final String            XMLATTR_NAME = "name";
    public static final String            XMLATTR_PORT = "port";
    public static final String            DISABLED_DEVICE_NAME = "NO$DEVICE$ATTACHED"; // internal; never to be shown to users

    protected           String            name;
    private             ConfigurationType type = BuiltInConfigurationType.NOTHING;
    private             int               port;
    private             boolean           enabled = false;

    public DeviceConfiguration(int port, ConfigurationType type, String name, boolean enabled)
        {
        this.port = port;
        this.type = type;
        this.name = name;
        this.enabled = enabled;
        }

    public DeviceConfiguration()
        {
        this(0);
        }

    public DeviceConfiguration(int port)
        {
        this(port, BuiltInConfigurationType.NOTHING, DISABLED_DEVICE_NAME, false);
        }

    public DeviceConfiguration(ConfigurationType type)
        {
        this(0, type, "", false);
        }

    // Devices know their port, and their type, and start out not enabled.
    public DeviceConfiguration(int port, ConfigurationType type)
        {
        this(port, type, DISABLED_DEVICE_NAME, false);
        }

    public boolean isEnabled()
        {
        return enabled;
        }

    public void setEnabled(boolean enabled)
        {
        this.enabled = enabled;
        }

    public String getName()
        {
        return this.name;
        }

    public void setName(String newName)
        {
        this.name = newName;
        }

    public void setConfigurationType(ConfigurationType type)
        {
        this.type = type;
        }

    public static void sortByName(List<? extends DeviceConfiguration> configurations)
        {
        Collections.sort(configurations, new Comparator<DeviceConfiguration>()
            {
            @Override public int compare(DeviceConfiguration lhs, DeviceConfiguration rhs)
                {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
                }
            });
        }

    public ConfigurationType getConfigurationType()
        {
        return this.type;
        }

    public ConfigurationType getSpinnerChoiceType()
        {
        return this.getConfigurationType();
        }

    public int getPort()
        {
        return port;
        }

    public void setPort(int port)
        {
        this.port = port;
        }

    /** This device is an I2c device. Returns information as to how to connect to same */
    public I2cChannel getI2cChannel()
        {
        return new I2cChannel(getPort());
        }

    /** A separate class to allow the compiler to help us find all the place this should be used */
    public static class I2cChannel
        {
        public final int channel;
        public I2cChannel(int channel) { this.channel = channel;}
        @Override public String toString() { return "channel=" + channel; }
        }

    @Override
    public int compareTo(DeviceConfiguration another)
        {
        return this.getPort() - another.getPort();
        }

    public void serializeXmlAttributes(XmlSerializer serializer)
        {
        try {
            serializer.attribute("", XMLATTR_NAME, this.getName());
            serializer.attribute("", XMLATTR_PORT, String.valueOf(this.getPort()));
            }
        catch (Exception e)
            {
            RobotLog.ee(TAG, e, "exception serializing");
            throw new RuntimeException(e);
            }
        }

    /**
     * Initialize this DeviceConfiguration from XML
     * @param parser An XmlPullParser pointed at the corresponding open tag
     * @param xmlReader A ReadXMLFileHandler instance to call onDeviceParsed on
     */
    public final void deserialize(XmlPullParser parser, ReadXMLFileHandler xmlReader) throws IOException, XmlPullParserException, RobotCoreException
        {
        String openingTagName = parser.getName();
        deserializeAttributes(parser);
        setConfigurationType(ReadXMLFileHandler.deform(openingTagName));
        setEnabled(true); // We only write *enabled* devices, so if we *read* one, it's enabled.

        int eventType = parser.next();
        String currentTagName = parser.getName();
        ConfigurationType configurationType = ReadXMLFileHandler.deform(currentTagName);

        while (eventType != XmlPullParser.END_DOCUMENT)
            { // we shouldn't reach the end of the document here
            if (eventType == XmlPullParser.END_TAG)
                {
                if (currentTagName != null && currentTagName.equals(openingTagName))
                    {
                    // We've finished processing all children
                    onDeserializationComplete(xmlReader);
                    return;
                    }
                }

            if (eventType == XmlPullParser.START_TAG)
                {
                deserializeChildElement(configurationType, parser, xmlReader);
                }
            eventType = parser.next();
            currentTagName = parser.getName();
            configurationType = ReadXMLFileHandler.deform(currentTagName);
            }
        // we should never reach the end of the document while parsing this part. If we do, it's an error.
        RobotLog.logAndThrow("Reached the end of the XML file while processing a device.");
        }

    /**
     * This gets called while the parser is pointed at the open tag for this device configuration.
     *
     * Override this to deserialize additional attributes of the open tag, or to do additional initialization
     *
     * Do NOT advance the parser from this method.
     */
    @CallSuper
    protected void deserializeAttributes(XmlPullParser parser)
        {
        setName(parser.getAttributeValue(null, XMLATTR_NAME));

        String portAttr = parser.getAttributeValue(null, DeviceConfiguration.XMLATTR_PORT);
        int port = portAttr == null ? -1 : Integer.parseInt(portAttr);  // -1 means no port specified
        setPort(port);
        }

    /**
     * This gets called while the parser is pointed at the open tag for a child device configuration.
     *
     * This method MUST be overridden by any subclass that needs to access XML child elements.
     *
     * When this method returns, the parser can be advanced only as far as the child's end tag.
     * Do not parse multiple child elements.
     */
    protected void deserializeChildElement(ConfigurationType configurationType, XmlPullParser parser, ReadXMLFileHandler xmlReader) throws IOException, XmlPullParserException, RobotCoreException
        { }

    /**
     * This gets called when the serialization process has been completed.
     */
    @CallSuper
    protected void onDeserializationComplete(ReadXMLFileHandler xmlReader)
        {
        xmlReader.onDeviceParsed(this);
        }
}
