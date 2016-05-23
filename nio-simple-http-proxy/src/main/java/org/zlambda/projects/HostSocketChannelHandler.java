package org.zlambda.projects;

import org.slf4j.Logger;
import org.zlambda.projects.context.SelectionKeyContext;
import org.zlambda.projects.context.ShareContext;
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
    state = state.perform(context, selectionKey);
    SelectionKeyContext hostKeyContext = context.getHostKeyContext();
    if (hostKeyContext.isIOClosed()) {
      hostKeyContext.closeIO();
    }
  }

  private interface HandlerStateAction {
    HandlerState perform(ShareContext context, SelectionKey key);
  }


  private enum HandlerState implements HandlerStateAction {
    WAIT_FOR_CONNECTION {
      @Override
      public HandlerState perform(ShareContext context, SelectionKey key) {
        if (!key.isConnectable()) {
          return WAIT_FOR_CONNECTION;
        }
        SelectionKeyContext hostKeyContext = context.getHostKeyContext();
        SelectionKeyContext clientKeyContext = context.getClientKeyContext();
        SocketChannel channel = SelectionKeyUtils.getSocketChannel(key);
        try {
          channel.finishConnect();
        } catch (IOException e) {
          LOGGER.error("Host channel <{}> failed to connect.", e);
          hostKeyContext.closeIO();
          return WAIT_FOR_CONNECTION;
        }
        /**
         * No need to check whether client IO state, since all these will be handled in bridging state
         */
        hostKeyContext.setConnectState(true);
        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        if (context.isHttps() && !clientKeyContext.isIOClosed()) {
          context.getBuffer()
                 .getDownstream()
                 .put("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
          SelectionKeyUtils.addInterestOps(context.getClientKey(), SelectionKey.OP_WRITE);
        }
        return BRIDGING;
      }
    },
    BRIDGING {
      @Override
      public HandlerState perform(ShareContext context, SelectionKey key) {
        SocketChannel channel = SelectionKeyUtils.getSocketChannel(key);
        SelectionKeyContext hostKeyContext = context.getHostKeyContext();
        SelectionKeyContext clientKeyContext = context.getClientKeyContext();
        /**
         * Client < -- OS -- [Proxy <-- IS -- Host]
         */
        if (key.isReadable()) {
          if (clientKeyContext.isOSClosed()) {
            LOGGER.debug("Client channel <{}> output stream is closed.",
                         clientKeyContext.getName());
            hostKeyContext.closeIS();
          } else {
            if (-1 ==
                SocketChannelUtils.readFromChannel(channel, context.getBuffer().getDownstream())) {
              hostKeyContext.closeIS();
              if (!clientKeyContext.isOSClosed()) {
                SelectionKeyUtils.addInterestOps(context.getClientKey(), SelectionKey.OP_WRITE);
              }
            } else if (!context.getBuffer().getDownstream().empty() &&
                       !clientKeyContext.isOSClosed()) {
              SelectionKeyUtils.addInterestOps(context.getClientKey(), SelectionKey.OP_WRITE);
            }
          }
        }

        /**
         * Client  -- IS --> [Proxy -- OS --> Host]
         */
        if (key.isWritable()) {
          if (clientKeyContext.isISClosed() && context.getBuffer().getUpstream().empty()) {
            LOGGER.debug("Client channel <{}> input stream is closed and upstream buffer is empty.",
                         clientKeyContext.getName());
            hostKeyContext.closeOS();
          } else {
            if (context.getBuffer().getUpstream().empty()) {
              SelectionKeyUtils.removeInterestOps(key, SelectionKey.OP_WRITE);
            } else if (-1 ==
                       SocketChannelUtils.writeToChannel(channel,
                                                         context.getBuffer().getUpstream())) {
              hostKeyContext.closeOS();
              clientKeyContext.closeIO();
            }
          }
        }
        return BRIDGING;
      }
    }
  }
}
