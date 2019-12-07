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
package org.firstinspires.ftc.robotcore.internal.network;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;

import java.util.Set;

/**
 * {@link RobotControllerPreference} is a simple utility class for remoting shared
 * preferences value to or from the robot controller.
 */
@SuppressWarnings("WeakerAccess")
public class RobotControllerPreference
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private String prefName;

    // There are only a fixed set of supported preference types. We just enumerate them
    // here in the interests of keeping our serialization logic simple.

    private String      stringValue = null;
    private Set<String> stringSetValue = null;
    private Integer     intValue = null;
    private Long        longValue = null;
    private Float       floatValue = null;
    private Boolean     booleanValue = null;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public RobotControllerPreference(String prefName, Object value)
        {
        this.prefName = prefName;

        if (value instanceof String)            stringValue = (String)value;
        else if (value instanceof Boolean)      booleanValue = (Boolean)value;
        else if (value instanceof Integer)      intValue = (Integer)value;
        else if (value instanceof Long)         longValue = (Long)value;
        else if (value instanceof Float)        floatValue = (Float)value;
        else if (value instanceof Set)          stringSetValue = (Set<String>)value;
        }

    //----------------------------------------------------------------------------------------------
    // Serialization
    //----------------------------------------------------------------------------------------------

    public static RobotControllerPreference deserialize(String string)
        {
        return SimpleGson.getInstance().fromJson(string, RobotControllerPreference.class);
        }

    public String serialize()
        {
        return SimpleGson.getInstance().toJson(this);
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public String getPrefName()
        {
        return prefName;
        }

    public Object getValue()
        {
        if (stringValue != null) return stringValue;
        if (booleanValue != null) return booleanValue;
        if (intValue != null) return intValue;
        if (longValue != null) return longValue;
        if (floatValue != null) return floatValue;
        if (stringSetValue != null) return stringSetValue;
        return null;
        }
    }
