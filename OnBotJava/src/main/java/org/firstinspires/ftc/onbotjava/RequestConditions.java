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

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public final class RequestConditions {
    public static final String REQUEST_KEY_FILE = "f";
    public static final String REQUEST_KEY_NEW = "new";
    public static final String REQUEST_KEY_SAVE = "data";
    public static final String REQUEST_KEY_TEMPLATE = "template";
    public static final String REQUEST_KEY_OPMODE_NAME = "opModeName";
    public static final String REQUEST_KEY_TEAM_NAME = "teamName";
    public static final String REQUEST_KEY_SETUP_HARDWARE = "rcSetupHardware";
    public static final String REQUEST_KEY_OPMODE_ANNOTATIONS = "opModeAnnotations";
    public static final String REQUEST_KEY_ID = "id";
    public static final String REQUEST_KEY_COPY_FROM = "origin";
    public static final String REQUEST_KEY_COPY_TO = "dest";
    public static final String REQUEST_KEY_DELETE = "delete";
    public static final String REQUEST_KEY_PRESERVE = "preserve";

    private RequestConditions() {

    }

    public static boolean containsParameters(@NonNull NanoHTTPD.IHTTPSession session, @NonNull String... parameters) {
        final Map<String, List<String>> data = session.getParameters();
        for (final String parameter : parameters) {
            if (!data.containsKey(parameter)) return false;
        }

        return true;
    }

    public static String dataForParameter(@NonNull NanoHTTPD.IHTTPSession session, @NonNull String parameter) {
        final Map<String, List<String>> data = session.getParameters();
        if (!data.containsKey(parameter)) {
            throwBecauseParameterMissing(parameter);
        }

        return data.get(parameter).get(0);
    }

    private static void throwBecauseParameterMissing(@NonNull String parameterName) {
        if (parameterName == null) throw new NullPointerException();
        throw new RuntimeException(String.format(Locale.ENGLISH, "Missing parameter \"%s\" from payload", parameterName));
    }
}
