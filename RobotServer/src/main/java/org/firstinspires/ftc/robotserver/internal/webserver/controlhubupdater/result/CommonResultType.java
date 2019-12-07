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

import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.DetailMessageType.LOGGED;
import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.PresentationType.ERROR;

/**
 * ResultType values common to both APK and OTA update transactions
 */
public enum CommonResultType implements ResultType {
    NO_UPDATE_FILE                (1, ERROR, LOGGED, AppUtil.getDefContext().getString(R.string.common_result_type_no_update_file)),
    FILE_DOES_NOT_EXIST           (2, ERROR, LOGGED, AppUtil.getDefContext().getString(R.string.common_result_type_file_does_not_exist)),
    PERFORMING_STARTUP_OPERATIONS (3, ERROR, LOGGED, AppUtil.getDefContext().getString(R.string.common_result_type_performing_startup_operations)),
    BUSY_WITH_PREVIOUS_REQUEST    (4, ERROR, LOGGED, AppUtil.getDefContext().getString(R.string.common_result_type_busy_with_previous_request));

    private final int code;
    private final PresentationType presentationType;
    private final DetailMessageType detailMessageType;
    private final String message;

    CommonResultType(int code, PresentationType presentationType, DetailMessageType detailMessageType, String message) {
        this.code = code;
        this.presentationType = presentationType;
        this.detailMessageType = detailMessageType;
        this.message = message;
    }

    @Override
    public Category getCategory() {
        return Category.COMMON;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override public PresentationType getPresentationType() {
        return presentationType;
    }

    @Override
    public DetailMessageType getDetailMessageType() {
        return detailMessageType;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public static CommonResultType fromCode(int code) {
        for (CommonResultType resultType : CommonResultType.values()) {
            if (code == resultType.getCode()) return resultType;
        }
        return null;
    }
}
