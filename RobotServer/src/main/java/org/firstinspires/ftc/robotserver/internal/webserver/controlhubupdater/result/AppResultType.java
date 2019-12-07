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
import org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.PresentationType;
import org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.DetailMessageType;

import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.DetailMessageType.DISPLAYED;
import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.DetailMessageType.LOGGED;

import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.DetailMessageType.SUBSTITUTED;
import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.PresentationType.ERROR;
import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.PresentationType.PROMPT;
import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.PresentationType.STATUS;
import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.PresentationType.SUCCESS;

/**
 * ResultType values unique to app update transactions
 */
public enum AppResultType implements ResultType {
    ATTEMPTING_INSTALLATION       (1,  STATUS,  SUBSTITUTED, AppUtil.getDefContext().getString(R.string.app_result_type_attempting_installation)),
    INSTALLATION_SUCCEEDED        (2,  SUCCESS, LOGGED,      AppUtil.getDefContext().getString(R.string.app_result_type_installation_succeeded)),
    INVALID_APK_FILE              (3,  ERROR,   DISPLAYED,   AppUtil.getDefContext().getString(R.string.app_result_type_invalid_apk_file)),
    IO_EXCEPTION_DURING_LOADING   (4,  ERROR,   LOGGED,      AppUtil.getDefContext().getString(R.string.app_result_type_io_exception)),
    TIMEOUT_EXPIRED               (5,  ERROR,   LOGGED,      AppUtil.getDefContext().getString(R.string.app_result_type_timeout_expired)),
    GENERIC_INSTALL_FAILURE       (6,  ERROR,   LOGGED,      AppUtil.getDefContext().getString(R.string.app_result_type_generic_install_failure)),
    INSTALLATION_BLOCKED          (7,  ERROR,   LOGGED,      AppUtil.getDefContext().getString(R.string.app_result_type_installation_blocked)),
    INSTALLATION_ABORTED          (8,  ERROR,   LOGGED,      AppUtil.getDefContext().getString(R.string.app_result_type_installation_aborted)),
    STORAGE_FAILURE               (9,  ERROR,   DISPLAYED,   AppUtil.getDefContext().getString(R.string.app_result_type_storage_failure)),
    INCOMPATIBLE_WITH_DEVICE      (10, ERROR,   DISPLAYED,   AppUtil.getDefContext().getString(R.string.app_result_type_incompatible_with_device)),
    UNKNOWN_INSTALL_STATUS        (11, ERROR,   LOGGED,      AppUtil.getDefContext().getString(R.string.app_result_type_unknown_install_status)),
    ILLEGAL_PACKAGE               (12, ERROR,   DISPLAYED,   AppUtil.getDefContext().getString(R.string.app_result_type_illegal_package)),
    UNINSTALL_REQUIRED            (13, PROMPT,  SUBSTITUTED, AppUtil.getDefContext().getString(R.string.app_result_type_uninstall_required)),
    UNINSTALL_FAILED              (14, ERROR,   DISPLAYED,   AppUtil.getDefContext().getString(R.string.app_result_type_uninstall_failed)),
    FAILED_AND_REVERTED           (15, ERROR,   LOGGED,      AppUtil.getDefContext().getString(R.string.app_result_type_failed_and_reverted)), // UNUSED by the shipping version of ControlHubUpdater
    FAILED_TO_RESTORE             (16, ERROR,   LOGGED,      AppUtil.getDefContext().getString(R.string.app_result_type_failed_to_restore)),
    REBOOTING_WITH_NEW_AP_SERVICE (17, SUCCESS, LOGGED,      AppUtil.getDefContext().getString(R.string.app_result_type_rebooting_with_new_ap_service)),
    AUTO_UPDATED_APP              (18, SUCCESS, SUBSTITUTED, AppUtil.getDefContext().getString(R.string.app_result_type_auto_updated_app)), // Indicates that the AP service was updated by an OS update
    REPLACED_WRONG_APP_VARIANT    (19, ERROR,   SUBSTITUTED, AppUtil.getDefContext().getString(R.string.app_result_type_replaced_wrong_app_variant)), // UNUSED by the shipping version of ControlHubUpdater
    UNINSTALLED_WRONG_APP_VARIANT (20, ERROR,   SUBSTITUTED, AppUtil.getDefContext().getString(R.string.app_result_type_uninstalled_wrong_app_variant)), // UNUSED by the shipping version of ControlHubUpdater
    INSTALLED_MISSING_APP         (21, ERROR,   SUBSTITUTED, AppUtil.getDefContext().getString(R.string.app_result_type_installed_missing_app)),
    UNINSTALLING_EXISTING_APP     (22, STATUS,  LOGGED,      AppUtil.getDefContext().getString(R.string.app_result_type_uninstalling_existing_app));

    private final int code;
    private final Result.PresentationType presentationType;
    private final DetailMessageType detailMessageType;
    private final String message;

    AppResultType(int code, PresentationType presentationType, DetailMessageType detailMessageType, String message) {
        this.code = code;
        this.presentationType = presentationType;
        this.detailMessageType = detailMessageType;
        this.message = message;
    }

    @Override
    public Category getCategory() {
        return Category.APP_UPDATE;
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

    public static AppResultType fromCode(int code) {
        for (AppResultType resultType : AppResultType.values()) {
            if (code == resultType.getCode()) return resultType;
        }
        return null;
    }
}
