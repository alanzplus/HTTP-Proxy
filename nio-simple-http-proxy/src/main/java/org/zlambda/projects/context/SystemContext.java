package org.zlambda.projects.context;

import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

public class SystemContext {
  private final BlockingQueue<SocketChannel> clientQueue;
  private final int port;
  private final int numWorkers;

  private SystemContext(Builder builder) {
    this.clientQueue = builder.clientQueue;
    this.port = builder.port;
    this.numWorkers = builder.numWorkers;
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

  @Override public String toString() {
    return String.format(
        "[" +
        "port : %d," +
        "number of workers: %d" +
        "]",
        getPort(), getNumWorkers()
    );
  }

  public static class Builder {
    private BlockingQueue<SocketChannel> clientQueue;
    private int port;
    private int numWorkers;

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

    public SystemContext build() {
      return new SystemContext(this);
    }
  }
}
