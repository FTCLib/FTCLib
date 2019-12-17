/*
 * Copyright (c) 2018 David Sargent
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of David Sargent nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.onbotjava;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.google.gson.reflect.TypeToken;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorSettings {
    private final Map<String, Object> settings;
    private final Map<String, Setting> nameToSettingMap;

    private EditorSettings() {
        settings = new HashMap<>();
        settings.put(Setting.FONT.name, "Source Code Pro");
        settings.put(Setting.THEME.name, "chrome");
        settings.put(Setting.FONT_SIZE.name, 16);
        settings.put(Setting.WHITESPACE.name, "space");
        settings.put(Setting.SPACES_TO_TAB.name, 4);
        settings.put(Setting.DEFAULT_PACKAGE.name, "org.firstinspires.ftc.teamcode");
        settings.put(Setting.AUTOCOMPLETE_ENABLED.name, true);
        settings.put(Setting.AUTOCOMPLETE_FORCE_ENABLE.name, false);
        settings.put(Setting.AUTOIMPORT_ENABLED.name, true);
        settings.put(Setting.KEYBINDING.name, "OnBotJava");
        settings.put(Setting.SHOW_PRINT_MARGIN.name, true);
        settings.put(Setting.SHOW_INVISIBLE_CHARS.name, false);
        settings.put(Setting.SOFT_WRAP.name, false);
        settings.put(Setting.AUTOCOMPLETE_PACKAGES.name, OnBotJavaWebInterfaceManager.packagesToAutocomplete());

        nameToSettingMap = new HashMap<>();
        for (Setting setting : Setting.values()) {
            nameToSettingMap.put(setting.name, setting);
        }
    }

    @SuppressWarnings("unchecked")
    private EditorSettings(Object map) {
        settings = (Map<String, Object>) map;
        nameToSettingMap = new HashMap<>();
        for (Setting setting : Setting.values()) {
            nameToSettingMap.put(setting.name, setting);
        }
    }

    EditorSettings(SharedPreferences preferences) {
        this();
        final SharedPreferences.Editor edit = preferences.edit();
        final Map<String, ?> prefMap = preferences.getAll();
        for (String key : settings.keySet()) {
            if (prefMap.containsKey(key)) {
                Object settingValue = nameToSettingMap.get(key).fromString(prefMap.get(key));
                settings.put(key, settingValue);
            } else {
                updateValue(edit, key);
            }
        }
        edit.apply();
    }

    private static EditorSettings parse(String json) {
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return new EditorSettings((OnBotJavaWebInterfaceManager.instance().gson().fromJson(json, type)));
    }

    private void updateValue(SharedPreferences.Editor edit, String key) {
        Object obj = settings.get(key);
        if (obj instanceof Integer) {
            edit.putInt(key, (Integer) obj);
        } else if (obj instanceof String) {
            edit.putString(key, (String) obj);
        } else if (obj instanceof Boolean) {
            edit.putBoolean(key, (Boolean) obj);
        } else {
            edit.putString(key, SimpleGson.getInstance().toJson(obj));
        }
    }

    public void parseAndUpdate(String json) {
        EditorSettings newEditorSettings = parse(json);
        update(newEditorSettings, OnBotJavaWebInterfaceManager.instance().sharedPrefs());
    }

    public enum Setting {
        AUTOCOMPLETE_ENABLED("autocompleteEnabled"),
        AUTOCOMPLETE_FORCE_ENABLE("autocompleteForceEnabled"),
        AUTOCOMPLETE_PACKAGES("autocompletePackages") {
            @NonNull
            @Override
            public Object fromString(@NonNull Object serialization) {
                try {
                    return SimpleGson.getInstance().fromJson((String) serialization, List.class);
                } catch (Exception ex) {
                    RobotLog.ee(EditorSettings.class.getName(), "autocomplete packages is corrupt");
                    return OnBotJavaWebInterfaceManager.packagesToAutocomplete();
                }
            }
        },
        AUTOIMPORT_ENABLED("autoImportEnabled"),
        DEFAULT_PACKAGE("defaultPackage"),
        FONT("font"),
        FONT_SIZE("fontSize"),
        KEYBINDING("keybinding"),
        SHOW_PRINT_MARGIN("printMargin"),
        SHOW_INVISIBLE_CHARS("invisibleChars"),
        SOFT_WRAP("softWrap"),
        SPACES_TO_TAB("spacesToTab"),
        THEME("theme"),
        WHITESPACE("whitespace");

        final String name;

        Setting(String name) {
            this.name = name;
        }

        @NonNull
        Object fromString(@NonNull Object serialization) {
            return serialization;
        }
    }

    private void update(EditorSettings settings, SharedPreferences preferences) {
        update(settings.settings, preferences);
    }

    private void update(Map<String, Object> updatedMap, SharedPreferences preferences) {
        final SharedPreferences.Editor editor = preferences.edit();
        for (String key : updatedMap.keySet()) {
            settings.put(key, updatedMap.get(key));
            updateValue(editor, key);
        }
        editor.apply();
    }

    private void trim(SharedPreferences preferences) {
        final Map<String, ?> prefMap = preferences.getAll();
        final SharedPreferences.Editor edit = preferences.edit();
        for (String key : prefMap.keySet()) {
            if (!settings.containsKey(key)) edit.remove(key);
        }
        edit.apply();
    }

    public Object get(Setting key) {
        return settings.get(key.name);
    }

    public void resetToDefaults() {
        SharedPreferences preferences = OnBotJavaWebInterfaceManager.instance().sharedPrefs();
        update(new EditorSettings().settings, preferences);
        trim(preferences);
    }

    public String toJSON() {
        return SimpleGson.getInstance().toJson(this.settings);
    }
}
