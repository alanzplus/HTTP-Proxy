package org.zlambda.projects;

import org.slf4j.Logger;
import org.zlambda.projects.context.SystemContext;
import org.zlambda.projects.utils.Common;

public class DebuggerThread extends Thread {
  private static final Logger LOGGER = Common.getSystemLogger();
  private final SystemContext context;
  private final int _30_SECONDS = 30 * 1000;

  public DebuggerThread(SystemContext context) {
    this.context = context;
  }

  @Override public void run() {
    if (!context.isStartDebugger()) {
      return;
    }
    while (true) {
      try {
        /**
         * Since the aim here just for monitoring if there is some unreleased channels after some time
         * so setting the dump period to 30 seconds seems enough.
         */
        Thread.currentThread().sleep(_30_SECONDS);
        LOGGER.info("active channels \n{}",
                    SystemContext.getSystemDebugger().cleanThenDumpActiveChannels());
      } catch (Exception e) {
        LOGGER.error("Debugger failed.", e);
      }
    }
  }
}
