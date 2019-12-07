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
package org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.webserver.R;
import org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.Category;
import org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.DetailMessageType;
import org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.PresentationType;

import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.DetailMessageType.DISPLAYED;
import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.DetailMessageType.LOGGED;
import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.PresentationType.ERROR;
import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.PresentationType.PROMPT;
import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.PresentationType.STATUS;
import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.PresentationType.SUCCESS;

/**
 * ResultType values unique to OTA update transactions
 */
public enum OtaResultType implements ResultType {
    VERIFICATION_SUCCEEDED   (1, STATUS,  LOGGED,    AppUtil.getDefContext().getString(R.string.ota_result_type_verification_succeeded)),
    INVALID_FILE_LOCATION    (2, ERROR,   LOGGED,    AppUtil.getDefContext().getString(R.string.ota_result_type_invalid_file_location)),
    INVALID_UPDATE_FILE      (3, ERROR,   LOGGED,    AppUtil.getDefContext().getString(R.string.ota_result_type_invalid_update_file)),
    ERROR_READING_FILE       (4, ERROR,   LOGGED,    AppUtil.getDefContext().getString(R.string.ota_result_type_error_reading_file)),
    FAILED_TO_INSTALL        (5, ERROR,   DISPLAYED, AppUtil.getDefContext().getString(R.string.ota_result_type_failed_to_install)),
    OTA_UPDATE_FINISHED      (6, SUCCESS, LOGGED,    AppUtil.getDefContext().getString(R.string.ota_result_type_ota_update_finished)),
    DOWNGRADE_NOT_AUTHORIZED (7, PROMPT,  LOGGED,    AppUtil.getDefContext().getString(R.string.ota_result_type_downgrade_not_authorized)),
    DEVICE_NOT_SUPPORTED     (8, ERROR,   LOGGED,    AppUtil.getDefContext().getString(R.string.ota_result_type_device_not_supported));

    private final int code;
    private final PresentationType presentationType;
    private final DetailMessageType detailMessageType;
    private final String message;

    OtaResultType(int code, PresentationType presentationType, DetailMessageType detailMessageType, String message) {
        this.code = code;
        this.presentationType = presentationType;
        this.detailMessageType = detailMessageType;
        this.message = message;
    }

    @Override
    public Category getCategory() {
        return Category.OTA_UPDATE;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public DetailMessageType getDetailMessageType() {
        return detailMessageType;
    }

    @Override public PresentationType getPresentationType() {
        return presentationType;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public static OtaResultType fromCode(int code) {
        for (OtaResultType resultType : OtaResultType.values()) {
            if (code == resultType.getCode()) return resultType;
        }
        return null;
    }
}
