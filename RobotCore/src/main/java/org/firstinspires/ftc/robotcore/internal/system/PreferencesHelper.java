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
package org.firstinspires.ftc.robotcore.internal.system;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;

import java.util.HashMap;
import java.util.Set;

/**
 * A little utility to help with the generic reading and writing of preferences
 */
@SuppressWarnings("WeakerAccess")
public class PreferencesHelper
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected final String            tag;
    protected final SharedPreferences sharedPreferences;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public PreferencesHelper(String tag)
        {
        this(tag, AppUtil.getDefContext());
        }

    public PreferencesHelper(String tag, Context context)
        {
        this(tag, PreferenceManager.getDefaultSharedPreferences(context));
        }

    public PreferencesHelper(String tag, SharedPreferences sharedPreferences)
        {
        this.tag = tag;
        this.sharedPreferences = sharedPreferences;
        }

    public SharedPreferences getSharedPreferences()
        {
        return sharedPreferences;
        }

    //----------------------------------------------------------------------------------------------
    // Generic accessing
    //----------------------------------------------------------------------------------------------
    
    /** Returns the value of the preference with the indicated name, or null if it is absent.
     * Note: this does *not* automatically deserialize StringMaps, as we have no way of
     * distinguishing them from just plain ordinary strings. */
    public Object readPref(String prefName)
        {
        return sharedPreferences.getAll().get(prefName);
        }

    public boolean writePrefIfDifferent(String prefName, @NonNull Object newValue)
        {
        if (newValue instanceof String)    return writeStringPrefIfDifferent(prefName, (String)newValue);
        if (newValue instanceof Boolean)   return writeBooleanPrefIfDifferent(prefName, (Boolean)newValue);
        if (newValue instanceof Integer)   return writeIntPrefIfDifferent(prefName, (Integer)newValue);
        if (newValue instanceof Long)      return writeLongPrefIfDifferent(prefName, (Long)newValue);
        if (newValue instanceof Float)     return writeFloatPrefIfDifferent(prefName, (Float)newValue);
        if (newValue instanceof StringMap) return writeStringMapPrefIfDifferent(prefName, (StringMap)newValue);
        if (newValue instanceof Set)       return writeStringSetPrefIfDifferent(prefName, (Set<String>)newValue);
        return false;
        }

    //----------------------------------------------------------------------------------------------
    // Type-specific Accessing
    //----------------------------------------------------------------------------------------------

    public String       readString(String prefName, String def)         { return sharedPreferences.getString(prefName, def); }
    public Set<String>  readStringSet(String prefName, Set<String> def) { return sharedPreferences.getStringSet(prefName, def); }
    public int          readInt(String prefName, int def)               { return sharedPreferences.getInt(prefName, def); }
    public long         readLong(String prefName, long def)             { return sharedPreferences.getLong(prefName, def); }
    public float        readFloat(String prefName, float def)           { return sharedPreferences.getFloat(prefName, def); }
    public boolean      readBoolean(String prefName, boolean def)       { return sharedPreferences.getBoolean(prefName, def); }
    public StringMap    readStringMap(String prefName, StringMap def)
        {
        StringMap result = StringMap.deserialize(readString(prefName, null));
        return result != null ? result : def;
        }

    public boolean      contains(String prefName)                       { return sharedPreferences.contains(prefName); }

    public void remove(String prefName)
        {
        sharedPreferences.edit()
                .remove(prefName)
                .apply();
        }

    public boolean writeStringPrefIfDifferent(String prefName, @NonNull String newValue)
        {
        Assert.assertNotNull(newValue);
        boolean changed = false;
        for (;;)
            {
            if (contains(prefName) && newValue.equals(readString(prefName, "")))
                break;
            logWrite(prefName, newValue);
            sharedPreferences.edit()
                    .putString(prefName, newValue)
                    .apply();
            changed = true;
            }
        return changed;
        }

    public boolean writeStringSetPrefIfDifferent(String prefName, @NonNull Set<String> newValue)
        {
        Assert.assertNotNull(newValue);
        boolean changed = false;
        for (;;)
            {
            if (contains(prefName) && newValue.equals(readStringSet(prefName, (Set<String>)null)))
                break;
            logWrite(prefName, newValue);
            sharedPreferences.edit()
                    .putStringSet(prefName, newValue)
                    .apply();
            changed = true;
            }
        return changed;
        }

    public boolean writeIntPrefIfDifferent(String prefName, int newValue)
        {
        Assert.assertNotNull(newValue);
        boolean changed = false;
        for (;;)
            {
            if (contains(prefName) && newValue==readInt(prefName, 0))
                break;
            logWrite(prefName, newValue);
            sharedPreferences.edit()
                    .putInt(prefName, newValue)
                    .apply();
            changed = true;
            }
        return changed;
        }

    public boolean writeLongPrefIfDifferent(String prefName, long newValue)
        {
        Assert.assertNotNull(newValue);
        boolean changed = false;
        for (;;)
            {
            if (contains(prefName) && newValue==readLong(prefName, 0L))
                break;
            logWrite(prefName, newValue);
            sharedPreferences.edit()
                    .putLong(prefName, newValue)
                    .apply();
            changed = true;
            }
        return changed;
        }

    public boolean writeFloatPrefIfDifferent(String prefName, float newValue)
        {
        Assert.assertNotNull(newValue);
        boolean changed = false;
        for (;;)
            {
            if (contains(prefName) && newValue==readFloat(prefName, 0F))
                break;
            logWrite(prefName, newValue);
            sharedPreferences.edit()
                    .putFloat(prefName, newValue)
                    .apply();
            changed = true;
            }
        return changed;
        }

    public boolean writeBooleanPrefIfDifferent(String prefName, boolean newValue)
        {
        Assert.assertNotNull(newValue);
        boolean changed = false;
        for (;;)
            {
            if (contains(prefName) && newValue==readBoolean(prefName, false))
                break;
            logWrite(prefName, newValue);
            sharedPreferences.edit()
                    .putBoolean(prefName, newValue)
                    .apply();
            changed = true;
            }
        return changed;
        }

    public boolean writeStringMapPrefIfDifferent(String prefName, StringMap newValue)
        {
        return writeStringPrefIfDifferent(prefName, newValue.serialize());
        }

    protected void logWrite(String prefName, Object value)
        {
        RobotLog.vv(tag, "writing pref name=%s value=%s", prefName, value);
        }

    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    public static class StringMap extends HashMap<String, String>
        {
        public String serialize()
            {
            return SimpleGson.getInstance().toJson(this);
            }
        public static StringMap deserialize(String serialized)
            {
            if (serialized == null)
                return null;
            else
                return SimpleGson.getInstance().fromJson(serialized, StringMap.class);
            }
        }

    }
