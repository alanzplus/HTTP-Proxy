package org.zlambda.projects;

import org.slf4j.Logger;
import org.zlambda.projects.buffer.ChannelBuffer;
import org.zlambda.projects.context.SelectionKeyContext;
import org.zlambda.projects.context.ShareContext;
import org.zlambda.projects.utils.Common;
import org.zlambda.projects.utils.SelectionKeyUtils;
import org.zlambda.projects.utils.SocketChannelUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class ClientSocketChannelHandler implements EventHandler {
  private static final Logger LOGGER = Common.getSystemLogger();
  private HandlerState state = HandlerState.PARSING_INITIAL_REQUEST;
  private final ShareContext context;

  public ClientSocketChannelHandler(ShareContext context) {
    this.context = context;
  }

  @Override
  public void execute(SelectionKey selectionKey) {
    state = state.perform(context, selectionKey);
    SelectionKeyContext clientKeyContext = context.getClientKeyContext();
    if (clientKeyContext.isIOClosed()) {
      clientKeyContext.closeIO();
    }
  }

  private interface HandlerStateAction {
    HandlerState perform(ShareContext context, SelectionKey key);
  }


  private enum HandlerState implements HandlerStateAction {
    PARSING_INITIAL_REQUEST {
      @Override
      public HandlerState perform(ShareContext context, SelectionKey key) {
        if (!key.isReadable()) {
          return PARSING_INITIAL_REQUEST;
        }
        ChannelBuffer upstreamBuffer = context.getBuffer().getUpstream();
        SocketChannel channel = SelectionKeyUtils.getSocketChannel(key);
        if (-1 == SocketChannelUtils.readFromChannel(channel, upstreamBuffer)) {
          context.getClientKeyContext().closeIO();
          return PARSING_INITIAL_REQUEST;
        }
        Optional<List<String>> header =
            parseInitialRequestHeader(upstreamBuffer.toViewInputStream());
        if (!header.isPresent()) {
          return PARSING_INITIAL_REQUEST;
        }
        LOGGER.info("got request line <{}>.", header.get().get(0));
        Optional<RequestLine> requestLine = RequestLine.construct(header.get().get(0));
        if (!requestLine.isPresent()) {
          LOGGER.error("cannot parse request line, so close client socket channel <{}>",
                       SocketChannelUtils.getRemoteAddress(channel));
          context.getClientKeyContext().closeIO();
          return PARSING_INITIAL_REQUEST;
        }
        /**
         * for https request, discard the initial request
         */
        if (requestLine.get().isHttps) {
          upstreamBuffer.clear();
        }
        if (!createHostAndRegisterChannel(requestLine.get(), key, context)) {
          context.getClientKeyContext().closeIO();
          return PARSING_INITIAL_REQUEST;
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
         * [Client -- IS --> Proxy] -- OS --> Host
         */
        if (key.isReadable()) {
          if (hostKeyContext.isConnected() && hostKeyContext.isOSClosed()) {
            LOGGER.debug(
                "Host socket channel <{}> output stream is closed, so close Client socket channel <{}> input stream",
                hostKeyContext.getName(), clientKeyContext.getName());
            clientKeyContext.closeIS();
          } else {
            if (-1 ==
                SocketChannelUtils.readFromChannel(channel, context.getBuffer().getUpstream())) {
              clientKeyContext.closeIS();
              if (hostKeyContext.isConnected() && !hostKeyContext.isOSClosed()) {
                SelectionKeyUtils.addInterestOps(context.getHostKey(), SelectionKey.OP_WRITE);
              }
            } else if (!context.getBuffer().getUpstream().empty() &&
                       hostKeyContext.isConnected() &&
                       !hostKeyContext.isOSClosed()) {
              SelectionKeyUtils.addInterestOps(context.getHostKey(), SelectionKey.OP_WRITE);
            }
          }
        }

        /**
         * [Client <-- OS -- Proxy] < -- IS -- Host
         */
        if (key.isWritable()) {
          if (hostKeyContext.isConnected() &&
              hostKeyContext.isISClosed() &&
              context.getBuffer().getDownstream().empty()) {
            LOGGER.debug(
                "Host socket channel <{}> input stream is closed and downstream buffer is empty, so close Client <{}> output stream ",
                hostKeyContext.getName(), clientKeyContext.getName());
            clientKeyContext.closeOS();
          } else {
            if (context.getBuffer().getDownstream().empty()) {
              SelectionKeyUtils.removeInterestOps(key, SelectionKey.OP_WRITE);
            } else if (-1 == SocketChannelUtils.writeToChannel(
                channel, context.getBuffer().getDownstream())) {
              clientKeyContext.closeOS();
              if (hostKeyContext.isConnected()) {
                hostKeyContext.closeIS();
              }
            }
          }
        }
        return BRIDGING;
      }
    },;


    /**
     * Reference https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html
     */
    private static class RequestLine {
      final String method;
      final String uri;
      final String version;
      final boolean isHttps;

      private RequestLine(String method, String uri, String version) {
        this.method = method;
        this.uri = uri;
        this.version = version;
        this.isHttps = method.matches("(?i)connect");
      }

      static Optional<RequestLine> construct(String requestLine) {
        String[] split = requestLine.split("\\s+", 3);
        return (3 == split.length) ? Optional.of(
            new RequestLine(split[0], split[1], split[2])) : Optional.empty();
      }
    }

    private static Optional<List<String>>
    parseInitialRequestHeader(InputStream inputStream) {
      Scanner scanner = new Scanner(inputStream, "utf-8");
      List<String> ret = new ArrayList<>();
      while (scanner.hasNextLine()) {
        ret.add(scanner.nextLine());
      }
      if (ret.isEmpty() || !ret.get(ret.size() - 1).equals("")) {
        return Optional.empty();
      } else {
        return Optional.of(ret);
      }
    }

    private static boolean
    createHostAndRegisterChannel(RequestLine line, SelectionKey key, ShareContext context) {
      String uri = line.uri;
      /**
       * Java URL cannot parse uri without protocol
       */
      if (line.isHttps && !uri.matches("^(https|http).*")) {
        uri = "http://" + uri;
        context.markHttps();
      }

      try {
        InetSocketAddress inetSocketAddress = constructInetSocketAddress(new URL(uri));
        SocketChannel hostSocketChannel = SocketChannel.open();
        hostSocketChannel.configureBlocking(false);
        HostSocketChannelHandler handler = new HostSocketChannelHandler(context);
        SelectionKey hostKey = hostSocketChannel.register(key.selector(), SelectionKey.OP_CONNECT,
                                                          handler);
        SelectionKeyContext hostKeyContext = new SelectionKeyContext
            .Builder(hostKey)
            .name(inetSocketAddress.toString())
            .channelState(new SelectionKeyContext.ChannelState(false))
            .build();
        context.setHostKey(hostKey, hostKeyContext);
        hostSocketChannel.connect(inetSocketAddress);
        return true;
      } catch (MalformedURLException e) {
        LOGGER.error("cannot parse URL <{}>.", uri, e);
      } catch (IOException e) {
        LOGGER.error("cannot create or register host socket channel", e);
      }
      return false;
    }

    private static InetSocketAddress constructInetSocketAddress(URL url) {
      int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
      return new InetSocketAddress(url.getHost(), port);
    }
  }

}
