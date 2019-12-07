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

package org.firstinspires.ftc.onbotjava.handlers.javascript;

import org.firstinspires.ftc.onbotjava.*;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;

import fi.iki.elonen.NanoHTTPD;
import org.firstinspires.ftc.robotserver.internal.webserver.MimeTypesUtil;

@RegisterWebHandler(uri = OnBotJavaProgrammingMode.URI_JS_SETTINGS)
public class FetchJavaScriptSettings implements WebHandler {
    /* Template
    (function(window, $, settings) {
        window.env = typeof window.env !== 'undefined' ? window.env : {},
        env.settings = settings;
        env.settings['_dict'] = env.settings.hasOwnProperty('_dict') ? env.settings._dict : {};
        var dict = env.settings._dict;

        settings.get = function(name) {
            return typeof dict[name] === 'undefined' ? null : dict[name];
        }

        settings.put = function(name, val) {
            if (typeof name === 'undefined' || typeof val === 'undefined') throw new Error('put doesn\' work with name or val undefined');
            if (settings.get(name) === null) { console.warn(name + " is not a valid setting"); return; }
            dict[name] = typeof val === 'function' ? val() : val;
            return $.post(settings._settingsUrl, 'settings=' + window.JSON.stringify(dict));
        }
    })(window, jQuery,
    {
        _dict: window.JSON.parse('%s'),
        _settingsUrl: '%s'
    });
     */
    @Override
    public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) {
        final String editorSettings = OnBotJavaWebInterfaceManager.instance().editorSettings().toJSON();
        HashMap<String, String> onBotJavaUrls = new HashMap<>();

        for (Field field : OnBotJavaProgrammingMode.class.getDeclaredFields()) {
            recordField(onBotJavaUrls, field);
        }

        for (Field field : RequestConditions.class.getDeclaredFields()) {
            recordField(onBotJavaUrls, field);
        }

        final String result = String.format(Locale.ENGLISH,
                "(function(window, $, settings) {\n" +
                "	window.env = typeof window.env !== 'undefined' ? window.env : {},\n" +
                "	env.settings = settings;\n" +
                "	env.settings['_dict'] = env.settings.hasOwnProperty('_dict') ? env.settings._dict : {};\n" +
                "	var dict = env.settings._dict;\n" +
                "\n" +
                "	settings.get = function(name) {\n" +
                "		return typeof dict[name] === 'undefined' ? null : dict[name];\n" +
                "	}\n" +
                "\n" +
                "	settings.put = function(name, val) {\n" +
                "		if (typeof name === 'undefined' || typeof val === 'undefined') throw new Error('put doesn\\' work with name or val undefined');\n" +
                "		if (settings.get(name) === null) { console.warn(name + \" is not a valid setting\"); return; }\n" +
                "		dict[name] = typeof val === 'function' ? val() : val;\n" +
                        "		return $.post(settings._settingsUrl, 'settings=' + window.JSON.stringify(dict));\n" +
                "   }\n" +
                "\n" +
                "   env.urls = JSON.parse(settings._urls)" +
                "})(window, jQuery, \n" +
                "{\n" +
                "	_dict: window.JSON.parse('%s'),\n" +
                "	_settingsUrl: '%s',\n" +
                "   _urls: '%s'\n" +
                "});",
                editorSettings, OnBotJavaProgrammingMode.URI_ADMIN_SETTINGS, SimpleGson.getInstance().toJson(onBotJavaUrls).replace("\\", "\\\\"));
        return StandardResponses.successfulRequest(MimeTypesUtil.MIME_JAVASCRIPT, result);
    }

    private void recordField(HashMap<? super String, ? super String> onBotJavaUrls, Field field) {
        int modifiers = field.getModifiers();
        if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers) || !field.getType().equals(String.class)){
            return;
        }

        field.setAccessible(true);
        try {
            onBotJavaUrls.put(field.getName(), (String) field.get(null));
        } catch (IllegalAccessException ignored) { /// should not be thrown

        }
    }
}
