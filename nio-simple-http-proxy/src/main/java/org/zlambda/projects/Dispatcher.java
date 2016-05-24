package org.zlambda.projects;

import org.slf4j.Logger;
import org.zlambda.projects.buffer.ProxyConnectionBuffer;
import org.zlambda.projects.context.SelectionKeyContext;
import org.zlambda.projects.context.ShareContext;
import org.zlambda.projects.context.SystemContext;
import org.zlambda.projects.context.WorkerContext;
import org.zlambda.projects.utils.Common;
import org.zlambda.projects.utils.SocketChannelUtils;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Dispatcher extends Thread {
  /**
   * For development debug usage
   */
  private static final Logger LOGGER = Common.getSystemLogger();
  private final SystemContext systemContext;
  // for synchronize the contextSet update when worker thread exit
  private final Object contextSetMonitor = new Object();
  private final Set<WorkerContext> workerContextSet = new HashSet<>();
  private final ExecutorService executorService;

  public Dispatcher(SystemContext systemContext) {
    super(Dispatcher.class.getSimpleName());
    this.systemContext = systemContext;
    this.executorService = Executors.newFixedThreadPool(systemContext.getNumWorkers());
  }

  private void createAndStartWorker() throws Exception {
    WorkerContext context = new WorkerContext.Builder()
        .selector(Selector.open())
        .contextSet(workerContextSet)
        .contextSetMonitor(contextSetMonitor)
        .build();
    workerContextSet.add(context);
    executorService.submit(new Worker(context));
  }

  @Override public void run() {
    LOGGER.info("{} thread started", getName());
    try {
      List<String> activeChannelStats = new ArrayList<>();
      while (true) {
        SocketChannel client;
        try {
          client = systemContext.getClientQueue().take();
        } catch (InterruptedException e) {
          LOGGER.error("got interruptedException while taking clientQueue.", e);
          Thread.currentThread().interrupt();
          continue;
        }
        /**
         * Most of the time, contextSetMonitor is contention free. When worker thread exits due to
         * unexpected exception, the worker will try to remove its context from the context set. So
         * only at that time, there will be contention between dispatcher thread and worker thread.
         */
        synchronized (contextSetMonitor) {
          if (workerContextSet.size() < systemContext.getNumWorkers()) {
            createAndStartWorker();
          }
          int minNumChannels = Integer.MAX_VALUE;
          WorkerContext targetWorkerContext = null;
          Iterator<WorkerContext> it = workerContextSet.iterator();
          while (it.hasNext()) {
            WorkerContext ct = it.next();
            int num = ct.getNumConnections();
            if (minNumChannels > num) {
              minNumChannels = num;
              targetWorkerContext = ct;
            }
            activeChannelStats.add(String.format("[%s:%d]", ct.getName(), num));
          }
          /**
           * approximate stats
           */
          LOGGER.info("active channels stats <{}>", activeChannelStats);
          activeChannelStats.clear();
          try {
            synchronized (targetWorkerContext.getWakeupBarrier()) {
              targetWorkerContext.getSelector().wakeup();
              ShareContext shareContext = new ShareContext(
                  new ProxyConnectionBuffer(systemContext.getChannelBufferSize()));
              ClientSocketChannelHandler handler = new ClientSocketChannelHandler(shareContext);
              SelectionKey key = client.register(
                  targetWorkerContext.getSelector(), SelectionKey.OP_READ, handler);
              SelectionKeyContext.ChannelState channelState =
                  new SelectionKeyContext.ChannelState().setConnectState(true);
              shareContext.setClientKey(
                  key,
                  new SelectionKeyContext.Builder(key)
                      .name(SocketChannelUtils.getRemoteAddress(client).toString())
                      .channelState(channelState).build()
              );
            }
          } catch (ClosedChannelException e) {
            LOGGER.error("Failed to register socket channel <{}>, reason {}.",
                         SocketChannelUtils.getRemoteAddress(client), e.getCause(), e);
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("got unexpected exception: {}, so terminate application.", e.getCause(), e);
      System.exit(-1);
    }
  }
}
