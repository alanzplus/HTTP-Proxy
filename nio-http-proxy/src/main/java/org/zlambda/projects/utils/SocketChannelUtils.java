package org.zlambda.projects.utils;

import org.slf4j.Logger;
import org.zlambda.projects.buffer.ChannelBuffer;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public enum SocketChannelUtils {
  ;
  private static final Logger LOGGER = Common.getSystemLogger();

  public static SocketAddress getRemoteAddress(SocketChannel socketChannel) {
    return socketChannel.socket().getRemoteSocketAddress();
  }

  public static int readFromChannel(SocketChannel channel, ChannelBuffer buffer) {
    try {
      int numOfRead = buffer.read(channel);
      if (-1 == numOfRead) {
        LOGGER.debug("reading <{}> got EOF.", getRemoteAddress(channel));
      }
      return numOfRead;
    } catch (IOException e) {
      if ("Connection reset by peer".equals(e.getMessage())) {
        LOGGER.debug("Failed to read from <{}>, reason <{}>.", getRemoteAddress(channel),
                     e.getMessage());
      } else {
        LOGGER.error("Failed to read from <{}>.", getRemoteAddress(channel), e);
      }
      return -1;
    }
  }

  public static int writeToChannel(SocketChannel channel, ChannelBuffer buffer) {
    try {
      return buffer.write(channel);
    } catch (IOException e) {
      if ("Broken pipe".equals(e.getMessage())) {
        LOGGER.debug("Failed to write to <{}>, reason <{}>.", getRemoteAddress(channel),
                     e.getMessage());
      } else {
        LOGGER.error("Failed to write to <{}>.", channel.toString(), e);
      }
      return -1;
    }
  }

  public static String getName(SocketChannel channel) {
    SocketAddress remoteSocketAddress = channel.socket().getRemoteSocketAddress();
    return null == remoteSocketAddress ? "unconnected" : remoteSocketAddress.toString();
  }
}
