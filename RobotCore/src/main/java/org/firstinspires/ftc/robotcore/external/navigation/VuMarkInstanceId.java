/*
Copyright (c) 2017 Robert Atkinson

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
package org.firstinspires.ftc.robotcore.external.navigation;

import com.vuforia.InstanceId;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * {@link VuMarkInstanceId} captures the identifying state decoded from a particular instance
 * of a Vuforia VuMark.
 *
 * @see com.vuforia.VuMarkTarget
 * @see com.vuforia.InstanceId
 */
@SuppressWarnings("WeakerAccess")
public class VuMarkInstanceId
    {
    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    /**
     * {@link Type} indicates the type of data that was found in the {@link InstanceId}
     * from which this data was decoded.
     *
     * @see #getType()
     */
    public enum Type
        {
            UNKNOWN,
            NUMERIC,
            STRING,
            DATA;
        }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected Type type;
    protected int numericValue;
    protected String stringValue;
    protected byte[] dataValue;

    @Override
    public String toString()
        {
        return "VuMarkInstanceId(" + this.getType() + ", " + this.getValue() + ")";
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public VuMarkInstanceId(InstanceId instanceId)
        {
        this.type = typeFrom(instanceId);
        switch (this.type)
            {
            case NUMERIC: this.numericValue = instanceId.getNumericValue().intValue(); break;
            case STRING: this.stringValue = new String(dataFrom(instanceId), Charset.forName("US-ASCII")); break;
            case DATA: this.dataValue = dataFrom(instanceId); break;
            }
        }

    protected static byte[] dataFrom(InstanceId instanceId)
        {
        ByteBuffer buffer = instanceId.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
        }

    protected static Type typeFrom(InstanceId instanceId)
        {
        switch (instanceId.getDataType())
            {
            case InstanceId.ID_DATA_TYPE.STRING:    return Type.STRING;
            case InstanceId.ID_DATA_TYPE.NUMERIC:   return Type.NUMERIC;
            case InstanceId.ID_DATA_TYPE.BYTES:     return Type.DATA;
            default:                                return Type.UNKNOWN;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Comparison
    //----------------------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (o instanceof VuMarkInstanceId)
            {
            VuMarkInstanceId them = (VuMarkInstanceId)o;
            if (this.getType() == them.getType())
                {
                switch (this.getType())
                    {
                    case STRING: return getStringValue().equals(them.getStringValue());
                    case NUMERIC: return getNumericValue() == them.getNumericValue();
                    case DATA: return Arrays.equals(getDataValue(), them.getDataValue());
                    }
                }
            }
        return false;
        }

    @Override
    public int hashCode()
        {
        switch (this.getType())
            {
            case STRING: return getStringValue().hashCode() ^ 0x55adef;
            case NUMERIC: return getNumericValue() ^ 0x55adef;
            case DATA: return Arrays.hashCode(getDataValue()) ^ 0x55adef;
            }
        return super.hashCode();
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public Type getType()
        {
        return type;
        }

    int getNumericValue()
        {
        return numericValue;
        }

    String getStringValue()
        {
        return stringValue;
        }

    byte[] getDataValue()
        {
        return dataValue;
        }

    Object getValue()
        {
        switch (this.getType())
            {
            case STRING: return getStringValue();
            case NUMERIC: return getNumericValue();
            case DATA: return getDataValue();
            default: return null;
            }
        }
    }
