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

package org.firstinspires.ftc.onbotjava.handlers.objbuild;


import org.firstinspires.ftc.onbotjava.OnBotJavaProgrammingMode;
import org.firstinspires.ftc.onbotjava.OnBotJavaWebInterfaceManager;
import org.firstinspires.ftc.onbotjava.RegisterWebHandler;
import org.firstinspires.ftc.onbotjava.StandardResponses;

import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;

import fi.iki.elonen.NanoHTTPD;

@RegisterWebHandler(uri = OnBotJavaProgrammingMode.URI_BUILD_LAUNCH)
public class LaunchBuild implements WebHandler {
    @Override
    public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) {
        return launchBuild();
    }

    /**
     * <p>Called with {@link OnBotJavaProgrammingMode#URI_BUILD_LAUNCH}</p>
     *
     * @return a response with the current millisecond time if the launch was successful
     */
    private NanoHTTPD.Response launchBuild() {
    /*
    *      <li>control: Whenever the contents of the 'buildRequest.txt' file in this directory is
*                changed, a build is automatically kicked off it's not already running. Note
*                that the contents of the file don't matter, only the act of changing it. This
*                directory also contains a locking mechanism so as to allow clients to examine
*                the state of a successful build without having to worry that a new build will
*                be kicked off while they are doing so. See 'buildLock' below.</li>
     */


        return StandardResponses.successfulRequest(OnBotJavaWebInterfaceManager.instance().buildMonitor().launchBuild());
    }

}
