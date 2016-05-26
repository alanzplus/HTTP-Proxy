package org.zlambda.projects.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class DirectChannelBuffer implements ChannelBuffer {
  private final ByteBuffer internal;
  private boolean isFree = false;
  private final DirectBufferPool pool;

  public DirectChannelBuffer(DirectBufferPool bufferPool) {
    this.isFree = false;
    this.pool = bufferPool;
    this.internal = bufferPool.take();
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
    return new IncrementalInputStream(internal.asReadOnlyBuffer());
  }

  /**
   * this input stream will not read everything in the byte buffer
   * into the on heap cache. Instead, it will read it CACHE_SIZE each time
   * when there is not enough data in the on heap cache.
   */
  private static class IncrementalInputStream extends InputStream {
    private static int CACHE_SIZE = 1024;
    private final byte[] onHeapCache = new byte[CACHE_SIZE];
    private final ByteBuffer byteBuffer;
    private int currentLimit;
    private int i;
    IncrementalInputStream(ByteBuffer readOnlyByteBuffer) {
      readOnlyByteBuffer.flip();
      this.byteBuffer = readOnlyByteBuffer;
      this.currentLimit = 0;
      this.i = 0;
    }

    @Override public int read() throws IOException {
      if (i == currentLimit) {
        this.currentLimit = Math.min(byteBuffer.remaining(), CACHE_SIZE);
        if (currentLimit == 0) {
          return -1;
        }
        byteBuffer.get(onHeapCache, 0, currentLimit);
        i = 0;
      }
      return onHeapCache[i++];
    }
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

  @Override public void free() {
    if (!isFree) {
      pool.release(internal);
    }
  }
}
