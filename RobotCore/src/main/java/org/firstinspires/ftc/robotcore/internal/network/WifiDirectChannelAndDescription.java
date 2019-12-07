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

import com.qualcomm.robotcore.R;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link WifiDirectChannelAndDescription} maintains human readable descriptions of Wifi Direct
 * channel names
 */
@SuppressWarnings("WeakerAccess")
public class WifiDirectChannelAndDescription implements Comparable<WifiDirectChannelAndDescription>
    {
    protected String description;
    protected int channel;

    public static List<WifiDirectChannelAndDescription> load()
        {
        String[] strings = AppUtil.getDefContext().getResources().getStringArray(R.array.wifi_direct_channels);
        List<WifiDirectChannelAndDescription> itemsList = new ArrayList<WifiDirectChannelAndDescription>();
        for (String string : strings)
            {
            itemsList.add(new WifiDirectChannelAndDescription(string));
            }
        return itemsList;
        }

    public static String getDescription(int channel)
        {
        for (WifiDirectChannelAndDescription channelAndDescription : load())
            {
            if (channelAndDescription.getChannel() == channel)
                {
                return channelAndDescription.getDescription();
                }
            }
        return AppUtil.getDefContext().getString(R.string.unknown_wifi_direct_channel);
        }

    public WifiDirectChannelAndDescription(String displayNameAndChannel)
        {
        String[] strings = displayNameAndChannel.split("\\|");
        this.description = strings[0];
        this.channel = Integer.parseInt(strings[1]);
        }

    public int getChannel()
        {
        return channel;
        }

    public String getDescription()
        {
        return description;
        }

    @Override public String toString()
        {
        return getDescription();
        }

    @Override public int compareTo(WifiDirectChannelAndDescription another)
        {
        return this.channel - another.channel;
        }
    }
