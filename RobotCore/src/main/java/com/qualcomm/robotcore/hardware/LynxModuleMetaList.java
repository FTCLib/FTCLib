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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * {@link LynxModuleMetaList} is a container of {@link RobotCoreLynxModule}s.
 * Its primary use is for transmission of module information from RC to DS.
 */
public class LynxModuleMetaList implements Iterable<LynxModuleMeta>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public SerialNumber     serialNumber; // the serial number of the LynxUSBDevice
    public LynxModuleMeta[] modules;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxModuleMetaList(SerialNumber serialNumber)
        {
        this(serialNumber, new LynxModuleMeta[0]);
        }

    public LynxModuleMetaList(SerialNumber serialNumber, Collection<LynxModuleMeta> modules)
        {
        this(serialNumber, metaFromModules(modules));
        }

    public LynxModuleMetaList(SerialNumber serialNumber, List<RobotCoreLynxModule> modules)
        {
        this(serialNumber, metaFromModules(modules));
        }

    private static LynxModuleMeta[] metaFromModules(List<RobotCoreLynxModule> modules)
        {
        LynxModuleMeta[] result = new LynxModuleMeta[modules.size()];
        for (int i=0; i < result.length; i++)
            {
            result[i] = new LynxModuleMeta(modules.get(i));
            }
        return result;
        }

    private static LynxModuleMeta[] metaFromModules(Collection<LynxModuleMeta> modules)
        {
        LynxModuleMeta[] result = new LynxModuleMeta[modules.size()];
        int i = 0;
        for (LynxModuleMeta meta : modules)
            {
            result[i++] = meta;
            }
        return result;
        }

    private LynxModuleMetaList(SerialNumber serialNumber, LynxModuleMeta[] modules)
        {
        this.serialNumber = serialNumber;
        this.modules = modules;
        }

    //----------------------------------------------------------------------------------------------
    // Iteration and access
    //----------------------------------------------------------------------------------------------

    @Override public Iterator<LynxModuleMeta> iterator()
        {
        return Arrays.asList(this.modules).iterator();
        }

    public LynxModuleMeta getParent()
        {
        for (int i = 0; i < modules.length; i++)
            {
            LynxModuleMeta meta = modules[i];
            if (meta.isParent())
                {
                return meta;
                }
            }
        return null;
        }

    //----------------------------------------------------------------------------------------------
    // Serialization
    //----------------------------------------------------------------------------------------------

    protected LynxModuleMetaList flatten()
        {
        LynxModuleMeta[] flatModules = new LynxModuleMeta[this.modules.length];
        for (int i = 0; i < this.modules.length; i++)
            {
            flatModules[i] = new LynxModuleMeta(this.modules[i]);
            }
        return new LynxModuleMetaList(serialNumber, flatModules);
        }

    public String toSerializationString()
        {
        return SimpleGson.getInstance().toJson(this.flatten());
        }

    public static LynxModuleMetaList fromSerializationString(String serialization)
        {
        JsonDeserializer deserializer = new JsonDeserializer()
            {
            @Override public Object deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException
                {
                return context.deserialize(json, LynxModuleMeta.class);
                }
            };

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(RobotCoreLynxModule.class, deserializer)
                .create();

        return gson.fromJson(serialization, LynxModuleMetaList.class);
        }

    //----------------------------------------------------------------------------------------------
    // Formatting
    //----------------------------------------------------------------------------------------------

    public @Override String toString()
        {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        result.append("[");
        for (LynxModuleMeta module : this.modules)
            {
            if (!first) result.append(" ");
            result.append(String.format(Locale.getDefault(), "%d(%s)", module.getModuleAddress(), module.isParent()));
            first = false;
            }
        result.append("]");
        return result.toString();
        }
    }
