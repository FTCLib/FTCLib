// Copyright 2016 Google Inc.

package org.firstinspires.ftc.robotcore.internal.webserver;

import com.qualcomm.robotcore.hardware.configuration.LynxConstants;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;
import org.firstinspires.ftc.robotcore.internal.hardware.android.AndroidBoard;
import org.firstinspires.ftc.robotcore.internal.network.DeviceNameManagerFactory;
import org.firstinspires.ftc.robotcore.internal.network.WifiUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * A class that contains various information about the robot controller's web server
 * that is useful to javascript.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
@SuppressWarnings("WeakerAccess")
public class RobotControllerWebInfo {

  public static final String TAG = RobotControllerWebInfo.class.getSimpleName();

  // NOTE: these names are known to javascript
  private final String deviceName;
  private final String networkName;
  private final String passphrase;
  private final String serverUrl;
  private final boolean serverIsAlive;
  private final long timeServerStartedMillis;
  private final String timeServerStarted;
  private final boolean isREVControlHub;
  private final boolean supports5GhzAp;
  private final boolean appUpdateRequiresReboot;
  private final boolean supportsOtaUpdate;
  private FtcUserAgentCategory ftcUserAgentCategory;

  public RobotControllerWebInfo(
      String networkName, String passphrase, String serverUrl,
      boolean serverIsAlive, long timeServerStartedMillis) {
    this.deviceName = DeviceNameManagerFactory.getInstance().getDeviceName();
    this.networkName = (networkName == null) ? this.deviceName : networkName;
    this.passphrase = passphrase;
    this.serverUrl = serverUrl;
    this.serverIsAlive = serverIsAlive;
    this.timeServerStartedMillis = timeServerStartedMillis;
    this.isREVControlHub = LynxConstants.isRevControlHub();
    this.ftcUserAgentCategory = FtcUserAgentCategory.OTHER;
    this.supports5GhzAp = WifiUtil.is5GHzAvailable();
    this.appUpdateRequiresReboot = !AndroidBoard.getInstance().hasControlHubUpdater(); // The Control Hub Updater replaces the old update method, which required a reboot
    this.supportsOtaUpdate = AndroidBoard.getInstance().hasControlHubUpdater();        // The Control Hub Updater enables the RC app to start OTA updates

    SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, h:mm aa", Locale.getDefault());
    this.timeServerStarted = formatter.format(new Date(timeServerStartedMillis));
  }

  public String getDeviceName() {
    return deviceName;
  }

  public String getNetworkName() {
    return networkName;
  }

  public String getPassphrase() {
    return passphrase;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public boolean isServerAlive() {
    return serverIsAlive;
  }

  public long getTimeServerStartedMillis() {
    return timeServerStartedMillis;
  }

  public String getTimeServerStarted() {
    return timeServerStarted;
  }

  public FtcUserAgentCategory getFtcUserAgentCategory() {
    return ftcUserAgentCategory;
  }

  public boolean isREVControlHub() {
    return isREVControlHub;
  }

  public boolean is5GhzApSupported() {
    return supports5GhzAp;
  }

  public boolean doesAppUpdateRequireReboot() {
    return appUpdateRequiresReboot;
  }

  public boolean isOtaUpdateSupported() {
    return supportsOtaUpdate;
  }

  // todo: fix realign the types of the input
  public void setFtcUserAgentCategory(Map<String, String> session) {
    String userAgent = session.get("user-agent");
    this.ftcUserAgentCategory = FtcUserAgentCategory.fromUserAgent(userAgent);
  }

  public String toJson() {
    return SimpleGson.getInstance().toJson(this);
  }

  public static RobotControllerWebInfo fromJson(String json) {
    return SimpleGson.getInstance().fromJson(json, RobotControllerWebInfo.class);
  }
}
