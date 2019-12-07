package org.firstinspires.ftc.robotserver.internal.programmingmode;

import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;

import static org.firstinspires.ftc.robotserver.internal.webserver.RobotControllerWebHandlers.decorateWithParms;

public class ProgrammingModeWebHandlerDecorator {
  private final ProgrammingModeManager manager;

  public ProgrammingModeWebHandlerDecorator(ProgrammingModeManager manager) {
    this.manager = manager;
  }

  public <T extends WebHandler> WebHandler decorate(T handler, boolean decorateWithParams) {
    WebHandler wh = decorateWithParams ? decorateWithParms(handler) : handler;
    return decorateWithLogging(wh);
  }

  /**
   * add logging to a {@link WebHandler}
   */
  public WebHandler decorateWithLogging(WebHandler delegate) {
    if (delegate instanceof LoggingHandler) {
      return delegate;
    } else {
      return new LoggingHandler(manager, delegate);
    }
  }
}
