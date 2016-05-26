package org.zlambda.projects;

import org.slf4j.Logger;
import org.zlambda.projects.context.SystemContext;
import org.zlambda.projects.utils.Common;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class SimpleHttpProxy {
  private static final Logger LOGGER = Common.getSystemLogger();
  private final SystemContext systemContext;
  private final List<Thread> failThenTerminateJVM;

  public SimpleHttpProxy() {
    systemContext = new SystemContext.Builder()
        .clientQueue(new LinkedBlockingQueue<>())
        .numWorkers(Integer.parseInt(System.getProperty("worker", "4")))
        .port(Integer.parseInt(System.getProperty("port", "9999")))
        /**
         * Each proxy connection use 2 channelBufferS,
         * one for upstream   [Client -> Host]
         * one for downstream [Client <- Host]
         */
        .channelBufferSize(Integer.parseInt(System.getProperty("channelBufferSize", "10"))) // unit KB, so default is 10KB
        .startDebugger(Boolean.parseBoolean(System.getProperty("startDebugger", "false")))
        .build();
    LOGGER.info("current system settings:\n{}", systemContext);
    failThenTerminateJVM = Arrays.asList(
        new ConnectionListener(systemContext),
        new Dispatcher(systemContext),
        new DebuggerThread(systemContext)
    );
  }

  public void start() {
    failThenTerminateJVM.forEach(Thread::start);
  }

  public static void main(String[] args) {
    new SimpleHttpProxy().start();
  }
}
