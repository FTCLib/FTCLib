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

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;

/**
 * {@link NoCachingWebHandler} will prohibit the web response from being cached
 */
@SuppressWarnings("WeakerAccess")
public class NoCachingWebHandler extends WebHandlerDecorator
    {
    public NoCachingWebHandler(WebHandler delegate)
        {
        super(delegate);
        }

    @Override
    public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException
        {
        return setNoCache(session, super.getResponse(session));
        }

    public static NanoHTTPD.Response setNoCache(NanoHTTPD.IHTTPSession session, NanoHTTPD.Response response)
        {
        if (true || session.getMethod()==NanoHTTPD.Method.GET || session.getMethod()==NanoHTTPD.Method.HEAD)
            {
            // Only GETs and HEADs are cacheable in the first place
            response.addHeader("Cache-Control", "no-cache");
            // NanoHTTPD currently doesn't seem to support repeat headers in responses. We could
            // fix that, but that would fork us from the release. And it's not worth it: it's the
            // no-cache header that's most particularly important. So we just omit the others for now.
            // response.addHeader("Cache-Control", "no-store");
            // response.addHeader("Cache-Control", "must-revalidate");
            }
        return response;
        }
    }
