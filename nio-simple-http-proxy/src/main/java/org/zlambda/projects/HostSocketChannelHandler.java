package org.zlambda.projects;

import org.slf4j.Logger;
import org.zlambda.projects.buffer.ChannelBuffer;
import org.zlambda.projects.context.ConnectionContext;
import org.zlambda.projects.context.ProxyContext;
import org.zlambda.projects.utils.Common;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class HostSocketChannelHandler implements EventHandler {
  private static final Logger LOGGER = Common.getSystemLogger();
  private final ProxyContext context;

  private HandlerState state = HandlerState.WAIT_FOR_CONNECTION;

  public HostSocketChannelHandler(ProxyContext context) {
    this.context = context;
  }

  @Override
  public void execute(SelectionKey selectionKey) {
    state = state.perform(context);
    context.cleanup();
  }

  private enum HandlerState {
    WAIT_FOR_CONNECTION {
      @Override
      public HandlerState perform(ProxyContext context) {
        ConnectionContext client = context.getClient();
        ConnectionContext host = context.getHost();
        if (!host.isConnectable()) {
          return WAIT_FOR_CONNECTION;
        }
        try {
          host.getChannel().finishConnect();
        } catch (IOException e) {
          LOGGER.error("Host channel <{}> failed to connect, reason: {}.",
                       host.getName(), e.getMessage(), e);
          client.closeIO();
          host.closeIO();
          return WAIT_FOR_CONNECTION;
        }
        host.unregister(SelectionKey.OP_CONNECT);
        host.register(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        client.register(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        if (context.isHttps()) {
          context.getConnectionBuffer().downstream().put(
              "HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
        }
        MonitorSingleton.get().collectChannelPair(client, host);
        return BRIDGING;
      }
    },
    BRIDGING {
      @Override
      public HandlerState perform(ProxyContext context) {
        ConnectionContext client = context.getClient();
        ConnectionContext host = context.getHost();
        /**
         * Client < -- OS -- [Proxy <-- IS -- Host]
         */
        if (host.isReadable()) {
          if (client.isOutputShutdown()) {
            LOGGER.debug(
                "Client socket channel <{}> output stream is closed, so close Host socket channel <{}> input stream",
                client.getName(), host.getName());
            host.shutdownIS();
          } else {
            ChannelBuffer downstream = context.getConnectionBuffer().downstream();
            if (-1 == host.read(downstream)) {
              host.shutdownIS();
            }
            /**
             * Read Event always trigger output stream to listen on write event
             */
            client.register(SelectionKey.OP_WRITE);
          }
        }

        /**
         * Client  -- IS --> [Proxy -- OS --> Host]
         */
        if (host.isWritable()) {
          ChannelBuffer upstream = context.getConnectionBuffer().upstream();
          if (client.isInputShutdown() && upstream.empty()) {
            LOGGER.debug(
                "Client channel <{}> input stream is closed and upstream buffer is empty, so close Host socket channel <{}> output stream",
                client.getName(), host.getName());
            host.shutdownOS();
          } else {
            if (upstream.empty()) {
              // keep cpu free
              host.unregister(SelectionKey.OP_WRITE);
            } else if (-1 == host.write(upstream)) {
              host.shutdownOS();
              /**
               * error on output stream should always immediately terminate its corresponding input stream
               */
              client.shutdownIS();
            }
          }
        }
        return BRIDGING;
      }
    },;

    abstract HandlerState perform(ProxyContext context);
  }

}
