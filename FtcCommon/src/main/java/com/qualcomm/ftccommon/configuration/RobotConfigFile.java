/*
 * Copyright (c) 2015 Craig MacFarlane
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Craig MacFarlane nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.qualcomm.ftccommon.configuration;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;

import com.google.gson.JsonSyntaxException;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.Collection;

/**
 * Metadata for a config file.
 *
 * If this is modified, then the Robocol version number needs to be bumped as this
 * is shared via serialization with the DriverStation.
 */
@SuppressWarnings("WeakerAccess")
public class RobotConfigFile {

    public enum FileLocation {
        NONE,
        LOCAL_STORAGE,
        RESOURCE,
    };

    private final static String LOGGER_TAG = "RobotConfigFile";

    private String name;
    private @XmlRes int resourceId;
    private FileLocation location;
    private boolean isDirty;

    public static RobotConfigFile noConfig(RobotConfigFileManager configFileManager)
    {
        return new RobotConfigFile(configFileManager, configFileManager.noConfig);
    }

    public RobotConfigFile(RobotConfigFileManager configFileManager, String name)
    {
        this.name = RobotConfigFileManager.stripFileNameExtension(name);
        this.resourceId = 0;
        this.location = this.name.equalsIgnoreCase(configFileManager.noConfig) ? FileLocation.NONE : FileLocation.LOCAL_STORAGE;
        this.isDirty = false;
    }

    public RobotConfigFile(String name, @XmlRes int resourceId)
    {
        this.name = name;
        this.resourceId = resourceId;
        this.location = FileLocation.RESOURCE;
        this.isDirty = false;
    }

    public boolean isReadOnly()
    {
        return location==FileLocation.RESOURCE || location==FileLocation.NONE;
    }

    public boolean containedIn(Collection<RobotConfigFile> configFiles)
    {
        for (RobotConfigFile him : configFiles) {
            if (him.name.equalsIgnoreCase(this.name)) {
                return true;
            }
        }
        return false;
    }

    public void markDirty()
    {
        isDirty = true;
    }

    public void markClean()
    {
        isDirty = false;
    }

    public boolean isDirty()
    {
        return isDirty;
    }

    public String getName()
    {
        return name;
    }

    public File getFullPath()
    {
        return RobotConfigFileManager.getFullPath(this.getName());
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public FileLocation getLocation()
    {
        return location;
    }

    public @Nullable XmlPullParser getXml()
    {
        switch (location) {
        case LOCAL_STORAGE:
            return getXmlLocalStorage();
        case RESOURCE:
            return getXmlResource();
        case NONE:
            return getXmlNone();
        }
        return null;
    }

    // In the 'none' case we simply return an XmlPullParser on a degenerate, empty, configuration
    protected XmlPullParser getXmlNone()
    {
        XmlPullParser result = null;
        try {
            String emptyXml = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n" +
                    "<Robot type=\"FirstInspires-FTC\">\n" + "</Robot>\n";
            StringReader stringReader = new StringReader(emptyXml);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            result = factory.newPullParser();
            result.setInput(stringReader);
        }
        catch (XmlPullParserException e) {
            RobotLog.logStacktrace(e);
        }
        return result;
    }

    protected XmlPullParser getXmlLocalStorage()
    {
        FileInputStream inputStream;
        XmlPullParserFactory factory;
        XmlPullParser parser;

        parser = null;
        try {
            File fullPath = RobotConfigFileManager.getFullPath(getName());
            inputStream = new FileInputStream(fullPath);
            factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            parser = factory.newPullParser();
            parser.setInput(inputStream, null);
        } catch (XmlPullParserException|FileNotFoundException e) {
            RobotLog.logStacktrace(e);
        }

        return parser;
    }

    protected XmlPullParser getXmlResource()
    {
        Context context = AppUtil.getInstance().getApplication();
        return context.getResources().getXml(resourceId);
    }

    public boolean isNoConfig()
    {
        return (location == FileLocation.NONE);
    }

    public @NonNull String toString()
    {
        return SimpleGson.getInstance().toJson(this);
    }

    /*
     * Gson will return null if the string is "null", and it will throw an exception if it
     * can't parse the json into the appropriate object.  In both cases, we will return a new,
     * empty RobotConfigFile.  Otherwise return the parsed object.
     */
    public static @NonNull RobotConfigFile fromString(RobotConfigFileManager configFileManager, String serializedForm)
    {
        try {
            RobotConfigFile file = SimpleGson.getInstance().fromJson(serializedForm, RobotConfigFile.class);
            if (file == null) {
                return noConfig(configFileManager);
            } else {
                return file;
            }
        } catch (JsonSyntaxException e) {
            RobotLog.ee(LOGGER_TAG, "Could not parse the stored config file data from shared settings");
            return noConfig(configFileManager);
        }
    }
}
