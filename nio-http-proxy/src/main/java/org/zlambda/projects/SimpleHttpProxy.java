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
        .numWorkers(Integer.parseInt(System.getProperty("worker", "8")))
        .port(Integer.parseInt(System.getProperty("port", "9999")))
        .enableMonitor(Boolean.parseBoolean(System.getProperty("enableMonitor", "true")))
        /**
         * Each proxy connection use 2 channelBufferS,
         * one for upstream   [Client -> Host]
         * one for downstream [Client <- Host]
         */
        .minBuffers(Integer.parseInt(System.getProperty("minNumBuffers", "100")))
        .maxBuffers(Integer.parseInt(System.getProperty("maxNumBuffers", "200")))
        .bufferSize(Integer.parseInt(System.getProperty("bufferSize", "10"))) // unit KB
        .useDirectBuffer(Boolean.parseBoolean(System.getProperty("useDirectBuffer", "true")))
        .monitorUpdateInterval(Integer.parseInt(System.getProperty("monitorUpdateInterval", "30"))) // second
        .build();

    LOGGER.info("current system settings:\n{}", systemContext);
    MonitorSingleton.init(systemContext);
    failThenTerminateJVM = Arrays.asList(
        new ConnectionListener(systemContext),
        new Dispatcher(systemContext),
        new MonitorThread(systemContext)
    );
  }

  public static void main(String[] args) {
    new SimpleHttpProxy().start();
  }

  public void start() {
    failThenTerminateJVM.forEach(Thread::start);
  }
}
