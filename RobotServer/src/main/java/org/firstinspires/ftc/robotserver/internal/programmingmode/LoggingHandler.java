package org.firstinspires.ftc.robotserver.internal.programmingmode;

import com.qualcomm.robotcore.util.RobotLog;
import fi.iki.elonen.NanoHTTPD;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;
import org.firstinspires.ftc.robotserver.internal.webserver.WebHandlerDecorator;

import java.io.IOException;

/**
 * Add logging to a WebHandler. This object decorates any RequestHandler that needs to
 * be logged by the ProgrammingModeServer.
 */
public class LoggingHandler extends WebHandlerDecorator {

  private ProgrammingModeManager programmingModeManager;

  public LoggingHandler(ProgrammingModeManager programmingModeManager, WebHandler delegate) {
    super(delegate);
    this.programmingModeManager = programmingModeManager;
  }

  @Override
  public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
    addLogging(session);
    return super.getResponse(session);
  }

  private void addLogging(NanoHTTPD.IHTTPSession session) {
    RobotLog.vv(ProgrammingModeManager.TAG, "serve uri=%s", session.getUri());
  }
}
