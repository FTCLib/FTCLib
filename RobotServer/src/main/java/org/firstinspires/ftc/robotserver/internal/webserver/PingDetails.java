// Copyright 2016 Google Inc.

package org.firstinspires.ftc.robotserver.internal.webserver;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;
import org.firstinspires.ftc.robotcore.internal.webserver.FtcUserAgentCategory;

/**
 * Class representing the details of a ping request.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public final class PingDetails {
  public static final String TAG = PingDetails.class.getSimpleName();
  private static final Map<String, String> identityToMachineName = new HashMap<String, String>();

  // NOTE: these get serialized in GSON and thus exposed to javascript
  public final String identity;       // a SessionCookie (UUID), or an IP address in a pinch
  public final String machineName;    // e.g. "Windows #2"
  public final String name;           // e.g. "connection.html"

  private PingDetails(String identity, String name, String userAgent) {
    this.identity = identity;
    this.name = name;

	String machineType = getMachineType(userAgent);
    String machineName = identityToMachineName.get(identity);
    if (machineName == null || !machineName.startsWith(machineType)) {
      for (int i = 1; i < Integer.MAX_VALUE; i++) {
        machineName = machineType + " #" + i;
        if (!identityToMachineName.containsValue(machineName)) {
          break;
        }
      }
      identityToMachineName.put(identity, machineName);
    }
    this.machineName = machineName;
  }

  public static PingDetails from(NanoHTTPD.IHTTPSession session) {
    final Map<String, List<String>> parms = session.getParameters();
    final Map<String, String> headers = session.getHeaders();

    String name = parms.get(RobotControllerWebHandlers.PARAM_NAME).get(0);
    String remoteIp = headers.get("remote-addr");
    String userAgent = headers.get("user-agent");

    String identity = SessionCookie.fromSession(session);
    if (identity == null) {
      // should never happen with well-behaved clients, but use host ip as surrogate if it does
      RobotLog.vv(TAG, "cookie absent: using ip=%", remoteIp);
      identity = remoteIp;
    }

    return new PingDetails(identity, name, userAgent);
  }

  private static boolean testAgent(String agentLower, String target) {
    return agentLower.contains(target.toLowerCase());
  }

  private static String getMachineType(String userAgent) {
    switch (FtcUserAgentCategory.fromUserAgent(userAgent)) {
      case DRIVER_STATION: return "DriverStation";
      case ROBOT_CONTROLLER: return "RobotController";
      case OTHER: {
       String agentLower = userAgent.toLowerCase();
        if (testAgent(agentLower, "Windows Phone")) {
          return "WindowsPhone";
        } else if (testAgent(agentLower, "Windows")) {
          return "Windows";
        } else if (testAgent(agentLower, "Macintosh")) {
          return "Mac";
        } else if (testAgent(agentLower, "CrOS")) {
          return "ChromeBook";
        } else if (testAgent(agentLower, "android")) {
          return "Android";
        } else if (testAgent(agentLower, "iPhone")) {
          return "iPhone";
        } else if (testAgent(agentLower, "iPad")) {
          return "iPad";
        } else if (testAgent(agentLower, "X11")) {
          return "Unix";
        }
      }
    }
   return "(unknown)";
  }

  public String toJson() {
    return SimpleGson.getInstance().toJson(this);
  }

  public static PingDetails fromJson(String json) {
    return SimpleGson.getInstance().fromJson(json, PingDetails.class);
  }

  // java.lang.Object methods

  @Override
  public boolean equals(Object o) {
    // We key only of off identity (ie: session) so that we can detect, e.g., an 'exit' from
    // a DS or RC user agent and then a quick re-enter as separate clients
    if (o instanceof PingDetails) {
      PingDetails that = (PingDetails) o;
      return Objects.equals(this.identity, that.identity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.identity.hashCode();
  }
}
