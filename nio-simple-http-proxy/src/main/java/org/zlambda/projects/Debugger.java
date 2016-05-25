package org.zlambda.projects;

import org.zlambda.projects.context.SelectionKeyContext;

public interface Debugger {
  void collectChannelPair(SelectionKeyContext client, SelectionKeyContext host);
  String dumpCurrentChannelStats();
  void cleanupClosedChannelPair();
}
