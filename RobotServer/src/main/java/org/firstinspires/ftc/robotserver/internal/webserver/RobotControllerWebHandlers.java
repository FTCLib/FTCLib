/*
 * Copyright (c) 2017 DEKA Research and Development
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of DEKA Research and Development nor the names of contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.robotserver.internal.webserver;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.util.Device;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import com.qualcomm.robotcore.util.WebHandlerManager;
import com.qualcomm.robotcore.util.WebServer;
import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;
import org.firstinspires.ftc.robotcore.internal.hardware.android.AndroidBoard;
import org.firstinspires.ftc.robotcore.internal.network.ApChannelManager;
import org.firstinspires.ftc.robotcore.internal.network.ApChannelManagerFactory;
import org.firstinspires.ftc.robotcore.internal.network.DeviceNameListener;
import org.firstinspires.ftc.robotcore.internal.network.DeviceNameManager;
import org.firstinspires.ftc.robotcore.internal.network.DeviceNameManagerFactory;
import org.firstinspires.ftc.robotcore.internal.network.PasswordManager;
import org.firstinspires.ftc.robotcore.internal.network.PasswordManagerFactory;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;
import org.firstinspires.ftc.robotcore.internal.webserver.RobotControllerWebInfo;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;
import org.firstinspires.ftc.robotcore.internal.webserver.WebObserver;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketManager;
import org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.ChUpdaterCommManager;
import org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.ChUpdaterUploadResponse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD;

import static fi.iki.elonen.NanoHTTPD.IHTTPSession;
import static fi.iki.elonen.NanoHTTPD.Response;
import static fi.iki.elonen.NanoHTTPD.newChunkedResponse;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

/**
 * A collection of web handlers having to do with core robot controller management, etc
 */
@SuppressWarnings("WeakerAccess")
public class RobotControllerWebHandlers
{
    public static final String TAG = RobotControllerWebHandlers.class.getSimpleName();

    public static boolean DEBUG = false;

    public static final String URI_RC_CONFIG = "/js/rc_config.js";  // knowledge of this constant is in .js files too
    public static final String URI_NAV_HOME = "/connection.html";
    public static final String URI_NAV_MANAGE = "/manage.html";
    public static final String URI_NAV_HELP = "/help.html";
    public static final String URI_ANON_PING = "/anonymousPing";
    public static final String URI_PING = "/ping";
    public static final String URI_LIST_LOG_FILES = "/listLogs";
    public static final String URI_DOWNLOAD_FILE = "/downloadFile";
    public static final String URI_RENAME_RC = "/renameRC";
    public static final String URI_CHANGE_AP_PASSWORD = "/changeApPassword";
    public static final String URI_CHANGE_AP_CHANNEL = "/changeApChannel";
    public static final String URI_UPLOAD_EXPANSION_HUB_FIRMWARE = "/uploadExpansionHubFirmware";
    public static final String URI_UPDATE_CONTROL_HUB_APK = "/updateControlHubAPK";
    public static final String URI_UPLOAD_WEBCAM_CALIBRATION_FILE = "/uploadWebcamCalibrationFile";
    public static final String URI_UPLOAD_CONTROL_HUB_OTA = "/uploadControlHubOta";
    public static final String URI_REBOOT = "/reboot";
    public static final String URI_RC_INFO = "/js/rcInfo.json";
    public static final String URI_COLORS = "/css/colors.less";

    public static final String INDEX_FILE = "frame.html";

    /** These are intercepted in ProgramAndManageActivity; they make sense only
     * the Android WebView client */
    public static final String URI_EXIT_PROGRAM_AND_MANAGE = "/exitProgramAndManage";
    public static final String URI_TOAST = "/toast";

    public static final String PARAM_NAME = "name";
    public static final String PARAM_AP_PASSWORD = "password";
    public static final String PARAM_NEW_NAME = "new_name";
    public static final String PARAM_MESSAGE = "message";

    public static void initialize(WebHandlerManager manager)
    {
        WebSocketManager webSocketManager = manager.getWebServer().getWebSocketManager();

        @NonNull WebHandler apkUpdateHandler;
        @Nullable ChUpdaterCommManager chUpdaterCommManager = null;

        // APK updating on Control Hub V1 is done differently from the Dragonboard Control Hub
        if (AndroidBoard.getInstance().hasControlHubUpdater()) {
            // Set up a ChUpdaterCommManager (used by the Manage page)
            chUpdaterCommManager = new ChUpdaterCommManager(webSocketManager);
            webSocketManager.registerNamespaceHandler(chUpdaterCommManager);
            apkUpdateHandler = new ControlHubApkUpdate(chUpdaterCommManager);
        } else {
            apkUpdateHandler = new DragonboardAPKUpdate();
            webSocketManager.registerNamespaceAsBroadcastOnly(ChUpdaterCommManager.WS_NAMESPACE); // The Manage page is going to subscribe either way, so we need to give it something to subscribe to.
        }

        manager.register("/",                       new ServerRootIndex(INDEX_FILE));
        manager.register(URI_ANON_PING,             new AnonymousPing());
        manager.register(URI_PING,                  decorateWithParms(new ClientPing())); // overridden in ProgrammingWebHandlers
        manager.register(URI_LIST_LOG_FILES,        new ListLogFiles());
        manager.register(URI_DOWNLOAD_FILE,         new FileDownload());
        manager.register(URI_RENAME_RC,             decorateWithParms(new RenameRobotController()));
        manager.register(URI_CHANGE_AP_PASSWORD,    decorateWithParms(new ChangeApPassword()));
        manager.register(URI_CHANGE_AP_CHANNEL,     decorateWithParms(new ChangeApChannel()));
        manager.register(URI_UPDATE_CONTROL_HUB_APK, apkUpdateHandler);
        manager.register(URI_UPLOAD_EXPANSION_HUB_FIRMWARE, new StandardUpload(AppUtil.LYNX_FIRMWARE_UPDATE_DIR.getAbsolutePath()));
        manager.register(URI_UPLOAD_WEBCAM_CALIBRATION_FILE, new StandardUpload(AppUtil.WEBCAM_CALIBRATIONS_DIR.getAbsolutePath()));
        manager.register(URI_UPLOAD_CONTROL_HUB_OTA, new OtaUpdate(chUpdaterCommManager));
        manager.register(URI_RC_CONFIG,             new RobotControllerConfiguration());
        manager.register(URI_RC_INFO,               new RobotControllerInfoHandler(manager.getWebServer()));
        manager.register(URI_REBOOT,                new Reboot());
        manager.register(URI_TOAST,                 new SimpleSuccess());
        manager.register(URI_EXIT_PROGRAM_AND_MANAGE, new SimpleSuccess()); // actually *fully* handled in ProgramAndManageActivity, but registering make things neat and tidy

        AppThemeColorsHandler colorsHandler = new AppThemeColorsHandler();
        manager.register(URI_COLORS, colorsHandler);
        manager.registerObserver(URI_COLORS, colorsHandler);
    }

    public static WebHandler decorateWithParms(WebHandler delegate)
    {
        return new SessionParametersGenerator(delegate);
    }

    /** Handles error handling for those using PARAM_NAME */
    public static abstract class RequireNameHandler implements WebHandler
    {
        @Override
        public final Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException
        {
            final Map<String, List<String>> parms = session.getParameters();
            String name = null;
            if (parms.containsKey(PARAM_NAME)) {
                name = parms.get(PARAM_NAME).get(0);
            }
            if (name == null) {
                return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                        "Bad Request: " + PARAM_NAME + " parameter is required");
            }
            else if (name.length() == 0) {
                return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                        "Bad Request: " + PARAM_NAME + " must be non-empty");
            } else {
                return getResponse(session, name);
            }
        }

        protected abstract Response getResponse(NanoHTTPD.IHTTPSession session, @NonNull String name) throws IOException, NanoHTTPD.ResponseException;
    }

    private static class ServerRootIndex implements WebHandler {
        private final String index;
        private final AssetManager assets;

        public ServerRootIndex(String indexFile) {
            index = indexFile;
            assets = AppUtil.getInstance().getApplication().getAssets();
        }

        @Override
        public Response getResponse(IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
            try {
                final InputStream file = assets.open(index);
                final String mimeType = MimeTypesUtil.determineMimeType(index);
                return newChunkedResponse(Response.Status.OK, mimeType, file);
            } catch (Exception ex) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                        NanoHTTPD.MIME_PLAINTEXT, "Internal Error");
            }
        }
    }

    public static class Redirection implements WebHandler
    {
        public enum QueryParameters { PRESERVE, DISCARD };
        private final String targetURI;
        private final QueryParameters queryParameters;

        public Redirection(String targetURI)
        {
            this(targetURI, QueryParameters.PRESERVE);
        }
        public Redirection(String targetURI, QueryParameters queryParameters)
        {
            this.targetURI = targetURI;
            this.queryParameters = queryParameters;
        }

        @Override
        public Response getResponse(IHTTPSession session)
        {
            String location = targetURI;

            String query = session.getQueryParameterString() != null && session.getQueryParameterString().length() > 0 ? session.getQueryParameterString() : null;
            if (queryParameters == QueryParameters.PRESERVE) {
                if (query != null) {
                    location += "?" + query;
                }
            }

			if (DEBUG) {
                String uri = session.getUri();
                if (query != null) {
                    uri += "?" + query;
                }
				RobotLog.dd(TAG, "In Redirect from='%s' to='%s'", uri, location);
			}

            // Temporary provides less browser lockup, and so better RC App versioning behavior,
            // but permanent might work better in brower UIs (doesn't track the redirect in address bar?).
			//
			// Or... maybe not...unclear. We don't see a compelling reason to perm-redirect, so we don't.
			//
            Response.IStatus status =  Response.Status.TEMPORARY_REDIRECT;

            final Response response = newFixedLengthResponse(status, NanoHTTPD.MIME_PLAINTEXT, "");
            response.addHeader("Location", location);
            return response;
        }
    }

    /**
     * {@link RobotControllerInfoHandler}
     */
    public static class RobotControllerInfoHandler implements WebHandler
    {
        private final WebServer webServer;

        public RobotControllerInfoHandler(@NonNull WebServer webServer)
        {
            this.webServer = webServer;
        }

        @Override
        public Response getResponse(IHTTPSession session) throws IOException, NanoHTTPD.ResponseException
        {
			if (DEBUG) {
				RobotLog.dd(TAG, "RCInfoHandler");
			}

            final RobotControllerWebInfo info = webServer.getConnectionInformation();
            info.setFtcUserAgentCategory(session.getHeaders());
            final String jsonResponse = info.toJson();
            return NoCachingWebHandler.setNoCache(session, newFixedLengthResponse(Response.Status.OK, MimeTypesUtil.getMimeType("json"), jsonResponse));
        }
    }

    /**
     * Returns json containing the full paths to all the extant log files.
     * Paths are relative to root, not absolute
     */
    public static class ListLogFiles implements WebHandler
    {
        @Override
        public Response getResponse(IHTTPSession session) throws IOException, NanoHTTPD.ResponseException
        {
            synchronized (this) {
                List<String> result = new ArrayList<>();
                List<File> logFiles = RobotLog.getExtantLogFiles(AppUtil.getDefContext());
                for (File logFile : logFiles) {
                    File relative = AppUtil.getInstance().getRelativePath(FileDownload.fileRoot, logFile.getAbsoluteFile());
                    if (relative.isAbsolute()) {
                        RobotLog.ee(TAG, "internal error: %s not under %s", logFile, FileDownload.fileRoot);
                    } else {
                        result.add(relative.getPath());
                    }
                }
                String json = SimpleGson.getInstance().toJson(result);
                return newFixedLengthResponse(Response.Status.OK, MimeTypesUtil.getMimeType("json"), json);
            }
        }
    }

    /**
     * Returns the content of an indicated file. We can return anything under the file root.
     */
    public static class FileDownload extends RequireNameHandler
    {
        public static File fileRoot = AppUtil.ROOT_FOLDER;

        @Override
        public Response getResponse(NanoHTTPD.IHTTPSession session, @NonNull String name) throws IOException
        {
			if (DEBUG) {
				RobotLog.dd(TAG, "FileDownload name=%s", name);
			}

            File absoluteFile = new File(fileRoot, name);
            return fetchFileContent(absoluteFile);
        }

        private Response fetchFileContent(File file) throws IOException
        {
            InputStream inputStream;
            try {
                // One might be tempted to truncate files here based on length. But that would
                // only be a reasonable thing to do for a limited number of file formats.
                inputStream = new BufferedInputStream(new FileInputStream(file));
            }
            catch (IOException e) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "");
            }
            Response response = newChunkedResponse(Response.Status.OK, MimeTypesUtil.determineMimeType(file.getName()), inputStream);
            response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            return response;
        }
    }


    /**
     * Upload a file to the Android device
     */
    public static abstract class FileUpload implements WebHandler
    {
        private static final String FILE_FORM_ID = "file";

        /**
         * Abstract method that must be implemented to return a response. The intention is to
         * allow post processing to be defined by child classes.
         *
         * @param uploadedFile File instance that points to the file that was just uploaded
         * @return an "OK" Response if the transfer was a success and an "ERROR" Response otherwise.
         */
        public abstract Response hook(File uploadedFile);

        /**
         * Abstract method that must be implemented to specify the location that the file should be
         * saved to.
         *
         * @param fileName the name that the file will be given when it is saved
         * @param tempFile the temporary file that will be moved based on the return value of this method
         * @return the folder location that the file should be saved to (don't include the filename)
         */
        public abstract File provideDestinationDirectory(String fileName, File tempFile);



        /**
         * Write an update file to the Updates directory.
         *
         * @param session one IHTTPSession that will return one Response
         * @return an "OK" Response if the transfer was a success and an "ERROR" Response otherwise.
         */
        @Override
        public final Response getResponse(IHTTPSession session)
        {
            synchronized (this) { // paranoia
                if (DEBUG) {
                    RobotLog.dd(TAG, "In FileUpload");
                }

                Map<String, String> fileList = new HashMap<>();
                try {
                    session.parseBody(fileList);
                    String fileName = getFileName(getUploadedFileName(session));
                    File tempFile = new File(fileList.get(FILE_FORM_ID));
                    File destDir = provideDestinationDirectory(fileName, tempFile);
                    File targetFile = new File(destDir, fileName);

                    if (!checkDir(destDir)) {
                        return RobotWebHandlerManager.internalErrorResponse(TAG, "Could not access upload location " + destDir.getAbsolutePath());
                    }

                    boolean success = tempFile.renameTo(targetFile);
                    if (!success) return RobotWebHandlerManager.internalErrorResponse(TAG, "Failed to move file to correct location");

                    return hook(targetFile);
                } catch (IOException | NanoHTTPD.ResponseException | FilenameNotProvidedException e) {
                    RobotLog.ee(TAG, e, "Failed to retrieve uploaded file");
                    return RobotWebHandlerManager.internalErrorResponse(TAG, "Upload failed, try again");
                }
            }
        }

        /**
         * Provides the filename that the file will be saved to. Can be overridden.
         *
         * @param uploadedFileName filename that the file had when the user uploaded it
         * @return String filename that the file will be saved as
         */
        protected String getFileName(String uploadedFileName)
        {
            return uploadedFileName;
        }

        /**
         * getUploadedFileName.
         * @param session the HTTP session where the file was uploaded
         * @return the filename that the file had upon upload
         */
        private String getUploadedFileName(IHTTPSession session) throws FilenameNotProvidedException {
            if (!session.getParameters().containsKey(FILE_FORM_ID)) throw new FilenameNotProvidedException();
            return session.getParameters().get(FILE_FORM_ID).get(0);
        }

        /**
         * checkDir.
         *
         * @param dir File instance referencing target directory
         * @return true if the directory exists, false if it could not be made.
         */
        private boolean checkDir(File dir)
        {
            if (!dir.exists())
            {
                return dir.mkdirs();
            }
            return true;
        }
    }

    /**
     * StandardUpload
     *
     * Class used to specify a Standard upload.
     */
    private static final class StandardUpload extends FileUpload
    {
        private final File destinationDir;

        StandardUpload(String destinationDir)
        {
            this.destinationDir = new File(destinationDir);
        }

        @Override
        public Response hook(File uploadedFile)
        {
            return RobotWebHandlerManager.OK_RESPONSE;
        }

        @Override public File provideDestinationDirectory(String filename, File tempFile) {
            return destinationDir;
        }
    }

    private static abstract class ControlHubUpdaterUpload extends FileUpload
    {
        private final ChUpdaterCommManager.UpdateType updateType;
        private final ChUpdaterCommManager chUpdaterCommManager;

        protected ControlHubUpdaterUpload(ChUpdaterCommManager.UpdateType updateType, @Nullable ChUpdaterCommManager chUpdaterCommManager)
        {
            this.updateType = updateType;
            this.chUpdaterCommManager = chUpdaterCommManager;
        }

        @Override
        public final Response hook(File uploadedFile)
        {
            if (!AndroidBoard.getInstance().hasControlHubUpdater() || chUpdaterCommManager == null) {
                if (AndroidBoard.getInstance().hasControlHubUpdater()) RobotLog.ww(TAG, "This device should include the Control Hub Updater, yet chUpdaterCommManager is null.");
                return RobotWebHandlerManager.internalErrorResponse(TAG, AppUtil.getDefContext().getString(R.string.ch_updater_not_supported));
            }

            UUID transactionId = chUpdaterCommManager.startUpdate(updateType, uploadedFile);
            return ChUpdaterUploadResponse.create(transactionId);
        }
    }

    /**
     * OtaUpdate
     *
     * Class used to upload an OS update zip file (Control Hub V1+ required)
     */
    private static class OtaUpdate extends ControlHubUpdaterUpload {

        protected OtaUpdate(@Nullable ChUpdaterCommManager chUpdaterCommManager)
        {
            super(ChUpdaterCommManager.UpdateType.OTA, chUpdaterCommManager);
        }

        @Override
        public File provideDestinationDirectory(String fileName, File tempFile) {
            return AppUtil.OTA_UPDATE_DIR;
        }
    }


    /**
     * ControlHubApkUpdate
     *
     * Class used to specify an APK upload for Control Hub V1+
     */
    private static class ControlHubApkUpdate extends ControlHubUpdaterUpload {

        protected ControlHubApkUpdate(ChUpdaterCommManager chUpdaterCommManager)
        {
            super(ChUpdaterCommManager.UpdateType.APP, chUpdaterCommManager);
        }

        @Override
        public File provideDestinationDirectory(String fileName, File tempFile)
        {
            return AppUtil.RC_APP_UPDATE_DIR;
        }
    }

    /**
     * DragonboardAPKUpdate
     *
     * Class used to specify an APK upload for Dragonboard Control Hub
     */
    private static final class DragonboardAPKUpdate extends FileUpload
    {
        private static final File DESTINATION_DIR = AppUtil.RC_APP_UPDATE_DIR;
        private static final String DRAGONBOARD_APK_METADATA_TAG = "org.firstinspires.main.entry";

        /**
         * The APK is invalid remove it and return a response.
         *
         * @param file File instance pointing to the APK file
         * @param extraError any extra information that should be returned in the response text
         *
         * @return an Internal Error with a description of what went wrong
         */
        private Response invalidApk(File file, String extraError)
        {
            RobotLog.ii(TAG, "Invalid APK. Removing from file system");
            final boolean isRemoved = file.delete();
            if (!isRemoved)
            {
                RobotLog.ii(TAG, "Invalid APK cannot be removed");
            }
            String errorString = "Invalid APK";
            if (extraError != null) errorString += (": " + extraError);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, errorString);
        }
        /**
         * Generate a description of the APK.
         *
         * @param filePath absolute path to APK
         * @param info APK Package information that is used to generate the description
         *
         * @return the empty string if the candidate has a proper meta tag and a full
         * description otherwise.
         */
        private String getDescription(String filePath, PackageInfo info)
        {
            final StringBuilder desc = new StringBuilder();
            desc.append(filePath).append(System.lineSeparator());
            desc.append(info.packageName).append(System.lineSeparator());
            boolean isValidToInstall = false;
            for (ActivityInfo activityInfo : info.activities)
            {
                if (activityInfo.metaData != null)
                {
                    // Check for the required metadata tag for this type of Control Hub
                    if (Device.isRevControlHub() && activityInfo.metaData.getBoolean(DRAGONBOARD_APK_METADATA_TAG, false))
                    {
                        desc.append(info.packageName).append("/").append(activityInfo.name).append(System.lineSeparator());
                        isValidToInstall = true;
                    }
                }
            }
            if (isValidToInstall)
            {
                return desc.toString();
            }
            return "";
        }
        /**
         * Attempt to write the description to a file.
         *
         * @param filename name of APK
         * @param desc generated description of APK
         *
         * @return An "OK" response if the write succeeded and "ERROR" otherwise.
         */
        private Response writeDescriptionFile(String filename, String desc)
        {
            final int lastDot = filename.lastIndexOf('.');
            final String descFileName = filename.substring(0, lastDot) + ".des";
            final File toWrite = new File(DESTINATION_DIR, descFileName);
            try (final Writer writer = new FileWriter(toWrite))
            {
                writer.write(desc);
                return newFixedLengthResponse(Response.Status.OK, MimeTypesUtil.MIME_JSON, "{}"); // We return an empty JSON object because the CHv1+ version of this handler returns a JSON object
            } catch (IOException e)
            {
                RobotLog.ii(TAG, e.getMessage());
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
            }
        }
        /**
         * Build a description of the APK that is being installed.
         * The format of this file follows where each new line implies there is a line separator:
         * ___________________________________________________
         * FilePath of APK
         * Package Name of APK
         * Activity Manager Path (Main entry point for APK)
         *
         * ___________________________________________________
         *
         * @param file File instance pointing to the APK file that is being installed
         */
        private Response writeDescription(final File file)
        {
            final PackageManager pm = AppUtil.getInstance().getActivity().getPackageManager();
            final PackageInfo info = pm.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
            if (info == null) // Turns out we do not have a valid APK at this point
            {
                return invalidApk(file, null);
            }
            else
            {
                final String desc = getDescription(file.getAbsolutePath(), info); // generate a description file
                final boolean isNotValid = desc.isEmpty();
                if (isNotValid) // We don't have the proper tag - give up
                {
                    return invalidApk(file, "This APK does not support the Control Hub.");
                }
                else
                {
                    return writeDescriptionFile(file.getName(), desc);
                }
            }
        }
        @Override
        public Response hook(File uploadedFile)
        {
            return writeDescription(uploadedFile);
        }

        @Override public File provideDestinationDirectory(String filename, File tempFile) {
            return DESTINATION_DIR;
        }
    }

    /** Renames the robot controller */
    public static class RenameRobotController extends RequireNameHandler
    {
        public static final String TAG = RenameRobotController.class.getSimpleName();

        @Override
        public Response getResponse(IHTTPSession session, final @NonNull String desiredDeviceName) throws IOException, NanoHTTPD.ResponseException
        {
            synchronized (this) {
                if (DEBUG) {
                    RobotLog.dd(TAG, "name=%s", desiredDeviceName);
                }

                final DeviceNameManager nameManager = DeviceNameManagerFactory.getInstance();
                if (!desiredDeviceName.equals(nameManager.getDeviceName())) {
                    // Change the name and wait synchronously (for a bit) for the change to take effect
                    // This helps the web ui reflect the change.
                    final Semaphore semaphore = new Semaphore(0);

                    final DeviceNameListener callback = new DeviceNameListener() {
                        @Override public void onDeviceNameChanged(String newDeviceName) {
                            if (newDeviceName.equals(desiredDeviceName)) {
                                RobotLog.vv(TAG, "name change to %s observed", desiredDeviceName);
                                semaphore.release(1);
                            }
                        }
                    };
                    nameManager.registerCallback(callback);
                    try {
                        nameManager.setDeviceName(desiredDeviceName);
                        semaphore.tryAcquire(250, TimeUnit.MILLISECONDS); // an eternity
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        nameManager.unregisterCallback(callback);
                    }
                } else {
                    RobotLog.vv(TAG, "name change to existing name %s; ignored", desiredDeviceName);
                }
                return RobotWebHandlerManager.OK_RESPONSE;
            }
        }
    }

    public static class ChangeApPassword extends RequireNameHandler
    {
        @Override
        protected Response getResponse(IHTTPSession session, @NonNull String name) throws IOException, NanoHTTPD.ResponseException
        {
            synchronized (this) {
                RobotLog.i("ChangeApPassword " + name);
                final PasswordManager passwordManager = PasswordManagerFactory.getInstance();
                if (passwordManager.setPassword(name) == true) {
                    return RobotWebHandlerManager.OK_RESPONSE;
                } else {
                    return RobotWebHandlerManager.internalErrorResponse(TAG, "Invalid password");
                }
            }
        }
    }

    public static class ChangeApChannel extends RequireNameHandler
    {
        @Override
        protected Response getResponse(IHTTPSession session, @NonNull String channel) throws IOException, NanoHTTPD.ResponseException
        {
            synchronized (this) {
                RobotLog.i("ChangeApChannel " + channel);
                final ApChannelManager apChannelManager = ApChannelManagerFactory.getInstance();
                if (apChannelManager.setChannel(channel)) {
                    return RobotWebHandlerManager.OK_RESPONSE;
                } else {
                    return RobotWebHandlerManager.internalErrorResponse(TAG, "Invalid channel");
                }
            }
        }
    }

    /**
     * AnonymousPing, just send back an OK_RESPONSE.
     */
    public static class AnonymousPing implements WebHandler
    {
        @Override
        public Response getResponse(IHTTPSession session)
        {
            if (DEBUG) {
                RobotLog.dd(TAG, "In AnonymousPing");
            }
            return RobotWebHandlerManager.OK_RESPONSE;
        }
    }

    /**
     * Respond to a ping. See also LoggingPing. Responds with the list of current ping details
     */
    public static class ClientPing extends RequireNameHandler
    {
        final PingDetailsHolder pingDetailsHolder = new PingDetailsHolder();

        @Override
        public Response getResponse(NanoHTTPD.IHTTPSession session, @NonNull String name) throws IOException, NanoHTTPD.ResponseException
        {
            synchronized (pingDetailsHolder) {
                pingDetailsHolder.addPing(getPingDetails(session));
                pingDetailsHolder.removeOldPings();
                logPing(session);
                return NoCachingWebHandler.setNoCache(session, newFixedLengthResponse(Response.Status.OK, MimeTypesUtil.getMimeType("json"), pingDetailsHolder.toJson()));
            }
        }

        protected PingDetails getPingDetails(IHTTPSession session)
        {
            return PingDetails.from(session);
        }

        protected void logPing(NanoHTTPD.IHTTPSession session)
        {
            // hook for subclass
        }
    }

    /**
     * Reboot.
     */
    public static class Reboot implements WebHandler
    {
        public final static String TAG = Reboot.class.getSimpleName();
        private static final boolean ENABLE_REBOOT = true;

        /**
         * Reboot the Android board. This is only possible on a REV Control Hub,
         * as on regular robot controllers, we lack appropriate permissions.
         *
         * @param session one IHTTPSession that will return one Response
         * @return an "OK" response, if there is an error rebooting it
         * will be logged to the RobotLog.
         */
        @Override
        public Response getResponse(IHTTPSession session)
        {
            if (LynxConstants.isRevControlHub() && ENABLE_REBOOT) {
                ThreadPool.getDefault().submit(new Runnable()   // REVIEW: make a singleton?
                {
                    @Override
                    public void run()
                    {
                        try {
                            AppUtil.getInstance().showToast(UILocation.BOTH, AppUtil.getDefContext().getString(R.string.toastRebootRC));
                            Thread.sleep(1000);
                            (new ProcessBuilder("reboot")).start();
                        } catch (IOException e) {
                            RobotLog.ee(TAG, e, "unable to process reboot request");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
                return RobotWebHandlerManager.OK_RESPONSE;
            } else {
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, NanoHTTPD.MIME_PLAINTEXT, "Rebooting supported only on REV Control Hub");
            }
        }
    }

    public static class RobotControllerConfiguration implements WebHandler
    {
        @Override
        public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException
        {
            StringBuilder js = new StringBuilder();
            appendVariables(js);
            return newFixedLengthResponse(Response.Status.OK, MimeTypesUtil.MIME_JAVASCRIPT, js.toString());
        }

        protected void appendVariables(StringBuilder js)
        {
            appendVariable(js, "URI_ANON_PING", URI_ANON_PING);
            appendVariable(js, "URI_PING", URI_PING);
            appendVariable(js, "URI_LIST_LOG_FILES", URI_LIST_LOG_FILES);
            appendVariable(js, "URI_DOWNLOAD_FILE", URI_DOWNLOAD_FILE);
            appendVariable(js, "URI_RENAME_RC", URI_RENAME_RC);
            appendVariable(js, "URI_CHANGE_AP_PASSWORD", URI_CHANGE_AP_PASSWORD);
            appendVariable(js, "URI_CHANGE_AP_CHANNEL", URI_CHANGE_AP_CHANNEL);
            appendVariable(js, "URI_UPLOAD_EXPANSION_HUB_FIRMWARE", URI_UPLOAD_EXPANSION_HUB_FIRMWARE);
            appendVariable(js, "URI_UPDATE_CONTROL_HUB_APK", URI_UPDATE_CONTROL_HUB_APK);
            appendVariable(js, "URI_UPLOAD_WEBCAM_CALIBRATION_FILE", URI_UPLOAD_WEBCAM_CALIBRATION_FILE);
            appendVariable(js, "URI_UPLOAD_CONTROL_HUB_OTA", URI_UPLOAD_CONTROL_HUB_OTA);
            appendVariable(js, "URI_NAV_HOME", URI_NAV_HOME);
            appendVariable(js, "URI_NAV_MANAGE", URI_NAV_MANAGE);
            appendVariable(js, "URI_NAV_HELP", URI_NAV_HELP);
            appendVariable(js, "URI_RC_INFO", URI_RC_INFO);
            appendVariable(js, "URI_REBOOT", URI_REBOOT);
            appendVariable(js, "URI_COLORS", URI_COLORS);

            appendVariable(js, "PARAM_NAME", PARAM_NAME);
            appendVariable(js, "PARAM_NEW_NAME", PARAM_NEW_NAME);
            appendVariable(js, "PARAM_MESSAGE", PARAM_MESSAGE);

            appendVariable(js, "URI_EXIT_PROGRAM_AND_MANAGE", URI_EXIT_PROGRAM_AND_MANAGE);
            appendVariable(js, "URI_TOAST", URI_TOAST);
        }

        public static void appendVariable(StringBuilder js, String name, String value)
        {
            js.append("var ").append(name).append(" = '").append(value).append("';\n");
        }
    }

    /**
     * Dynamically synthesizes and returns a file containing 'less' variable
     * definitions corresponding to the current application theme colors.
     */
    public static class AppThemeColorsHandler implements WebHandler, WebObserver
    {
        public static final String TAG = AppThemeColorsHandler.class.getSimpleName();
        protected final Map<String, String> sessionColors = new ConcurrentHashMap<>();

        // Using the instance holder defers generation until *first*access*, by which time
        // the theme will have had a chance to be applied.
        protected static class InstanceHolder
        {
            public final static String ourColors = AppThemeColors.fromTheme().toLess();
        }

        @Override public void observe(IHTTPSession session)
        {
            // If there's an app theme header, then remember that as being associated with the session
            String appThemeHeader = session.getHeaders().get(AppThemeColors.htppHeaderNameLower);
            if (appThemeHeader != null) {
                String sessionCookie = SessionCookie.fromSession(session);
                if (sessionCookie != null) { // should never happen with well-behaved client
                    // RobotLog.vv(TAG, "found appThemeHeader this=0x%08x session=%s", this.hashCode(), sessionCookie);
                    sessionColors.put(sessionCookie, AppThemeColors.fromHeader(appThemeHeader));
                }
            }
        }

        @Override
        public Response getResponse(IHTTPSession session) throws IOException, NanoHTTPD.ResponseException
        {
            // Use session-specific colors if we have 'em; otherwise, our colors
            String sessionCookie = SessionCookie.fromSession(session);
            String colors = sessionCookie == null ? null : sessionColors.get(sessionCookie);
            if (colors == null) {
                // RobotLog.vv(TAG, "used rc colors: this=0x%08x session=%s", this.hashCode(), sessionCookie);
                colors = InstanceHolder.ourColors;
            } else {
                // RobotLog.vv(TAG, "used session colors: this=0x%08x session=%s", this.hashCode(), sessionCookie);
            }

            // Use no-cache since while it won't change while we are alive, it might get
            // cached across our lifetimes, which could be problematic.
            return NoCachingWebHandler.setNoCache(session, newFixedLengthResponse(Response.Status.OK, MimeTypesUtil.MIME_CSS, colors));
        }
    }

    public static class SimpleSuccess implements WebHandler
    {
        public final static String TAG = SimpleSuccess.class.getSimpleName();

        @Override public Response getResponse(IHTTPSession session)
        {
            return RobotWebHandlerManager.OK_RESPONSE;
        }
    }

    // do not instantiate
    private RobotControllerWebHandlers(){}
}
