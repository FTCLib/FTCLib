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

package org.firstinspires.ftc.onbotjava;

public final class OnBotJavaSecurityManager {
    private final static String VALID_TEMPLATE_FILE_REGEX =
            "templates/([\\w.\\d/_]+/)*([\\w.\\d_]+\\.(?:java|txt|md|properties|groovy|/)|[\\w\\d_]+)$";
    private final static String VALID_SRC_FILE_REGEX =
            "/(?:src|jars)[\\w.\\d/_$]+[\\w.\\d_$]+\\.(?:java|jar|zip|txt|md|properties|groovy|json)$";
    private final static String VALID_SRC_FILE_OR_FOLDER_REGEX =
            "(/(src|jars)([\\w.\\d/_$]+[\\w.\\d_$]+(\\.(java|jar|zip|txt|md|properties|groovy|json)|/)|/))$";

    // this class shouldn't be constructed or overriden
    private OnBotJavaSecurityManager() {

    }

    public static boolean isValidSourceFileLocation(String filename) {
        return isValidFileLocation(filename, VALID_SRC_FILE_REGEX);
    }

    public static boolean isValidSourceFileOrFolder(String filename) {
        return isValidSourceFileOrFolder(filename, false);
    }

    public static boolean isValidSourceFileOrFolder(String filename, boolean allowSourceRootDir) {
        return isValidFileLocation(filename, VALID_SRC_FILE_OR_FOLDER_REGEX) || (allowSourceRootDir && filename.equals("/src/"));
    }

    public static boolean isValidTemplateFile(String filename) {
        return isValidFileLocation(filename, VALID_TEMPLATE_FILE_REGEX);
    }

    /**
     * Verifies a file is web-accessible
     *
     * @param fileName          the file name being accessed
     * @param regularExpression a regular expression specifying the file name, extensions, and possible
     *                          locations
     * @return {@code true} if the file is web-accessible, otherwise {@code false}
     */
    private static boolean isValidFileLocation(final String fileName, final String regularExpression) {
        return fileName.matches(regularExpression) && !fileName.matches("\\.{2}");
    }
}
