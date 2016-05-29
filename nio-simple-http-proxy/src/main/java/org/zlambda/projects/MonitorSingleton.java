package org.zlambda.projects;

import org.zlambda.projects.buffer.DirectChannelBufferPool;
import org.zlambda.projects.context.ConnectionContext;
import org.zlambda.projects.context.SystemContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MonitorSingleton {
  private static Monitor INSTANCE = null;

  private MonitorSingleton() {
  }

  public static Monitor get() {
    return INSTANCE;
  }

  public static void init(final SystemContext context) {
    INSTANCE = context.enableMonitor() ? create(context) : createDummy();
  }

  private static Monitor create(SystemContext context) {
    return new Monitor() {
      private final Object channelMapMonitor = new Object();
      private final Map<ConnectionContext, ConnectionContext> CHANNEL_STATS = new HashMap<>();
      private final ConnectionContext dummyContext = new ConnectionContext();

      @Override
      public void collectChannelPair(ConnectionContext client, ConnectionContext host) {
        synchronized (channelMapMonitor) {
          CHANNEL_STATS.put(client, null == host ? dummyContext : host);
        }
      }

      /**
       * This method can always dump the latest state of the channel, because the "toString" method
       * of socketChannel has internal locking ! (channel is thread-safe class)
       */
      @Override
      public String dumpStats() {
        StringBuilder sb = new StringBuilder();
        synchronized (channelMapMonitor) {
          Iterator<Map.Entry<ConnectionContext, ConnectionContext>> iterator =
              CHANNEL_STATS.entrySet().iterator();
          while (iterator.hasNext()) {
            Map.Entry<ConnectionContext, ConnectionContext> next = iterator.next();
            ConnectionContext client = next.getKey();
            ConnectionContext host = next.getValue();
            if (!client.isOpen() && (dummyContext.equals(host) || !host.isOpen())) {
              iterator.remove();
            } else {
              String line = String.format(
                  "%s -> %s\n", client.getChannel(),
                  dummyContext.equals(host) ? "un-register" : host.getChannel());
              sb.append(line.replaceAll("java\\.nio\\.channels\\.SocketChannel", ""));
            }
          }
        }
        if (context.isUseDirectBuffer()) {
          DirectChannelBufferPool bufferPool = (DirectChannelBufferPool) context.getBufferPool();
          sb.append(String.format("un-release buffers %d\n", bufferPool.numUsedBuffers()));
        }
        return sb.toString();
      }
    };
  }

  private static Monitor createDummy() {
    return new Monitor() {
      @Override
      public void collectChannelPair(ConnectionContext client, ConnectionContext host) {

      }

      @Override
      public String dumpStats() {
        return "";
      }
    };
  }
}
