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

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.UpdaterConstants;

import static org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result.DetailMessageType.SUBSTITUTED;

/**
 * A result sent to us by the Control Hub Updater
 *
 * May be a status update, a prompt, an error, or a success message.
 */
public final class Result {
    @NonNull private final ResultType resultType;
    @Nullable private String detailMessage;
    @Nullable private Throwable cause;

    private Result(@NonNull ResultType resultType, @Nullable String detailMessage, @Nullable Throwable cause) {
        this.resultType = resultType;
        this.detailMessage = detailMessage;
        this.cause = cause;
    }

    public Category getCategory() {
        return resultType.getCategory();
    }

    public int getCode() {
        return resultType.getCode();
    }

    public String getMessage() {
        String message = resultType.getMessage();
        if (getDetailMessageType() == SUBSTITUTED) {
            String localDetailMessage = detailMessage; // Save a copy of the detail message so we don't overwrite the real one. BTW, it's important not to use getDetailMessage here.
            if (localDetailMessage == null) {
                localDetailMessage = "";
            }
            message = String.format(message, localDetailMessage);
        }
        return message;
    }

    @Nullable
    public String getDetailMessage() {
        if (getDetailMessageType() == SUBSTITUTED) {
            return null; // The detail message will be returned as a part of the main message. We should pretend it doesn't exist outside of that.
        }
        return detailMessage;
    }

    public DetailMessageType getDetailMessageType() {
        return resultType.getDetailMessageType();
    }

    /**
     * Get the cause of the error (may be null)
     */
    @Nullable
    public Throwable getCause() {
        return cause;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public PresentationType getPresentationType() {
        return resultType.getPresentationType();
    }

    /**
     * Parse a Bundle into a Result
     */
    public static Result fromBundle(Bundle bundle) {
        ResultType resultType = null;

        // We look up the ResultType using the category and code values
        Category category = Category.fromString(bundle.getString(UpdaterConstants.RESULT_BUNDLE_CATEGORY_KEY));
        int code = bundle.getInt(UpdaterConstants.RESULT_BUNDLE_CODE_KEY);

        if (category != null) { // paranoia
            switch (category) {
                case COMMON:
                    resultType = CommonResultType.fromCode(code);
                    break;
                case OTA_UPDATE:
                    resultType = OtaResultType.fromCode(code);
                    break;
                case APP_UPDATE:
                    resultType = AppResultType.fromCode(code);
                    break;
            }
        }

        if (resultType == null) {
            // We didn't recognize the category/code combination, so we'll create a new UnknownResultType
            // instead, which will use the message, PresentationType, and DetailMessageType provided in the bundle.
            String message = bundle.getString(UpdaterConstants.RESULT_BUNDLE_MESSAGE_KEY);
            PresentationType presentationType = PresentationType.fromString(bundle.getString(UpdaterConstants.RESULT_BUNDLE_PRESENTATION_TYPE_KEY));
            DetailMessageType detailMessageType = DetailMessageType.fromString(bundle.getString(UpdaterConstants.RESULT_BUNDLE_DETAIL_MESSAGE_TYPE_KEY));

            resultType = new UnknownResultType(category, code, presentationType, detailMessageType, message);
        }

        // Return a new Result object
        String detailMessage = bundle.getString(UpdaterConstants.RESULT_BUNDLE_DETAIL_MESSAGE_KEY);
        Throwable cause = (Throwable) bundle.getSerializable(UpdaterConstants.RESULT_BUNDLE_CAUSE_KEY);
        return new Result(resultType, detailMessage, cause);
    }

    /**
     * Enum that specifies which ResultType category this result is
     */
    public enum Category {
        COMMON, OTA_UPDATE, APP_UPDATE;

        public static Category fromString(String string) {
            for (Category category : Category.values()) {
                if (category.name().equals(string)) return category;
            }
            return null;
        }
    }

    /**
     * Enum that specifies how this result is to be displayed to the user
     */
    public enum PresentationType {
        // Note: Status messages are intended to be non-dismissable
        SUCCESS, ERROR, STATUS, PROMPT;

        public static PresentationType fromString(String string) {
            for (PresentationType presentationType : PresentationType.values()) {
                if (presentationType.name().equals(string)) return presentationType;
            }
            return null;
        }
    }

    /**
     * Enum that specifies what should be done with the result's detail message
     */
    public enum DetailMessageType {
        LOGGED, // The detail message should merely be logged, if it is present
        DISPLAYED, // The detail message should be displayed, if it is present
        SUBSTITUTED; // The detail message should be provided by the CH updater, and will be injected into the main message. If no detail message is provided, a blank string will be injected.

        public static DetailMessageType fromString(String string) {
            for (DetailMessageType detailMessageType : DetailMessageType.values()) {
                if (detailMessageType.name().equals(string)) return detailMessageType;
            }
            return null;
        }
    }
}

