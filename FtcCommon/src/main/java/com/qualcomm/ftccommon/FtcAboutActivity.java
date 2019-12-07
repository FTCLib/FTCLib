package com.qualcomm.ftccommon;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.widget.FrameLayout;

import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.robocol.RobocolConfig;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;
import com.qualcomm.robotcore.util.Version;
import com.qualcomm.robotcore.wifi.NetworkConnection;

import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.network.DeviceNameManagerFactory;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.RecvLoopRunnable;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.ui.ThemedActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class FtcAboutActivity extends ThemedActivity
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "FtcDriverStationAboutActivity";
	@Override public String getTag() { return TAG; }
    @Override protected FrameLayout getBackBar() { return findViewById(org.firstinspires.inspection.R.id.backbar); }

    protected final Context         context = AppUtil.getDefContext();
    protected final boolean         remoteConfigure = AppUtil.getInstance().isDriverStation();
    protected       AboutFragment   aboutFragment;
    protected       Future          refreshFuture = null;

    final RecvLoopRunnable.RecvLoopCallback recvLoopCallback = new RecvLoopRunnable.DegenerateCallback()
        {
        @Override public CallbackResult commandEvent(Command command)
            {
            RobotLog.vv(TAG, "commandEvent: %s", command.getName());
            if (remoteConfigure)
                {
                switch (command.getName())
                    {
                    case RobotCoreCommandList.CMD_REQUEST_ABOUT_INFO_RESP: {
                        final RobotCoreCommandList.AboutInfo aboutInfo = RobotCoreCommandList.AboutInfo.deserialize(command.getExtra());
                        AppUtil.getInstance().runOnUiThread(new Runnable()
                            {
                            @Override public void run()
                                {
                                refreshRemote(aboutInfo);
                                }
                            });
                        return CallbackResult.HANDLED;
                        }
                    }
                }
            return CallbackResult.NOT_HANDLED;
            }
        };

    //----------------------------------------------------------------------------------------------
    // Life Cycle
    //----------------------------------------------------------------------------------------------

    public static CommandList.AboutInfo getLocalAboutInfo()
        {
        RobotCoreCommandList.AboutInfo info = new RobotCoreCommandList.AboutInfo();
        info.appVersion = getAppVersion();
        info.libVersion = Version.getLibraryVersion();
        info.buildTime = getBuildTime();
        info.networkProtocolVersion = String.format(Locale.US, "v%d", RobocolConfig.ROBOCOL_VERSION);
        info.osVersion = LynxConstants.getControlHubOsVersion();

        NetworkConnection networkConnection = NetworkConnectionHandler.getInstance().getNetworkConnection();
        if (networkConnection != null)
            {
            info.networkConnectionInfo = networkConnection.getInfo();
            }
        else
            {
            info.networkConnectionInfo = AppUtil.getDefContext().getString(R.string.unavailable);
            }
        return info;
        }

    protected static String getAppVersion()
        {
        Context context = AppUtil.getDefContext();
        String appVersion;
        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            }
        catch (PackageManager.NameNotFoundException e)
            {
            appVersion = context.getString(R.string.unavailable);
            }
        return appVersion;
        }

    /** https://code.google.com/p/android/issues/detail?id=220039 */
    protected static String getBuildTime()
        {
        Context context = AppUtil.getDefContext();
        String buildTime = context.getString(R.string.unavailable);
        try
            {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            ZipFile zf = new ZipFile(ai.sourceDir);
            ZipEntry ze = zf.getEntry("classes.dex");
            zf.close();
            long time = ze.getTime();
            buildTime = SimpleDateFormat.getInstance().format(new java.util.Date(time));
            }
        catch (PackageManager.NameNotFoundException|IOException e)
            {
            RobotLog.ee(TAG, e, "exception determining build time");
            }
        return buildTime;
        }

    //----------------------------------------------------------------------------------------------
    // AboutFragment
    //----------------------------------------------------------------------------------------------

    public static class AboutFragment extends PreferenceFragment
        {
        protected final boolean remoteConfigure = AppUtil.getInstance().isDriverStation();
        private boolean firstRemoteRefresh = true;

        public void refreshLocal(RobotCoreCommandList.AboutInfo aboutInfo)
            {
            setPreferenceSummary(R.string.pref_app_version, aboutInfo.appVersion);
            setPreferenceSummary(R.string.pref_lib_version, aboutInfo.libVersion);
            setPreferenceSummary(R.string.pref_network_protocol_version, aboutInfo.networkProtocolVersion);
            setPreferenceSummary(R.string.pref_build_time, aboutInfo.buildTime);
            setPreferenceSummary(R.string.pref_network_connection_info, aboutInfo.networkConnectionInfo);
            setPreferenceSummary(R.string.pref_os_version, aboutInfo.osVersion);
            }

        public void refreshRemote(RobotCoreCommandList.AboutInfo aboutInfo)
            {
            if (remoteConfigure)
                {
                if (firstRemoteRefresh && aboutInfo.osVersion != null) // If the remote device reports an OS version, we need to add a preference for it
                    {
                    PreferenceCategory prefAppCategoryRc = (PreferenceCategory) findPreference(getString(R.string.pref_app_category_rc));
                    Preference osVersionPreference = new Preference(getPreferenceScreen().getContext());
                    osVersionPreference.setTitle(getString(R.string.about_ch_os_version));
                    osVersionPreference.setKey(getString(R.string.pref_os_version_rc));
                    prefAppCategoryRc.addPreference(osVersionPreference);
                    }
                firstRemoteRefresh = false;
                setPreferenceSummary(R.string.pref_app_version_rc, aboutInfo.appVersion);
                setPreferenceSummary(R.string.pref_lib_version_rc, aboutInfo.libVersion);
                setPreferenceSummary(R.string.pref_network_protocol_version_rc, aboutInfo.networkProtocolVersion);
                setPreferenceSummary(R.string.pref_build_time_rc, aboutInfo.buildTime);
                setPreferenceSummary(R.string.pref_network_connection_info_rc, aboutInfo.networkConnectionInfo);
                setPreferenceSummary(R.string.pref_os_version_rc, aboutInfo.osVersion);
                }
            }

        public void refreshAllUnavailable()
            {
            setPreferenceSummary(R.string.pref_app_version, null);
            setPreferenceSummary(R.string.pref_lib_version, null);
            setPreferenceSummary(R.string.pref_network_protocol_version, null);
            setPreferenceSummary(R.string.pref_build_time, null);
            setPreferenceSummary(R.string.pref_network_connection_info, null);
            setPreferenceSummary(R.string.pref_os_version, null);

            setPreferenceSummary(R.string.pref_app_version_rc, null);
            setPreferenceSummary(R.string.pref_lib_version_rc, null);
            setPreferenceSummary(R.string.pref_network_protocol_version_rc, null);
            setPreferenceSummary(R.string.pref_build_time_rc, null);
            setPreferenceSummary(R.string.pref_network_connection_info_rc, null);
            setPreferenceSummary(R.string.pref_os_version_rc, null);
            }

        @Override public void onCreate(Bundle savedInstanceState)
            {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(com.qualcomm.ftccommon.R.xml.ftc_about_activity);
            PreferenceCategory prefAppCategory = (PreferenceCategory) findPreference(getString(R.string.pref_app_category));
            prefAppCategory.setTitle(remoteConfigure ? R.string.prefcat_about_ds : R.string.prefcat_about_rc);

            if (remoteConfigure)
                {
                addPreferencesFromResource(com.qualcomm.ftccommon.R.xml.ftc_about_activity_rc);
                Preference prefAppCategoryRc = findPreference(getString(R.string.pref_app_category_rc));
                prefAppCategoryRc.setTitle(R.string.prefcat_about_rc);
                }
            else if (LynxConstants.getControlHubOsVersion() != null)
                {
                Preference osVersionPreference = new Preference(getPreferenceScreen().getContext());
                osVersionPreference.setTitle(getString(R.string.about_ch_os_version));
                osVersionPreference.setKey(getString(R.string.pref_os_version));
                prefAppCategory.addPreference(osVersionPreference);
                }

            refreshAllUnavailable();
            }

        protected void setPreferenceSummary(@StringRes int idPref, String value)
            {
            setPreferenceSummary(AppUtil.getDefContext().getString(idPref), value);
            }

        protected void setPreferenceSummary(String prefName, String value)
            {
            if (TextUtils.isEmpty(value))
                {
                value = AppUtil.getDefContext().getString(R.string.unavailable);
                }
            Preference preference = findPreference(prefName);
            if (preference != null)
                {
                preference.setSummary(value);
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Refreshing
    //----------------------------------------------------------------------------------------------

    protected void startRefreshing()
        {
        stopRefreshing();
        int msInterval = 5000;
        refreshFuture = ThreadPool.getDefaultScheduler().scheduleAtFixedRate(new Runnable()
            {
            @Override public void run()
                {
                AppUtil.getInstance().runOnUiThread(new Runnable()
                    {
                    @Override public void run()
                        {
                        refresh();
                        }
                    });
                }
            }, 0, msInterval, TimeUnit.MILLISECONDS);
        }

    protected void stopRefreshing()
        {
        if (refreshFuture != null)
            {
            refreshFuture.cancel(false);
            refreshFuture = null;
            }
        }

    protected void refreshRemote(RobotCoreCommandList.AboutInfo aboutInfo)
        {
        aboutFragment.refreshRemote(aboutInfo);
        }

    protected void refresh()
        {
        aboutFragment.refreshLocal(getLocalAboutInfo());
        if (remoteConfigure)
            {
            NetworkConnectionHandler.getInstance().sendCommand(new Command(RobotCoreCommandList.CMD_REQUEST_ABOUT_INFO));
            }
        }

    //----------------------------------------------------------------------------------------------
    // Activity Life Cycle
    //----------------------------------------------------------------------------------------------

    @Override protected void onCreate(Bundle savedInstanceState)
        {
        RobotLog.vv(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_settings);

        // Always make sure we have a real device name before we launch
        DeviceNameManagerFactory.getInstance().initializeDeviceNameIfNecessary();

        // Display the fragment as the main content.
        aboutFragment = new AboutFragment();

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, aboutFragment)
                .commit();

        NetworkConnectionHandler.getInstance().pushReceiveLoopCallback(recvLoopCallback);
        }

    @Override
    protected void onResume()
        {
        super.onResume();
        startRefreshing();
        }

    @Override
    protected void onPause()
        {
        stopRefreshing();
        super.onPause();
        }

    @Override protected void onDestroy()
        {
        RobotLog.vv(TAG, "onDestroy()");
        super.onDestroy();
        NetworkConnectionHandler.getInstance().removeReceiveLoopCallback(recvLoopCallback);
        }
    }
