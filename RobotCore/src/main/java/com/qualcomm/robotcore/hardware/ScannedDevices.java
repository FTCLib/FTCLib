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
package com.qualcomm.robotcore.hardware;

import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.qualcomm.robotcore.util.SerialNumber;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * {@link ScannedDevices} is a simple distinguished kind of map of serial numbers
 * to device types. Simple serialization and deserialization logic is provided.
 */
@SuppressWarnings("WeakerAccess")
public class ScannedDevices
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "ScannedDevices";

    protected final Object lock = new Object();
    protected @Expose String errorMessage = null;
    protected @Expose @JsonAdapter(MapAdapter.class) Map<SerialNumber, DeviceManager.UsbDeviceType> map = new HashMap<>();

    /** There *has* to be an easier way here, somehow. */
    protected static class MapAdapter extends TypeAdapter<Map<SerialNumber,DeviceManager.UsbDeviceType>>
        {
        @Override public void write(JsonWriter writer, Map<SerialNumber, DeviceManager.UsbDeviceType> map) throws IOException
            {
            writer.beginArray();
            for (Map.Entry<SerialNumber,DeviceManager.UsbDeviceType> entry : map.entrySet())
                {
                writer.beginObject();
                writer.name("key").value(entry.getKey().getString());
                writer.name("value").value(entry.getValue().toString());
                writer.endObject();
                }
            writer.endArray();
            }

        @Override public Map<SerialNumber, DeviceManager.UsbDeviceType> read(JsonReader reader) throws IOException
            {
            HashMap<SerialNumber,DeviceManager.UsbDeviceType> result = new HashMap<>();
            reader.beginArray();
            while (reader.hasNext())
                {
                SerialNumber serialNumber = null;
                DeviceManager.UsbDeviceType deviceType = null;
                reader.beginObject();
                while (reader.hasNext())
                    {
                    String name = reader.nextName();
                    switch (name)
                        {
                        case "key":
                            serialNumber = SerialNumber.fromString(reader.nextString());
                            break;
                        case "value":
                            deviceType = DeviceManager.UsbDeviceType.from(reader.nextString());
                            break;
                        default:
                            reader.skipValue();
                            break;
                        }
                    }
                reader.endObject();
                if (serialNumber != null && deviceType != null)
                    {
                    result.put(serialNumber, deviceType);
                    }
                }
            reader.endArray();
            return result;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ScannedDevices(ScannedDevices them)
        {
        this.map.clear();
        for (Map.Entry<SerialNumber,DeviceManager.UsbDeviceType> entry : them.entrySet())
            {
            this.map.put(entry.getKey(), entry.getValue());
            }
        this.errorMessage = them.errorMessage;
        }

    public ScannedDevices()
        {
        super();
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public void setErrorMessage(String errorMessage)
        {
        synchronized (lock)
            {
            if (TextUtils.isEmpty(this.errorMessage))
                {
                this.errorMessage = errorMessage;
                }
            }
        }

    public @Nullable String getErrorMessage()
        {
        return errorMessage;
        }

    public int size()
        {
        synchronized (lock)
            {
            return map.size();
            }
        }

    public DeviceManager.UsbDeviceType put(SerialNumber serialNumber, DeviceManager.UsbDeviceType deviceType)
        {
        synchronized (lock)
            {
            return map.put(serialNumber, deviceType);
            }
        }

    public DeviceManager.UsbDeviceType get(SerialNumber serialNumber)
        {
        synchronized (lock)
            {
            return map.get(serialNumber);
            }
        }

    public boolean containsKey(SerialNumber serialNumber)
        {
        synchronized (lock)
            {
            return map.containsKey(serialNumber);
            }
        }

    public Set<SerialNumber> keySet()
        {
        synchronized (lock)
            {
            return map.keySet();
            }
        }

    public Set<Map.Entry<SerialNumber,DeviceManager.UsbDeviceType>> entrySet()
        {
        synchronized (lock)
            {
            return map.entrySet();
            }
        }

    public DeviceManager.UsbDeviceType remove(SerialNumber serialNumber)
        {
        synchronized (lock)
            {
            return map.remove(serialNumber);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Serialization
    //----------------------------------------------------------------------------------------------

    protected static Gson newGson()
        {
        return new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        }

    public String toSerializationString()
        {
        Gson gson = newGson();
        return gson.toJson(this, ScannedDevices.class);
        }

    public static ScannedDevices fromSerializationString(String string)
        {
        ScannedDevices result = new ScannedDevices();
        //
        string = string.trim(); // paranoia
        if (string.length() > 0)
            {
            Gson gson = newGson();
            result = gson.fromJson(string, ScannedDevices.class);
            }
        //
        return result;
        }
    }
