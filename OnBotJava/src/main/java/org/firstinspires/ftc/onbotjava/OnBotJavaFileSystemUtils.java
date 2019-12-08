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

import android.content.res.AssetManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Predicate;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import fi.iki.elonen.NanoHTTPD;
import org.firstinspires.ftc.robotserver.internal.webserver.MimeTypesUtil;

import static org.firstinspires.ftc.onbotjava.OnBotJavaSecurityManager.isValidSourceFileOrFolder;
import static org.firstinspires.ftc.onbotjava.StandardResponses.badRequest;
import static org.firstinspires.ftc.onbotjava.StandardResponses.serverError;
import static fi.iki.elonen.NanoHTTPD.newChunkedResponse;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public final class OnBotJavaFileSystemUtils {
    private static final String TAG = OnBotJavaFileSystemUtils.class.getName();

    public static final File templatesDir = new File(OnBotJavaManager.javaRoot, "templates");
    public static final String PATH_SEPARATOR = "/";
    public static final String EXT_TEMP_FILE = ".tmp";
    public static final String EXT_JAVA_FILE = ".java";
    public static final String EXT_ZIP_FILE = ".zip";

    private OnBotJavaFileSystemUtils() {

    }

    public enum LineEndings {
        WINDOWS("\r\n"), UNIX("\n");
        public final String lineEnding;

        LineEndings(String ending) {
            lineEnding = ending;
        }
    }

    public static NanoHTTPD.Response getFile(Map<String, List<String>> data) {
        return getFile(data, false, null);
    }

    public static NanoHTTPD.Response getFile(Map<String, List<String>> data, boolean folderAsZip, String lineEndings) {
        if (!data.containsKey(RequestConditions.REQUEST_KEY_FILE)) return StandardResponses.badRequest();
        String trimmedUri = data.get(RequestConditions.REQUEST_KEY_FILE).get(0);
        final String filePath = OnBotJavaManager.javaRoot.getAbsolutePath() + trimmedUri;
        if (OnBotJavaSecurityManager.isValidSourceFileLocation(trimmedUri)) { // is a file
            return getFileAsFile(lineEndings, filePath);
        } else if (OnBotJavaSecurityManager.isValidSourceFileOrFolder(trimmedUri, false)) { // is a folder
            if (folderAsZip) {
                return getFolderAsZip(filePath);
            } else {
                return getFolderAsTree(filePath);
            }
        }
        return StandardResponses.unauthorizedAccess();
    }

    @NonNull
    private static NanoHTTPD.Response getFolderAsTree(String filePath) {
        StringBuilder builder = new StringBuilder("Contents:\n");
        for (String file : new File(filePath).list()) {
            builder.append(file).append(LineEndings.UNIX.lineEnding);
        }

        return StandardResponses.successfulJsonRequest(builder.toString());
    }

    @NonNull
    private static NanoHTTPD.Response getFolderAsZip(String filePath) {
        final File sourceFolder = new File(filePath);
        final File tempFolder;
        try {
            tempFolder = File.createTempFile("onbotjava", EXT_TEMP_FILE, AppUtil.getDefContext().getCacheDir());
        } catch (IOException e) {
            RobotLog.ee(TAG, e, "Cannot create temp file for zip");
            return StandardResponses.serverError();
        }
        tempFolder.delete();
        tempFolder.mkdirs();
        final File outputZipFile = new File(tempFolder, filePath.substring(filePath.lastIndexOf(PATH_SEPARATOR) + 1) +  EXT_ZIP_FILE);
        try (FileOutputStream destOutput = new FileOutputStream(outputZipFile)) {
            try (final ZipOutputStream zipOutputStream = new ZipOutputStream(destOutput)) {
                zipOutputStream.setMethod(ZipOutputStream.DEFLATED);
                forEachInFolder(sourceFolder, true, new Predicate<File>() {
                    @Override
                    public boolean test(File file) {
                        try {
                            String entryName = file.getAbsolutePath();
                            if (entryName.endsWith(EXT_TEMP_FILE)) return true;

                            entryName = entryName.substring(sourceFolder.getAbsolutePath().length());
                            if (entryName.startsWith(PATH_SEPARATOR)) entryName = entryName.substring(1);
                            if (file.isDirectory() && !entryName.endsWith(PATH_SEPARATOR)) entryName += PATH_SEPARATOR;

                            ZipEntry entry = new ZipEntry(entryName);
                            zipOutputStream.putNextEntry(entry);
                            if (!file.isDirectory()) {
                                try (FileInputStream inputStream =  new FileInputStream(file)) {
                                    AppUtil.getInstance().copyStream(inputStream, zipOutputStream);
                                }
                            }
                            zipOutputStream.closeEntry();
                        } catch (IOException ex) {
                            RobotLog.ww(TAG, ex, "Cannot save file \"%s\" in zip", file.getAbsolutePath());
                        }

                        return true; // this result is ignored
                    }
                });
            }
        } catch (IOException ex) {
            RobotLog.ee(TAG, ex, "Cannot create zip file");
            return StandardResponses.serverError();
        }

        // These files should be deleted when the controller's cache is cleared or on exit of the Java VM
        // todo: find a way of clearing these files that doesn't harm NanoHTTPD
        outputZipFile.deleteOnExit();
        tempFolder.deleteOnExit();
        try {
            return newChunkedResponse(NanoHTTPD.Response.Status.OK, MimeTypesUtil.getMimeType( EXT_ZIP_FILE), new FileInputStream(outputZipFile));
        } catch (FileNotFoundException e) {
            return StandardResponses.serverError();
        }
    }

    @NonNull
    private static NanoHTTPD.Response getFileAsFile(String lineEndings, String filePath) {
        try {
            if (lineEndings == null) {
                return serveFile(filePath);
            } else {
                return serveFile(filePath, lineEndings);
            }
        } catch (FileNotFoundException e) {
            return StandardResponses.fileNotFound();
        }
    }

    private static void forEachInFolder(@NonNull File folder, boolean recursive, Predicate<File> action) throws FileNotFoundException {
        if (!folder.isDirectory()) throw new FileNotFoundException("not a directory");
        for (File file : folder.listFiles()) {
            if (recursive && file.isDirectory()) forEachInFolder(file, true, action);
            action.test(file);
        }
    }


    private static boolean copyAsset(@NonNull String assetPath, @NonNull File dest, boolean mirror) throws IOException {
        if (assetPath.isEmpty()) throw new IllegalArgumentException("assetPath cannot be empty");
        boolean templatesEnsured = true;
        final AssetManager assetManager = AppUtil.getInstance().getRootActivity().getAssets();
        assetPath = assetPath.endsWith(PATH_SEPARATOR) ? assetPath.substring(0, assetPath.length() - 1) : assetPath;

        String name = assetPath.substring(assetPath.lastIndexOf(PATH_SEPARATOR) + 1);
        final File newDest = new File(dest, name);
        if (mirror && newDest.exists()) {
            AppUtil.getInstance().delete(newDest);
            if (newDest.exists())
                throw new IOException(String.format(Locale.ENGLISH, "Failed to remove %s to in order to create a clean copy", newDest.getAbsolutePath()));
        }

        try {
            final List<String> children = Arrays.asList(assetManager.list(assetPath));

            if (children.isEmpty()) { // asset is a file
                try (InputStream stream = assetManager.open(assetPath)) {
                    try {
                        AppUtil.getInstance().copyStream(stream, newDest);
                    } catch (FileNotFoundException ex) {
                        RobotLog.ww(TAG, ex, "Could not open file %s", newDest.getAbsolutePath());
                        templatesEnsured = false;
                    }
                }
            } else { // asset is a folder
                newDest.mkdirs();

                for (String child : children) {
                    if (!copyAsset(assetPath + PATH_SEPARATOR + child, newDest, mirror)) {
                        templatesEnsured = false;
                    }
                }
            }

        } catch (IOException e) {
            RobotLog.ee(TAG, e, "Cannot copy asset, template data might be invalid");
            templatesEnsured = false;
        }

        return templatesEnsured;
    }

    public static boolean ensureTemplates() {
        if (templatesDir.exists() && !templatesDir.isDirectory()) templatesDir.delete();
        final String javaTemplatesDirPath = "java/templates";

        try {
            // Allow the user to keep custom templates, although they are now responsible for maintenance of the templates folder
            return copyAsset(javaTemplatesDirPath, templatesDir.getParentFile(), !(new File(templatesDir, "user").exists()));
        } catch (IOException ex) {
            throw new RuntimeException("ensureTemplates", ex);
        }
    }

    public static void searchForFiles(@NonNull String startPath, @NonNull File startFile, @NonNull List<String> results, boolean includeFolders) {
        // fail fast
        if (!startFile.isDirectory())
            throw new IllegalArgumentException("startFile is not a directory");
        if (results == null) throw new NullPointerException("results is null");
        for (File srcFile : startFile.listFiles()) {
            String absolutePath = srcFile.getAbsolutePath();
            absolutePath = absolutePath.startsWith(startPath) ? absolutePath.substring(startPath.length()) : absolutePath;
            // The trailing slash is how clients can tell the result is a folder
            if (srcFile.isDirectory()) absolutePath += PATH_SEPARATOR;
            if (!srcFile.isDirectory() || includeFolders) results.add(absolutePath);
            if (srcFile.isDirectory()) searchForFiles(startPath, srcFile, results, includeFolders);
        }
    }

    @NonNull
    public static NanoHTTPD.Response serveFile(@NonNull String uri) throws FileNotFoundException {
        return serveFile(uri, null);
    }

    @NonNull
    private static NanoHTTPD.Response serveFile(@NonNull String uri, @Nullable String lineEnding) throws FileNotFoundException {
        File test = new File(uri);
        uri = test.getAbsolutePath();
        if (!uri.startsWith(AppUtil.FIRST_FOLDER.getAbsolutePath()) && !uri.contains(".."))
            return StandardResponses.unauthorizedAccess();
        File file = new File(uri);
        String mime = MimeTypesUtil.determineMimeType(uri);
        if (file.exists() && file.canRead()) {
            FileInputStream reader = new FileInputStream(file);
            if (lineEnding != null) {
                StringBuilder builder = readFile(lineEnding, reader);
                return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, mime, builder.toString());
            } else {
                return newChunkedResponse(NanoHTTPD.Response.Status.OK, mime, reader);
            }
        }

        return StandardResponses.fileNotFound();
    }

    @NonNull
    public static StringBuilder readFile(@NonNull LineEndings lineEnding, @NonNull File file) throws IOException {
        return readFile(lineEnding.lineEnding, file);
    }

    @NonNull
    public static StringBuilder readFile(@NonNull String lineEnding, @NonNull File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return readFile(lineEnding, inputStream);
        }
    }

    @NonNull
    private static StringBuilder readFile(@NonNull String lineEnding, @NonNull FileInputStream reader) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader1 = new BufferedReader(new InputStreamReader(reader))) {
            String line;
            while ((line = reader1.readLine()) != null) {
                builder.append(line).append(lineEnding);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return builder;
    }
}
