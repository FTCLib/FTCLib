/*
Copyright (c) 2018 Noah Andrews

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Noah Andrews nor the names of his contributors may be used to
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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * GSON TypeAdapter for the BuiltInConfigurationType enum
 */

public class BuiltInConfigurationTypeJsonAdapter extends TypeAdapter<BuiltInConfigurationType> {

    @Override
    public BuiltInConfigurationType read(JsonReader reader) throws IOException {
        String xmlTag = null;

        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        reader.beginObject();

        while (reader.hasNext()) {
            if (reader.nextName().equals("xmlTag")) {
                xmlTag = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return BuiltInConfigurationType.fromXmlTag(xmlTag);
    }

    @Override
    public void write(JsonWriter writer, BuiltInConfigurationType configurationType) throws IOException {
        if (configurationType == null) {
            writer.nullValue();
            return;
        }
        writer.beginObject();
        writer.name("xmlTag").value(configurationType.getXmlTag());
        writer.name("name").value(configurationType.getDisplayName(ConfigurationType.DisplayNameFlavor.Normal));
        writer.name("flavor").value(ConfigurationType.DeviceFlavor.BUILT_IN.toString());
        writer.endObject();
    }
}
