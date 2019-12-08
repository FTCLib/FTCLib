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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.util.ReadWriteFile;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.firstinspires.ftc.onbotjava.OnBotJavaFileSystemUtils.PATH_SEPARATOR;
import static org.firstinspires.ftc.onbotjava.OnBotJavaFileSystemUtils.readFile;

public class JavaSourceFile {
    private final File sourceFile;

    private JavaSourceFile(@NonNull File sourceFile) {
        this.sourceFile = sourceFile;
    }

    @NonNull
    public static JavaSourceFile forFile(@NonNull File sourceFile) {
        return new JavaSourceFile(sourceFile);
    }

    public void copyTo(@NonNull File dest) throws IOException {
        copyTo(dest, null);
    }

    /**
     * Copy a Java source file, correcting the package and class name
     */
    private void copyTo(@NonNull File dest, @Nullable String sourcePackageName) throws IOException {
        final String contents = readFile(OnBotJavaFileSystemUtils.LineEndings.UNIX.lineEnding, sourceFile).toString();

        final String oldPackageName;
        if (sourcePackageName == null) {
            oldPackageName = packageName();
        } else {
            oldPackageName = sourcePackageName;
        }

        String oldClassName = className();

        writeJavaFileFromContents(oldClassName, oldPackageName, contents, dest);
    }

    public void writeJavaFileFromContents(@NonNull String oldClassName, @Nullable String oldPackageName, @NonNull String contents, @NonNull File dest) {
        JavaSourceFile destSource = JavaSourceFile.forFile(dest);
        String newPackageName = destSource.packageName();
        String newClassName = destSource.className();
        String newContents = contents;
        if (oldPackageName != null) {
            newContents = contents.replaceAll("(\\s*)package(\\s+)" + oldPackageName + ";(\\s)",
                    "$1package$2" + newPackageName + ";$3");
        }

        newContents = newContents.replaceAll(
                "(\\s+)(class|interface|@interface)(\\s+)" + oldClassName + "(\\s+)",
                "$1$2$3" + newClassName + "$4");
        ReadWriteFile.writeFile(dest, newContents);
    }

    private String packageName() {
        String packageName = sourceFile.getAbsolutePath();
        packageName = packageName.substring(0, packageName.lastIndexOf(PATH_SEPARATOR));
        if (packageName.indexOf(OnBotJavaManager.srcDir.getAbsolutePath()) == 0) {
            packageName = packageName.substring(OnBotJavaManager.srcDir.getAbsolutePath().length() + 1);
        }
        packageName = packageName.replaceAll(PATH_SEPARATOR, ".");
        return packageName;
    }

    public String className() {
        String oldClassName = sourceFile.getName();
        if (oldClassName.endsWith(OnBotJavaFileSystemUtils.EXT_JAVA_FILE)) {
            oldClassName = oldClassName.substring(0, oldClassName.length() - OnBotJavaFileSystemUtils.EXT_JAVA_FILE.length());
        } else if (oldClassName.contains(".")) {
            oldClassName = oldClassName.substring(0, oldClassName.lastIndexOf("."));
        }

        return oldClassName;
    }

    @Nullable
    public String packageNameFromContents() {
        final String code;
        try {
            code = readFile(OnBotJavaFileSystemUtils.LineEndings.UNIX, sourceFile).toString();
            return extractPackageNameFromContents(code);
        } catch (IOException e) {
            throw new RuntimeException("could not extract package name from contents of file", e);
        }
    }

    @Nullable
    public static String extractPackageNameFromContents(@NonNull String code) {
        final Pattern packagePattern = Pattern.compile("(?:\\n|)package\\s+(.+);(?:\\n|\\s*)");
        final Matcher matcher = packagePattern.matcher(code);
        if (matcher.find()) { // has package statement
            return matcher.group(1);
        }

        return null;
    }
}
