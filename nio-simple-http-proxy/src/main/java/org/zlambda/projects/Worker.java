package org.zlambda.projects;

import org.slf4j.Logger;
import org.zlambda.projects.context.WorkerContext;
import org.zlambda.projects.utils.Common;
import org.zlambda.projects.utils.SelectionKeyUtils;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Worker implements Runnable {
  private static final Logger LOGGER = Common.getSystemLogger();
      // 1 minute, simple for output active channels
  private final WorkerContext context;

  public Worker(WorkerContext context) {
    this.context = context;
  }

  @Override
  public void run() {
    try {
      context.setName(Thread.currentThread().getName());
      LOGGER.info("start worker <{}>.", context.getName());
      Selector selector = context.getSelector();
      while (true) {
        int selected = selector.select();
        synchronized (context.getWakeupBarrier()) {
        }
        context.setNumConnections(selector.keys().size());
        if (0 == selected) {
          continue;
        }
        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
          SelectionKey key = iterator.next();
          if (!key.isValid()) {
            LOGGER.error("got invalid key <{}>.", SelectionKeyUtils.getName(key));
          } else {
            ((EventHandler) key.attachment()).execute(key);
          }
          iterator.remove();
        }
      }
    } catch (Exception e) {
      LOGGER.error("got unexpected error. so terminate worker <{}>", context.getName(), e);
      synchronized (context.getContextSetMonitor()) {
        context.getContextSet().remove(context);
      }
      /**
       * close of selector will not immediate close the socket channel,
       * so we need to cleanup
       */
      for (SelectionKey key : context.getSelector().keys()) {
        SocketChannel socketChannel = SelectionKeyUtils.getSocketChannel(key);
        Common.close(socketChannel);
      }
      Common.close(context.getSelector());
    }
  }
}
