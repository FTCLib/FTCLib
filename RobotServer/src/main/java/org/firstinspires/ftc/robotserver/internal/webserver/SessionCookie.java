/*
Copyright (c) 2017 Robert Atkinson

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
package org.firstinspires.ftc.robotserver.internal.webserver;

import androidx.annotation.Nullable;

import com.qualcomm.robotcore.util.RobotLog;

import java.util.Locale;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD;

/**
 * {@link SessionCookie} enhances NanoHTTPD cookies so as to be dynamic to client sessions,
 * rather than client hosts.
 *
 * Note that 'session cookie' is a technical term in the parlance of HTTP cookie
 * architecture.
 */
@SuppressWarnings("WeakerAccess")
public class SessionCookie extends NanoHTTPD.Cookie
    {
    public static final String TAG = SessionCookie.class.getSimpleName();

    protected static final String sessionCookieName = "consoleSession";

    public SessionCookie(String name, String value)
        {
        super(name, value, "");
        }

    public static void ensureInSession(NanoHTTPD.IHTTPSession session)
        {
        String sessionCookie = fromSessionInternal(session);
        if (null == sessionCookie)
            {
            sessionCookie = UUID.randomUUID().toString();
            session.getCookies().set(new SessionCookie(sessionCookieName, sessionCookie));
            RobotLog.vv(TAG, "added SessionCookie: cookie=%s uri='%s'", sessionCookie, session.getUri());
            }
        }

    protected static @Nullable String fromSessionInternal(NanoHTTPD.IHTTPSession session)
        {
        return session.getCookies().read(sessionCookieName);
        }

    // If all is well with the client, in that it honors our session cookies, then
    // this should never return null. But not all clients are well behaved. Sigh.
    public static @Nullable String fromSession(NanoHTTPD.IHTTPSession session)
        {
        String result = fromSessionInternal(session);
        if (result == null)
            {
            RobotLog.ee(TAG, "session cookie unexpectedly null uri=%s", session.getUri());
            }
        return result;
        }

    @Override public String getHTTPHeader()
        {
        if (this.e == null || this.e.length() == 0)
            {
            return String.format(Locale.US, "%s=%s", this.n, this.v);
            }
        else
            {
            return super.getHTTPHeader();
            }
        }
    }
