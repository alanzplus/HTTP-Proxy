package org.zlambda.projects.buffer;

public class ProxyConnectionBuffer {
  private final ChannelBuffer downstream, upstream;

  public ProxyConnectionBuffer(int size) {
    this.downstream = new SimpleChannelBuffer(size);
    this.upstream = new SimpleChannelBuffer(size);
  }

  public ChannelBuffer getDownstream() {
    return downstream;
  }

  public ChannelBuffer getUpstream() {
    return upstream;
  }
}
