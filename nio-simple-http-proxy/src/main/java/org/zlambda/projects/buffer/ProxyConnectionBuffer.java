package org.zlambda.projects.buffer;

public class ProxyConnectionBuffer {
  private final ChannelBuffer downstream, upstream;

  public ProxyConnectionBuffer() {
    this.downstream = new SimpleChannelBuffer();
    this.upstream = new SimpleChannelBuffer();
  }

  public ChannelBuffer getDownstream() {
    return downstream;
  }

  public ChannelBuffer getUpstream() {
    return upstream;
  }
}
