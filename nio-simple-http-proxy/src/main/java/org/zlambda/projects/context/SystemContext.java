package org.zlambda.projects.context;

import org.zlambda.projects.Debugger;
import org.zlambda.projects.utils.SelectionKeyUtils;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class SystemContext {
  private static volatile Debugger DEBUGGER;
  private final BlockingQueue<SocketChannel> clientQueue;
  private final int port;
  private final int numWorkers;
  private final int channelBufferSize;
  private final boolean startDebugger;

  private SystemContext(Builder builder) {
    this.clientQueue = builder.clientQueue;
    this.port = builder.port;
    this.numWorkers = builder.numWorkers;
    this.channelBufferSize = builder.channelBufferSize;
    this.startDebugger = builder.startDebugger;
    initGlobalDebugger();
  }

  private void initGlobalDebugger() {
    DEBUGGER = this.startDebugger ? new DebuggerImpl() : new DummyDebugger();
  }

  public static Debugger getSystemDebugger() {
    return DEBUGGER;
  }

  public BlockingQueue<SocketChannel> getClientQueue() {
    return clientQueue;
  }

  public int getPort() {
    return port;
  }

  public int getNumWorkers() {
    return numWorkers;
  }

  public int getChannelBufferSize() {
    return channelBufferSize;
  }

  public boolean isStartDebugger() {
    return startDebugger;
  }

  @Override public String toString() {
    return String.format(
        "{\n" +
        "\tport : %d\n" +
        "\tnumWorkers: %d\n" +
        "\tchannelBufferSize: %d KB\n"  +
        "\tstartDebugger: %s\n"  +
        "}",
        getPort(), getNumWorkers(), getChannelBufferSize(), startDebugger ? "true" : "false"
    );
  }

  public static class Builder {
    private BlockingQueue<SocketChannel> clientQueue;
    private int port;
    private int numWorkers;
    private int channelBufferSize;
    private boolean startDebugger;

    public Builder clientQueue(BlockingQueue<SocketChannel> queue) {
      this.clientQueue = queue;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder numWorkers(int num) {
      this.numWorkers = num;
      return this;
    }

    public Builder channelBufferSize(int size) {
      this.channelBufferSize = size;
      return this;
    }

    public Builder startDebugger(boolean startDebugger) {
      this.startDebugger = startDebugger;
      return this;
    }

    public SystemContext build() {
      return new SystemContext(this);
    }
  }

  private static class DummyDebugger implements Debugger {
    @Override public void collectChannelPair(SelectionKeyContext client, SelectionKeyContext host) {

    }

    @Override public String cleanThenDumpActiveChannels() {
      return "";
    }
  }


  private static class DebuggerImpl implements Debugger {
    private final Object channelMapMonitor = new Object();
    private final Map<SelectionKeyContext, SelectionKeyContext> CHANNEL_STATS = new HashMap<>();
    private final SelectionKeyContext dummyContext = new SelectionKeyContext
        .Builder(null)
        .channelState(new SelectionKeyContext.ChannelState().setConnectState(false))
        .build();

    public DebuggerImpl() {}

    @Override public void collectChannelPair(SelectionKeyContext client, SelectionKeyContext host) {
      synchronized (channelMapMonitor) {
        CHANNEL_STATS.put(client, null == host ? dummyContext : host);
      }
    }

    /**
     * This method can always dump the latest state of the channel, because the "toString" method
     * of socketChannel has internal locking !
     */
    @Override public String cleanThenDumpActiveChannels() {
      StringBuilder sb = new StringBuilder();
      synchronized (channelMapMonitor) {
        Iterator<Map.Entry<SelectionKeyContext, SelectionKeyContext>> iterator =
            CHANNEL_STATS.entrySet().iterator();
        while (iterator.hasNext()) {
          Map.Entry<SelectionKeyContext, SelectionKeyContext> next = iterator.next();
          SelectionKeyContext clientKeyContext = next.getKey();
          SelectionKeyContext hostKeyContext = next.getValue();
          if (clientKeyContext.isIOClosed() && hostKeyContext.isIOClosed()) {
            iterator.remove();
          } else {
            String line = String.format(
                "%s -> %s\n", SelectionKeyUtils.getSocketChannel(clientKeyContext.getKey()),
                dummyContext.equals(hostKeyContext) ? "un-register" :
                SelectionKeyUtils.getSocketChannel(hostKeyContext.getKey()));
            sb.append(line.replaceAll("java\\.nio\\.channels\\.SocketChannel", ""));
          }
        }
      }
      return sb.toString();
    }
  }
}
