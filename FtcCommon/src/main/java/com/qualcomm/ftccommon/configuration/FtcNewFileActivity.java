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
package com.qualcomm.ftccommon.configuration;

import android.os.Bundle;

/**
 * FtcNewFileActivity is instantiated when the 'New' configuration button is pressed.
 * It's like any other config XML file editor, except that it auto-populates its
 * contents from a scan of the USB bus on startup.
 */
public class FtcNewFileActivity extends FtcConfigurationActivity
    {
    public static final RequestCode requestCode = RequestCode.NEW_FILE;

    @Override protected void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);

        // Populate the list on creation
        dirtyCheckThenSingletonUSBScanAndUpdateUI(false);
        }

    @Override protected void ensureConfigFileIsFresh()
        {
        // We do nothing here: we're a new configuration, so there's nothing to
        // retrieve. This is important in the remoteConfig case, as the auto usb scan
        // we do just above would race with the retrieval and population of the unneeded
        // old config that was active when we launched, which might include lynx usb devices,
        // whose discovery thereon might race with the auto usb scan and cause problems.
        }
    }
