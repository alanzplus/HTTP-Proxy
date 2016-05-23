package org.zlambda.projects;

import org.zlambda.projects.context.SystemContext;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class SimpleHttpProxy {
  private final SystemContext systemContext;
  private final List<Thread> failThenTerminateJVMThreads;

  public SimpleHttpProxy() {
    systemContext = new SystemContext.Builder()
        .clientQueue(new LinkedBlockingQueue<>())
        .numWorkers(Integer.parseInt(System.getProperty("worker", "4")))
        .port(Integer.parseInt(System.getProperty("port", "9999")))
        .build();
    failThenTerminateJVMThreads = Arrays.asList(
        new ConnectionListener(systemContext),
        new Dispatcher(systemContext)
    );
  }

  public void start() {
    failThenTerminateJVMThreads.forEach(Thread::start);
  }

  public static void main(String[] args) {
    new SimpleHttpProxy().start();
  }
}
