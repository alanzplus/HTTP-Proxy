package org.zlambda.projects;

import java.nio.channels.SelectionKey;

public interface EventHandler {
  void execute(SelectionKey key);
}
