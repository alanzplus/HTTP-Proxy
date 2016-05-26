package org.zlambda.projects.buffer;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.zlambda.projects.utils.Common;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Threadsafe Class
 *
 * Refers to https://github.com/midonet/midonet/blob/master/netlink/src/main/java/org/midonet/netlink/BufferPool.java
 *
 * This implementation removes the checking ownership checking of byte buffer when it is released
 */
public class DirectBufferPool {
  private static final Logger LOGGER = Common.getSystemLogger();
  private final int minNumBuffers;
  private final int maxNumBuffers;
  private final int bufferSize;
  private final BlockingQueue<ByteBuffer> pool;
  private final AtomicInteger numBuffers;

  public DirectBufferPool(int minNumBuffers, int maxNumBuffers, int bufferSize) {
    Preconditions.checkArgument(
        minNumBuffers < maxNumBuffers,
        "minNumBuffers should not greater than maxNumBuffers"
    );
    Preconditions.checkArgument(
        minNumBuffers > 0 && maxNumBuffers > 0 && bufferSize > 0,
        "minNumBuffers, maxNumBuffers, bufferSize should > 0"
    );
    this.minNumBuffers = minNumBuffers;
    this.maxNumBuffers = maxNumBuffers;
    this.bufferSize = bufferSize;
    this.pool = new ArrayBlockingQueue<>(maxNumBuffers);
    this.numBuffers = new AtomicInteger(0);
    init();
  }

  private void init() {
    while (numBuffers.getAndIncrement() < minNumBuffers) {
      pool.offer(ByteBuffer.allocate(bufferSize));
    }
  }

  public ByteBuffer take() {
    ByteBuffer byteBuffer = pool.poll();
    if (null != byteBuffer) {
      return byteBuffer;
    }
    if (numBuffers.incrementAndGet() <= maxNumBuffers) {
      return ByteBuffer.allocateDirect(bufferSize);
    } else {
      numBuffers.decrementAndGet();
      return ByteBuffer.allocate(bufferSize);
    }
  }

  public void release(ByteBuffer byteBuffer) {
    if (null == byteBuffer || !byteBuffer.isDirect()) {
      return;
    }
    pool.offer(byteBuffer);
  }
}
