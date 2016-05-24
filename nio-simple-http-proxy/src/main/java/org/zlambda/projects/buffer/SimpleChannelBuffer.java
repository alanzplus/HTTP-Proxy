package org.zlambda.projects.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SimpleChannelBuffer implements ChannelBuffer {
  private static final int KB = 1024;
  private final ByteBuffer internal;

  public SimpleChannelBuffer(int size) {
    this.internal = ByteBuffer.allocate(size * KB);
  }

  /**
   * Invariant:
   * 0 index should point to the first available byte in the buffer
   * internal.position() should points to first free space
   */
  @Override public int read(SocketChannel channel) throws IOException {
    return channel.read(internal);
  }

  /**
   * Invariant:
   * 0 index should point to the first available byte in the buffer
   * internal.position() should points to first free space
   */
  @Override public int write(SocketChannel channel) throws IOException {
    internal.flip();
    int ret = channel.write(internal);
    internal.compact();
    return ret;
  }

  /**
   * Invariant:
   * 0 index should point to the first available byte in the buffer
   * internal.position() should points to first free space
   */
  @Override public void put(byte[] bytes) {
    internal.put(bytes);
  }

  /**
   * Invariant: @{code internal}'s state should not be modified
   */
  @Override public InputStream toViewInputStream() {
    return new InputStream() {
      int limit = internal.position();
      int i = 0;

      @Override
      public int read() throws IOException {
        if (i < limit) {
          return internal.array()[i++];
        } else {
          return -1;
        }
      }
    };
  }

  /**
   * Invariant:
   * 0 index should point to the first available byte in the buffer
   * internal.position() should points to first free space
   */
  @Override public int size() {
    return internal.position(); // it is obvious from the invariant
  }

  @Override public boolean empty() {
    return size() == 0;
  }

  @Override
  public void clear() {
    internal.clear();
  }
}

