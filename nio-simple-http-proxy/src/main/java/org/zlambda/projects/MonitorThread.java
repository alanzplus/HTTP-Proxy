package org.zlambda.projects;

import org.slf4j.Logger;
import org.zlambda.projects.context.SystemContext;
import org.zlambda.projects.utils.Common;

public class MonitorThread extends Thread {
  private static final Logger LOGGER = Common.getSystemLogger();
  private final SystemContext context;
  private final int SECOND = 1000;

  public MonitorThread(SystemContext context) {
    super(MonitorThread.class.getSimpleName());
    this.context = context;
  }

  @Override
  public void run() {
    if (!context.enableMonitor()) {
      return;
    }
    while (true) {
      try {
        sleep(context.getMonitorUpdateInterval() * SECOND);
        LOGGER.info("Monitor Stats\n{}", MonitorSingleton.get().dumpStats());
      } catch (Exception e) {
        LOGGER.error("Debugger failed.", e);
      }
    }
  }
}
