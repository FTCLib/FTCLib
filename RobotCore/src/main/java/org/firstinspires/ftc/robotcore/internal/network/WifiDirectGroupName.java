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

import android.net.wifi.p2p.WifiP2pGroup;
import androidx.annotation.NonNull;

import com.google.gson.reflect.TypeToken;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A simple utility for manipulating Wifi Direct group names, particularly
 * names of remembered Wifi Direct groups.
 */
@SuppressWarnings("WeakerAccess")
public class WifiDirectGroupName implements Comparable<WifiDirectGroupName>
    {
    protected String name;

    public WifiDirectGroupName(@NonNull WifiP2pGroup group)
        {
        this.name = group.getNetworkName();
        }
    public WifiDirectGroupName(@NonNull String name)
        {
        this.name = name;
        }

    public static List<WifiDirectGroupName> namesFromGroups(Collection<WifiP2pGroup> groups)
        {
        List<WifiDirectGroupName> result = new ArrayList<WifiDirectGroupName>();
        for (WifiP2pGroup group : groups)
            {
            result.add(new WifiDirectGroupName(group));
            }
        return result;
        }

    @Override public String toString()
        {
        return this.name;
        }

    @Override public int compareTo(WifiDirectGroupName another)
        {
        return this.toString().compareTo(another.toString());
        }

    public static String serializeNames(Collection<WifiP2pGroup> groups)
        {
        return serializeNames(namesFromGroups(groups));
        }

    public static String serializeNames(List<WifiDirectGroupName> names)
        {
        return SimpleGson.getInstance().toJson(names);
        }

    public static List<WifiDirectGroupName> deserializeNames(String string)
        {
        Type listType = new TypeToken<ArrayList<WifiDirectGroupName>>(){}.getType();
        return SimpleGson.getInstance().fromJson(string, listType);
        }
    }
