package org.zlambda.projects;

import org.slf4j.Logger;
import org.zlambda.projects.context.SystemContext;
import org.zlambda.projects.utils.Common;

public class DebuggerThread extends Thread {
  private static final Logger LOGGER = Common.getSystemLogger();
  private final SystemContext context;

  public DebuggerThread(SystemContext context) {
    this.context = context;
  }

  @Override public void run() {
    if (!context.isStartDebugger()) {
      return;
    }
    while (true) {
      try {
        Thread.currentThread().sleep(500);
        Debugger systemDebugger = SystemContext.getSystemDebugger();
        systemDebugger.hashCode();
      } catch (Exception e) {}
    }
  }
}
