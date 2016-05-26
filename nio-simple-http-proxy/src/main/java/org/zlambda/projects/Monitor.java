package org.zlambda.projects;

import org.zlambda.projects.context.ConnectionContext;

public interface Monitor {
  void collectChannelPair(ConnectionContext client, ConnectionContext host);

  String dumpStats();
}
