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

/**
 * ResultType built at runtime from the information sent by the CH Updater.
 *
 * Allows forward-compatibility with newer versions of the CH Updater, which may add new ResultTypes
 * that we don't know about.
 */
class UnknownResultType implements ResultType {
    private final Result.Category category;
    private final int code;
    private final Result.PresentationType presentationType;
    private final Result.DetailMessageType detailMessageType;
    private final String message;

    public UnknownResultType(Result.Category category, int code, Result.PresentationType presentationType, Result.DetailMessageType detailMessageType, String message) {
        this.category = category;
        this.code = code;
        this.presentationType = presentationType;
        this.detailMessageType = detailMessageType;
        this.message = message;
    }

    @Override public Result.Category getCategory() {
        return category;
    }

    @Override public int getCode() {
        return code;
    }

    @Override public Result.PresentationType getPresentationType() {
        return presentationType;
    }

    @Override public Result.DetailMessageType getDetailMessageType() {
        return detailMessageType;
    }

    @Override public String getMessage() {
        return message;
    }
}
