package org.zlambda.projects.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zlambda.projects.SimpleHttpProxy;

import java.io.Closeable;
import java.io.IOException;

public enum Common {
  ;

  private static final Logger systemLogger = LoggerFactory.getLogger(SimpleHttpProxy.class);

  public static Logger getSystemLogger() {
    return systemLogger;
  }

  @SuppressWarnings("unchecked")
  public static <Super, Sub extends Super> Sub downCast(Super sp) {
    return (Sub) sp;
  }

  public static void close(Closeable closeable, String name) {
    if (null == closeable) {
      return;
    }
    try {
      closeable.close();
    } catch (IOException e) {
      String msg = null == name ? "failed to close" : "failed to close <" + name + ">";
      getSystemLogger().error("{}", msg, e);
    }
  }

  public static void close(Closeable closeable) {
    close(closeable, null);
  }
}
