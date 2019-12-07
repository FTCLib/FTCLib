/*
Copyright (c) 2019 Noah Andrews

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Noah Andrews nor the names of his contributors may be used to
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
package org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater;

/**
 * Constants used in communication with the Control Hub Updater
 */
public final class UpdaterConstants {
    private UpdaterConstants() {
    }

    public static final String CONTROL_HUB_UPDATER_PACKAGE  = "com.revrobotics.controlhubupdater";
    public static final String CONTROL_HUB_UPDATE_SERVICE   = "com.revrobotics.controlhubupdater.UpdateService";
    public static final String ACTION_APPLY_OTA_UPDATE      = "com.revrobotics.controlhubupdater.action.APPLY_OTA_UPDATE";
    public static final String ACTION_UPDATE_FTC_APP        = "com.revrobotics.controlhubupdater.action.UPDATE_FTC_APP";
    public static final String EXTRA_UPDATE_FILE_PATH       = "com.revrobotics.controlhubupdater.extra.UPDATE_FILE_PATH";
    public static final String EXTRA_RESULT_RECEIVER        = "com.revrobotics.controlhubupdater.extra.RESULT_RECEIVER";

    // Result broadcast values (broadcasts are sent when the system does an action on boot, or when the ResultReceiver instance has been invalidated because the app restarted)
    public static final String RESULT_BROADCAST = "com.revrobotics.controlhubupdater.broadcast.RESULT_BROADCAST"; // Unused, because the broadcast receiver is registered in the manifest
    public static final String RESULT_BROADCAST_BUNDLE_EXTRA = "com.revrobotics.controlhubupdater.broadcast.extra.BUNDLE"; // Contains the exact same bundle as would have been passed to a ResultReceiver instance

    public static final String RESULT_BUNDLE_CATEGORY_KEY = "category";                     // Will contain a String representation of Result.Category
    public static final String RESULT_BUNDLE_PRESENTATION_TYPE_KEY = "presentationType";    // Will contain a String representation of Result.PresentationType
    public static final String RESULT_BUNDLE_DETAIL_MESSAGE_TYPE_KEY = "detailMessageType"; // Will contain a String representation of Result.DetailMessageType
    public static final String RESULT_BUNDLE_CODE_KEY = "resultCode";                       // Will contain an int.
    public static final String RESULT_BUNDLE_MESSAGE_KEY = "message";                       // Will contain a String, which can be ignored if you know what the result code means.
    public static final String RESULT_BUNDLE_CAUSE_KEY = "cause";                           // Will contain either null or a Throwable. Should be logged if not null.
    public static final String RESULT_BUNDLE_DETAIL_MESSAGE_KEY = "detailMessage";          // Will contain either null or a String. Should be logged if not null.

    // Extras that we can send with intents as parameters
    public static final String EXTRA_DANGEROUS_ACTION_CONFIRMED = "com.revrobotics.controlhubupdater.extra.DANGEROUS_ACTION_CONFIRMED"; // Optional, populate with boolean
}
