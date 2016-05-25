package org.zlambda.projects;

import org.slf4j.Logger;
import org.zlambda.projects.buffer.ChannelBuffer;
import org.zlambda.projects.context.SelectionKeyContext;
import org.zlambda.projects.context.ShareContext;
import org.zlambda.projects.context.SystemContext;
import org.zlambda.projects.utils.Common;
import org.zlambda.projects.utils.SelectionKeyUtils;
import org.zlambda.projects.utils.SocketChannelUtils;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class HostSocketChannelHandler implements EventHandler {
  private static final Logger LOGGER = Common.getSystemLogger();
  private final ShareContext context;

  private HandlerState state = HandlerState.WAIT_FOR_CONNECTION;

  public HostSocketChannelHandler(ShareContext context) {
    this.context = context;
  }

  @Override
  public void execute(SelectionKey selectionKey) {
    state = state.perform(context);
    SelectionKeyContext.cleanup(context.getClientKeyContext());
    SelectionKeyContext.cleanup(context.getHostKeyContext());
  }

  private enum HandlerState {
    WAIT_FOR_CONNECTION {
      @Override
      public HandlerState perform(ShareContext context) {
        SelectionKeyContext hostKeyContext = context.getHostKeyContext();
        SelectionKeyContext clientKeyContext = context.getClientKeyContext();
        if (!hostKeyContext.getKey().isConnectable()) {
          return WAIT_FOR_CONNECTION;
        }
        SocketChannel channel = SelectionKeyUtils.getSocketChannel(hostKeyContext.getKey());
        try {
          channel.finishConnect();
        } catch (IOException e) {
          LOGGER.error("Host channel <{}> failed to connect, reason: {}.",
                       hostKeyContext.getName(), e.getMessage(), e);
          clientKeyContext.closeIO();
          hostKeyContext.closeIO();
          return WAIT_FOR_CONNECTION;
        }
        hostKeyContext.setConnectState(true);
        hostKeyContext.getKey().interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        clientKeyContext.getKey().interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        if (context.isHttps()) {
          context.getBuffer()
                 .getDownstream()
                 .put("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
        }
        SystemContext.getSystemDebugger().collectChannelPair(
            clientKeyContext,
            hostKeyContext
        );
        return BRIDGING;
      }
    },
    BRIDGING {
      @Override
      public HandlerState perform(ShareContext context) {
        SelectionKeyContext hostKeyContext = context.getHostKeyContext();
        SelectionKeyContext clientKeyContext = context.getClientKeyContext();
        SelectionKey key = hostKeyContext.getKey();
        SocketChannel channel = SelectionKeyUtils.getSocketChannel(key);
        /**
         * Client < -- OS -- [Proxy <-- IS -- Host]
         */
        if (key.isReadable()) {
          if (clientKeyContext.isOSClosed()) {
            LOGGER.debug("Client socket channel <{}> output stream is closed, so close Host socket channel <{}> input stream",
                         clientKeyContext.getName(), hostKeyContext.getName());
            hostKeyContext.closeIS();
          } else {
            ChannelBuffer downstreamBuffer = context.getBuffer().getDownstream();
            if (-1 == SocketChannelUtils.readFromChannel(channel, downstreamBuffer)) {
              hostKeyContext.closeIS();
            }
            /**
             * Read Event always trigger output stream to listen on write event
             */
            SelectionKeyUtils.addInterestOps(context.getClientKey(), SelectionKey.OP_WRITE);
          }
        }

        /**
         * Client  -- IS --> [Proxy -- OS --> Host]
         */
        if (key.isWritable()) {
          if (clientKeyContext.isISClosed() && context.getBuffer().getUpstream().empty()) {
            LOGGER.debug("Client channel <{}> input stream is closed and upstream buffer is empty, so close Host socket channel <{}> output stream",
                         clientKeyContext.getName(), hostKeyContext.getName());
            hostKeyContext.closeOS();
          } else {
            ChannelBuffer upstreamBuffer = context.getBuffer().getUpstream();
            if (upstreamBuffer.empty()) {
              // keep cpu free
              SelectionKeyUtils.removeInterestOps(key, SelectionKey.OP_WRITE);
            } else if (-1 == SocketChannelUtils.writeToChannel(channel, upstreamBuffer)) {
              hostKeyContext.closeOS();
              /**
               * error on output stream should always immediately terminate its corresponding input stream
               */
              clientKeyContext.closeIO();
            }
          }
        }
        return BRIDGING;
      }
    },
    ;
    abstract HandlerState perform(ShareContext context);
  }

}
