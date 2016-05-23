package org.zlambda.projects.context;

import org.slf4j.Logger;
import org.zlambda.projects.buffer.ProxyConnectionBuffer;
import org.zlambda.projects.utils.Common;

import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Map;

/**
 * Context share between client socket channel and host socket channel,
 * it is not thread-safe, but it is confined to be used within one thread. Since the pair
 * client socket channel and host socket channel are handled within the same worker thread
 */
public class ShareContext {
  private static final Logger LOGGER = Common.getSystemLogger();

  private final ProxyConnectionBuffer buffer;
  private boolean isHttps = false;
  private SelectionKey clientKey = null;
  private SelectionKey hostKey = null;
  private final Map<SelectionKey, SelectionKeyContext> keyContextMap = new HashMap<>();

  public ShareContext(ProxyConnectionBuffer buffer) {
    this.buffer = buffer;
  }

  public SelectionKey getClientKey() {
    return clientKey;
  }

  public SelectionKey getHostKey() {
    return hostKey;
  }

  public SelectionKeyContext getKeyContext(SelectionKey key) {
    return keyContextMap.get(key);
  }

  public SelectionKeyContext getClientKeyContext() {
    return getKeyContext(clientKey);
  }

  public SelectionKeyContext getHostKeyContext() {
    return getKeyContext(hostKey);
  }

  public void setClientKey(SelectionKey key, SelectionKeyContext context) {
    this.clientKey = key;
    keyContextMap.put(key, context);
  }

  public void setHostKey(SelectionKey key, SelectionKeyContext context) {
    this.hostKey = key;
    keyContextMap.put(key, context);
  }

  public void markHttps() {
    this.isHttps = true;
  }

  public boolean isHttps() {
    return isHttps;
  }

  public ProxyConnectionBuffer getBuffer() {
    return buffer;
  }
}
