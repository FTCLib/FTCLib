/*
Copyright (c) 2017 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
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
package org.firstinspires.ftc.ftccommon.internal;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.qualcomm.ftccommon.LaunchActivityConstantsList;
import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil.DialogContext;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.ui.LocalByRefRequestCodeHolder;
import org.firstinspires.ftc.robotcore.internal.ui.ThemedActivity;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;
import org.firstinspires.ftc.robotcore.internal.webserver.FtcUserAgentCategory;
import org.firstinspires.ftc.robotcore.internal.webserver.RobotControllerWebInfo;
import org.firstinspires.ftc.robotserver.internal.webserver.AppThemeColors;
import org.firstinspires.ftc.robotserver.internal.webserver.MimeTypesUtil;
import org.firstinspires.ftc.robotserver.internal.webserver.RobotControllerWebHandlers;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ProgramAndManageActivity} hosts the robot controller console web
 * interface in an android WebView
 */
@SuppressWarnings("WeakerAccess")
public class ProgramAndManageActivity extends ThemedActivity
{
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "Console";
    @Override public String getTag() { return TAG; }
    @Override protected FrameLayout getBackBar() { return findViewById(org.firstinspires.inspection.R.id.backbar); }

    protected RobotControllerWebInfo webInfo;
    protected WebView webView;

    //----------------------------------------------------------------------------------------------
    // Web Glue
    //----------------------------------------------------------------------------------------------

    protected enum RequestCode { NONE, CHOOSE_FILE }

    protected class ProgramAndManageWebChromeClient extends WebChromeClient
    {
        // https://www.w3schools.com/jsref/met_win_alert.asp. Needs just OK
        @Override public boolean onJsAlert(WebView view, String url, String message, JsResult result)
        {
            RobotLog.vv(TAG, "onJsAlert() message=%s", message);
            return showJsAlert(view, url, message, result);
        }

        @Override public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result)
        {
            RobotLog.vv(TAG, "onJsBeforeUnload() url=%s message=%s", url, message);
            return showJsAlert(view, url, message, result);
        }

        protected boolean showJsAlert(WebView view, String url, String message, final JsResult result)
        {
            showAlert(message, new Consumer<AppUtil.DialogContext>()
            {
                @Override public void accept(AppUtil.DialogContext context)
                {
                    result.confirm();
                }
            });
            return true;
        }

        // https://www.w3schools.com/jsref/met_win_confirm.asp. Needs OK vs Cancel
        @Override public boolean onJsConfirm(WebView view, String url, String message, final JsResult result)
        {
            RobotLog.vv(TAG, "onJsConfirm() url=%s message=%s", url, message);
            showConfirm(message, new Consumer<DialogContext>()
            {
                @Override public void accept(DialogContext dialogContext)
                {
                    Assert.assertFalse(dialogContext.getOutcome() == DialogContext.Outcome.UNKNOWN);
                    if (dialogContext.getOutcome() == AppUtil.DialogContext.Outcome.CONFIRMED)
                    {
                        result.confirm();
                    }
                    else
                    {
                        result.cancel();
                    }
                }
            });
            return true;
        }

        // https://www.w3schools.com/jsref/met_win_prompt.asp. Needs OK, Cancel, and a text box
        @Override public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result)
        {
            RobotLog.vv(TAG, "onJsPrompt() url=%s message=%s default=%s", url, message, defaultValue);
            showPrompt(message, defaultValue, new Consumer<DialogContext>()
            {
                @Override public void accept(DialogContext dialogContext)
                {
                    Assert.assertFalse(dialogContext.getOutcome() == DialogContext.Outcome.UNKNOWN);
                    if (dialogContext.getOutcome() == AppUtil.DialogContext.Outcome.CONFIRMED)
                    {
                        result.confirm(dialogContext.getText().toString());
                    }
                    else
                    {
                        result.cancel();
                    }
                }
            });
            return true;
        }

        /**
         * Tell the client to open a file chooser.
         * @param uploadFile A ValueCallback to set the URI of the file to upload.
         *      onReceiveValue must be called to wake up the thread.a
         * @param acceptType The value of the 'accept' attribute of the input tag
         *         associated with this file picker.
         * @param capture The value of the 'capture' attribute of the input tag
         *         associated with this file picker.
         * @hide
         *
         * This method was present and and hidden in KitKat. it seems to have been *removed* for
         * Lollipop rather than deprecated (probably because it was hidden).
         */
        /*@Override*/
        public void openFileChooser(final ValueCallback<Uri> uploadFile, String acceptType, String capture)
        {
            RobotLog.vv(TAG, "openFileChooser(): acceptType=%s capture=%s", acceptType, capture);
            Assert.assertNotNull(uploadFile);
            showFileChooser(new ValueCallback<Uri[]>()
            {
                @Override public void onReceiveValue(Uri[] value)
                {
                    uploadFile.onReceiveValue(value != null && value.length > 0 ? value[0] : null);
                }
            }, acceptType);
        }


        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) // FileChooserParams is >= 21
            {
                String[] types = fileChooserParams.getAcceptTypes();
                String type = (types.length > 0) ? types[0] : null;

                showFileChooser(filePathCallback, type);
                return true;
            }
            else
            {
                RobotLog.vv(TAG, "onShowFileChooser(): unexpected on this API level");
                return false;
            }
        }

        protected void showFileChooser(ValueCallback<Uri[]> filePathCallback, @Nullable String type)
        {
            RobotLog.vv(TAG, "showFileChooser(): type=%s", type);
            boolean launched = false;   // make sure we ALWAYS call back, even under failures
            try {
                // https://developer.android.com/reference/android/content/Intent.html#ACTION_GET_CONTENT
                Intent intentGetContent = new Intent(Intent.ACTION_GET_CONTENT);
                intentGetContent.addCategory(Intent.CATEGORY_OPENABLE);
                intentGetContent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

                String extension = type != null && type.startsWith(".") && type.indexOf('/') < 0
                        ? type.substring(1)
                        : null;

                // Null type check here is paranoia. Either way, first try what we've been asked for
                if (type == null) type = "*/*";
                intentGetContent.setType(type);

                // Try the system mime type map
                if (intentGetContent.resolveActivity(getPackageManager()) == null && extension != null)
                {
                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    if (type != null)
                    {
                        intentGetContent.setType(type);
                    }
                }

                // Try OUR mime type map
                if (intentGetContent.resolveActivity(getPackageManager()) == null && extension != null)
                {
                    type = MimeTypesUtil.getMimeType(extension);
                    if (type != null)
                    {
                        intentGetContent.setType(type);
                    }
                }

                // Try to handle systems (like Amazon Fire) which are deficient in their support for ACTION_GET_CONTENT
                if (intentGetContent.resolveActivity(getPackageManager()) == null)
                {
                    // https://developer.amazon.com/public/solutions/devices/kindle-fire/specifications/05-supported-android-intents#actiongetcontent
                    intentGetContent.setType("image/*");
                }

                // Give up if it just wont work
                if (intentGetContent.resolveActivity(getPackageManager()) == null)
                {
                    // It appears that NO (?) Android ships with a BUILT-IN file chooser?
                    // That said, there are many available in the Google Play store:
                    //
                    //      https://play.google.com/store/search?q=file%20chooser&c=apps
                    //
                    // Watch out for the permissions they request: many of them ask for WAY more than
                    // they ought too; I wonder what they're doing with that? Many also seem to
                    // have a poor virus reputation. Some seem quite promising, though:
                    //
                    //      https://play.google.com/store/apps/details?id=org.openintents.filemanager
                    //      https://www.amazon.com/dp/B00PUGKX2Y/ref=pe_385040_118058080_TE_M1DP
                    //
                    showAlert(getString(R.string.alertMessageNoActionGetContent), null);
                    return;
                }

                // Dynamically remember contextual state associated with this request
                LocalByRefRequestCodeHolder<ValueCallback<Uri[]>> holder = new LocalByRefRequestCodeHolder<>(RequestCode.CHOOSE_FILE.ordinal(), filePathCallback);

                RobotLog.vv(TAG, "showFileChooser(): launching get=%s", intentGetContent);
                try {
                    startActivityForResult(intentGetContent, holder.getActualRequestCode());
                    launched = true;
                }
                catch (ActivityNotFoundException e)
                {
                    RobotLog.ee(TAG, e, "internal error");  // logic above should prevent this
                }
            }
            finally
            {
                if (!launched)
                {
                    filePathCallback.onReceiveValue(null);
                }
            }
        }

        @Override public boolean onConsoleMessage(ConsoleMessage consoleMessage)
        {
            URI uri = URI.create(consoleMessage.sourceId());
            RobotLog.dd(TAG, "%s(%s,%d): %s",
                    consoleMessage.messageLevel().toString().toLowerCase(Locale.getDefault()),
                    uri.getPath(),
                    consoleMessage.lineNumber(),
                    consoleMessage.message());
            return true;
        }
    }

    // This version doesn't filter out any illegal characters
    protected String getUrlParam(String urlString, String paramName)
    {
        UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
        sanitizer.setAllowUnregisteredParamaters(true);
        sanitizer.registerParameter(paramName, UrlQuerySanitizer.getAllButNulLegal());
        sanitizer.parseUrl(urlString);
        return sanitizer.getValue(paramName);
    }

    protected class ProgramAndManageWebViewClient extends WebViewClient
    {
        @Override public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            RobotLog.vv(TAG, "shouldOverrideUrlLoading() url=%s", url);
            // don't let redirects escape
            return false;
        }

        /** We use the deprecated version since we need to support API 19 */
        @Override public WebResourceResponse shouldInterceptRequest(WebView view, String urlString)
        {
            // RobotLog.vv(TAG, "shouldInterceptRequest() url=%s", url);
            URI url = URI.create(urlString);
            if (RobotControllerWebHandlers.URI_EXIT_PROGRAM_AND_MANAGE.equals(url.getPath()))
            {
                // Exit back to the driver station main screen
                RobotLog.vv(TAG, "exiting Program & Manage");
                ProgramAndManageActivity.this.finish();
                return null;
            }
            else if (RobotControllerWebHandlers.URI_TOAST.equals(url.getPath()))
            {
                // Show the requested toast
                RobotLog.vv(TAG, "toast url=%s", urlString);
                String message = getUrlParam(urlString, RobotControllerWebHandlers.PARAM_MESSAGE);
                if (message != null)
                {
                    AppUtil.getInstance().showToast(UILocation.ONLY_LOCAL, message);
                }
                return null; // will use SimpleSuccess in our web handlers: easiest thing to do
            }
            return null;
        }
    }

    @Override protected void onActivityResult(int actualRequestCode, int resultCode, Intent data)
    {
        RobotLog.vv(TAG, "onActivityResult() requestCode=%d resultCode=%d data=%s", actualRequestCode, resultCode, data);
        LocalByRefRequestCodeHolder holder = LocalByRefRequestCodeHolder.from(actualRequestCode);
        if (holder != null)
        {
            int requestCode = holder.getUserRequestCode();
            if (requestCode == RequestCode.CHOOSE_FILE.ordinal())
            {
                Uri[] result = (data==null || resultCode!=RESULT_OK)
                        ? null
                        : new Uri[] { data.getData() };
                RobotLog.vv(TAG, "CHOOSE_FILE result=%s", result==null ? null : result[0]);

                ValueCallback<Uri[]> filePathCallback = (ValueCallback<Uri[]>)holder.getTargetAndForget();

                filePathCallback.onReceiveValue(result);
            }
            else
            {
                RobotLog.ee(TAG, "onActivityResult() user requestCode=%d: unexpected", requestCode);
            }
        }
        else
        {
            RobotLog.ee(TAG, "onActivityResult() actual requestCode=%d: unexpected", actualRequestCode);
        }
    }

    protected DialogContext showAlert(String message, @Nullable Consumer<DialogContext> runOnDismiss)
    {
        AppUtil.DialogParams params = new AppUtil.DialogParams(UILocation.ONLY_LOCAL, getString(R.string.alertTitleRobotControllerConsole), message);
        params.activity = this;
        params.flavor = AppUtil.DialogFlavor.ALERT;
        return AppUtil.getInstance().showDialog(params, runOnDismiss);
    }
    protected DialogContext showConfirm(String message, @Nullable Consumer<DialogContext> runOnDismiss)
    {
        AppUtil.DialogParams params = new AppUtil.DialogParams(UILocation.ONLY_LOCAL, getString(R.string.alertTitleRobotControllerConsole), message);
        params.activity = this;
        params.flavor = AppUtil.DialogFlavor.CONFIRM;
        return AppUtil.getInstance().showDialog(params, runOnDismiss);
    }
    protected DialogContext showPrompt(String message, @Nullable String defaultValue, @Nullable Consumer<DialogContext> runOnDismiss)
    {
        AppUtil.DialogParams params = new AppUtil.DialogParams(UILocation.ONLY_LOCAL, getString(R.string.alertTitleRobotControllerConsole), message);
        params.activity = this;
        params.flavor = AppUtil.DialogFlavor.PROMPT;
        params.defaultValue = defaultValue;
        return AppUtil.getInstance().showDialog(params, runOnDismiss);
    }

    // https://stackoverflow.com/questions/33434532/android-webview-download-files-like-browsers-do
    protected class ProgramAndManageDownloadListener extends BroadcastReceiver implements DownloadListener
    {
        protected Set<Long> outstandingDownloadIds = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

        @Override public void onDownloadStart(String urlString, String userAgent, String contentDisposition, String mimetype, long contentLength)
        {
            // Normally, the name would be taken from the 'filename' parameter to the
            // 'Content-Disposition' header of the actual retrieval. But in the architecture
            // here, we (surprisingly) don't seem to have access to that information. So
            // we have to make do.

            // Our logic only ever attempts to download to URI_DOWNLOAD_FILE, so we take advantage of that
            String filename = null;
            URI uri = URI.create(urlString);
            if (RobotControllerWebHandlers.URI_DOWNLOAD_FILE.equals(uri.getPath()))
            {
                filename = getUrlParam(urlString, RobotControllerWebHandlers.PARAM_NAME);
                if (filename != null)
                {
                    filename = new File(filename).getName();
                }
            }
            if (filename == null)
            {
                filename = URLUtil.guessFileName(urlString, contentDisposition, mimetype);
            }

            RobotLog.vv(TAG, "onDownloadStart(url=%s disp='%s' mime='%s' length=%d) filename=%s", urlString, contentDisposition, mimetype, contentLength, filename);

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlString));
            request.setDescription(getString(R.string.webViewDownloadRequestDescription));
            request.setTitle(filename);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

            DownloadManager downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            long downloadId = downloadManager.enqueue(request); Assert.assertFalse(downloadId==0);
            outstandingDownloadIds.add(downloadId);
            AppUtil.getInstance().showToast(UILocation.ONLY_LOCAL, getString(R.string.toastWebViewDownloadFile, filename));
        }

        public void register()
        {
            registerReceiver(this, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }

        public void unregister()
        {
            unregisterReceiver(this);
        }

        @Override public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                if (outstandingDownloadIds.remove(downloadId) && outstandingDownloadIds.size()==0)
                {
                    AppUtil.getInstance().showToast(UILocation.ONLY_LOCAL, getString(R.string.toastWebViewDownloadsComplete));
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Life Cycle
    //----------------------------------------------------------------------------------------------

    final ProgramAndManageWebChromeClient webChromeClient = new ProgramAndManageWebChromeClient();
    final ProgramAndManageWebViewClient webViewClient = new ProgramAndManageWebViewClient();
    final ProgramAndManageDownloadListener downloadListener = new ProgramAndManageDownloadListener();

    @SuppressLint("SetJavaScriptEnabled") @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        RobotLog.vv(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        downloadListener.register();

        setContentView(R.layout.activity_program_and_manage);

        webInfo = RobotControllerWebInfo.fromJson(getIntent().getStringExtra(LaunchActivityConstantsList.RC_WEB_INFO));
        webView = (WebView) findViewById(R.id.webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUserAgentString(FtcUserAgentCategory.addToUserAgent(WebSettings.getDefaultUserAgent(this)));

        webView.setWebChromeClient(webChromeClient);
        webView.setWebViewClient(webViewClient);
        webView.setDownloadListener(downloadListener);

        /* probably not worthwhile
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
            // On KitKat and below, this was the default, so we don't need to explicitly
            // accept. We are permissive here because (a) we're locked down, unable to get
            // at any sites but our own on the RC, and (b) it helps avoid any security-blocks
            // that might surprise us. In particular, let us remember that we won't be using
            // DNS to connect, but rather IP addresses. So the 'domain matching' involved in
            // evaluating third-party-cookie status is likely problematic.
            CookieManager.getInstance().acceptThirdPartyCookies(webView);
            }*/

        // We pass in our theme so that it's consistent with OUR look and feel rather than
        // the other guy. Really only important on the DS; on the RC, it should be a no-op.
        Map<String,String> headers = new HashMap<>();
        headers.put(AppThemeColors.httpHeaderName, AppThemeColors.toHeader(AppThemeColors.fromTheme().toLess()));

        // Lauch ourselves into our robot controller!
        webView.loadUrl(webInfo.getServerUrl(), headers);
    }

    /* make the back button back out pages before backing out of the activity */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (event.getAction() == KeyEvent.ACTION_DOWN)
        {
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack())
                    {
                        webView.goBack();
                        return true;
                    }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onResume()
    {
        RobotLog.vv(TAG, "onResume()");
        super.onResume();
    }

    @Override protected void onPause()
    {
        RobotLog.vv(TAG, "onPause()");
        super.onPause();
    }

    @Override protected void onDestroy()
    {
        RobotLog.vv(TAG, "onDestroy()");
        super.onDestroy();
        downloadListener.unregister();
    }
}
