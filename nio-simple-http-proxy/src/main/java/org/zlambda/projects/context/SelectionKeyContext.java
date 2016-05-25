package org.zlambda.projects.context;

import org.slf4j.Logger;
import org.zlambda.projects.utils.Common;
import org.zlambda.projects.utils.SelectionKeyUtils;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class SelectionKeyContext {
  private static final Logger LOGGER = Common.getSystemLogger();

  public static class ChannelState {
    boolean isISClosed = false;
    boolean isOSClosed = false;
    boolean isConnected = false;

    public ChannelState() {
      setConnectState(false);
    }

    public ChannelState setConnectState(boolean isConnected) {
      this.isConnected = isConnected;
      this.isISClosed = !isConnected;
      this.isOSClosed = !isConnected;
      return this;
    }
  }

  private final String name;
  private final ChannelState channelState;
  private final SelectionKey key;

  private SelectionKeyContext(Builder builder) {
    this.name = builder.name;
    this.channelState = builder.channelState;
    this.key = builder.key;
  }

  public SelectionKey getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public boolean isISClosed() {
    return channelState.isISClosed;
  }

  public boolean isOSClosed() {
    return channelState.isOSClosed;
  }

  public boolean isIOClosed() {
    return isISClosed() && isOSClosed();
  }

  public boolean isConnected() {
    return channelState.isConnected;
  }

  public void setConnectState(boolean isConnected) {
    channelState.setConnectState(isConnected);
  }

  public void closeIS() {
    if (isISClosed()) {
      return;
    }
    SocketChannel socketChannel = SelectionKeyUtils.getSocketChannel(key);
    try {
      socketChannel.shutdownInput();
      SelectionKeyUtils.removeInterestOps(key, SelectionKey.OP_READ);
      channelState.isISClosed = true;
    } catch (IOException e) {
      LOGGER.error("failed to shutdown input of socket channel <{}>", name, e);
    }
  }

  public void closeOS() {
    if (isOSClosed()) {
      return;
    }
    SocketChannel socketChannel = SelectionKeyUtils.getSocketChannel(key);
    try {
      socketChannel.shutdownOutput();
      SelectionKeyUtils.removeInterestOps(key, SelectionKey.OP_WRITE);
      channelState.isOSClosed = true;
    } catch (IOException e) {
      LOGGER.error("failed to shutdown output of socket channel <{}>", name, e);
    }
  }

  public void closeIO() {
    closeIS();
    closeOS();
    try {
      SocketChannel socketChannel = SelectionKeyUtils.getSocketChannel(key);
      socketChannel.close();
      channelState.setConnectState(false);
      LOGGER.debug("close socket channel <{}>.", name);
    } catch (IOException e) {
      LOGGER.error("failed to close socket channel <{}>", name, e);
    }
  }

  public void cleanupIO() {
    if (isConnected() && isIOClosed()) {
      closeIO();
    }
  }

  public static void cleanup(SelectionKeyContext context) {
    if (null == context) {
      return;
    }
    context.cleanupIO();
  }

  public static class Builder {
    private String name;
    private ChannelState channelState;
    private final SelectionKey key;

    public Builder(SelectionKey key) {
      this.key = key;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder channelState(ChannelState state) {
      channelState = state;
      return this;
    }

    public SelectionKeyContext build() {
      return new SelectionKeyContext(this);
    }
  }
}
