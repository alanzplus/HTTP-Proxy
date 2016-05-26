package org.zlambda.projects.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SocketChannel;

public interface ChannelBuffer {
  /**
   * [Channel -> Buffer]
   * Read from channel and write to buffer
   *
   * @param channel
   * @return
   * @throws IOException
   */
  int read(SocketChannel channel) throws IOException;

  /**
   * [Buffer -> Channel]
   * Read from buffer and write to buffer
   *
   * @param channel
   * @return
   * @throws IOException
   */
  int write(SocketChannel channel) throws IOException;

  /**
   * Put {code bytes} into buffer
   *
   * @param bytes
   */
  void put(byte[] bytes);

  /**
   * Create a "view" inputstream, which means the read operation of the inpustream will not modify
   * the actual pointer of the internal buffer
   *
   * @return
   */
  InputStream toViewInputStream();

  /**
   * return the size of unconsumed data
   */
  int size();

  boolean empty();

  void clear();

  void free();
}

