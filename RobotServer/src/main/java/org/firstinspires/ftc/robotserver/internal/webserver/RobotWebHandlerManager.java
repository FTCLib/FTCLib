/*
 * Copyright (c) 2017 DEKA Research and Development
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
 * Neither the name of DEKA Research and Development nor the names of contributors may be used to
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

package org.firstinspires.ftc.robotserver.internal.webserver;

import android.content.res.AssetManager;

import com.qualcomm.robotcore.util.RobotLog;

import com.qualcomm.robotcore.util.WebHandlerManager;
import com.qualcomm.robotcore.util.WebServer;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import fi.iki.elonen.NanoHTTPD;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;
import org.firstinspires.ftc.robotcore.internal.webserver.WebObserver;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

/**
 * A manager for registering {@link WebHandler} objects (it's not a *service*, as a service is
 * a formal Android system concept).
 */
@SuppressWarnings("WeakerAccess")
public final class RobotWebHandlerManager implements WebHandlerManager {
    public static final String TAG = RobotWebHandlerManager.class.getSimpleName();

    private final Map<String, WebHandler> handlerMap = new ConcurrentHashMap<>(37);
    private final Map<String, WebObserver> observersMap = new ConcurrentHashMap<>();
    private final WebHandler serveAsset = new ServeAsset();
    private final WebServer webServer;

    // common Response objects
    public static final NanoHTTPD.Response OK_RESPONSE =
            newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "");

    public static final NanoHTTPD.Response INTERNAL_ERROR_RESPONSE =
            newFixedLengthResponse(
                    NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "Internal Error");

    // convenience for error cases
    public static NanoHTTPD.Response internalErrorResponse(String tag, String format, Object... args)
    {
        String errorString = String.format(format, args);
        return internalErrorResponse(tag, errorString);
    }

    public static NanoHTTPD.Response internalErrorResponse(String tag, String errorString)
    {
        RobotLog.ee(tag, errorString);
        return newFixedLengthResponse(
                NanoHTTPD.Response.Status.INTERNAL_ERROR,
                NanoHTTPD.MIME_PLAINTEXT,
                errorString);
    }

    /**
     * ServeAsset.
     */
    private static class ServeAsset implements WebHandler
    {
        public static final String TAG = ServeAsset.class.getSimpleName();

        /** we serve static assets all with a fixed date stamp for better caching (and because its easy, so why not) */
        public static SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        public static String staticDateStamp;
        static {
            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            staticDateStamp = gmtFrmt.format(new Date());
        }

        // final reference is visible to all threads, sharing an AssetManager is also thread safe.
        private final AssetManager assetManager = AppUtil.getInstance().getRootActivity().getAssets();
        private final MimeTypesUtil.TypedPaths typedPaths = new MimeTypesUtil.TypedPaths();

        public ServeAsset()
        {
            // ".map" is a very generic extension, so for the .maps we have, we look at the whole path instead
            String mimeTypeJson = MimeTypesUtil.getMimeType("json");
            typedPaths.setMimeType("css/bootstrap.min.css.map", mimeTypeJson);
            typedPaths.setMimeType("css/bootstrap-theme.css.map", mimeTypeJson);
            typedPaths.setMimeType("css/bootstrap-theme.min.css.map", mimeTypeJson);
        }

        /**
         * Serve an asset from the file system.
         *
         * @param session one IHTTPSession that will return one Response
         * @return an "OK" Response if the asset was found and a "NOT FOUND" response otherwise.
         */
        @Override
        public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException
        {
            final String uri = session.getUri();
            final String path = uri.startsWith("/") ? uri.substring(1) : uri;
            String mimeType = typedPaths.determineMimeType(path);

            if (mimeType == null) {
                return RobotWebHandlerManager.internalErrorResponse(TAG, "Mime type unknown: uri='%s' path='%s'", uri, path);
            }

            InputStream inputStream;
            try {
                inputStream = assetManager.open(path);
            } catch (IOException e) {
                NanoHTTPD.Response response = newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "");
                response.addHeader("Date", staticDateStamp);
                return response;
            }
            return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, mimeType, inputStream);
        }
    }

    /**
     * Construct a {@link RobotWebHandlerManager}.
     */
    public RobotWebHandlerManager(WebServer webServer)
    {
        this.webServer = webServer;
        RobotControllerWebHandlers.initialize(this);
    }

    @Override
    public WebServer getWebServer()
    {
        return webServer;
    }

    @Override
    public void register(String command, WebHandler webHandler)
    {
        handlerMap.put(command, webHandler);
    }
    @Override
    public WebHandler getRegistered(String command)
    {
        return handlerMap.get(command);
    }

    @Override
    public void registerObserver(String key, WebObserver webObserver)
    {
        // We don't ourselves here need the key, but it allows replacement for clients
        observersMap.put(key, webObserver);
    }

    /**
     * Start to generate the Response for the request.
     *
     * @param session IHTTPSession to generate a Response from
     * @return Response for the request
     */
    NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session)
    {
        SessionCookie.ensureInSession(session);

        for (WebObserver observer : observersMap.values()) {
            observer.observe(session);
        }

        final String command = session.getUri();
        final WebHandler webHandler = handlerMap.get(command);

        try {
            if (webHandler == null) {
                return serveAsset.getResponse(session);
            } else {
                return webHandler.getResponse(session);
            }
        } catch (IOException e) {
            RobotLog.logStackTrace(e);
            return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Internal Error");
        } catch (NanoHTTPD.ResponseException e) {
            RobotLog.logStackTrace(e);
            return newFixedLengthResponse(e.getStatus(), NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
        }
    }

}
