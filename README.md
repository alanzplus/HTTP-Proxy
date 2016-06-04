HTTP/HTTPS Forward Proxy
------------------------
This project contains Java implementation of HTTP/HTTPS (Tunneling over HTTP) Forward Proxy.

Requires JDK 1.8 or higher.

### N.I.O Based

#### overview
`nio-http-proxy` contains the NIO based implementation of HTTP/HTTPS Forward Proxy, which is robust and cpu-memory efficient.

#### quick start

`run.sh` will automatically compile, package and start the proxy.

```bash
cd nio-http-proxy
export JAVA_HOME=path_to_java8
./run.sh
```

#### configuration

Here is list of proxy configurations. To change the configuration, simple modify the `run.sh`.

##### basic

```
-Dport=9999   # proxy port
-Dworker=8    # the proxy application use a fixed worker pool, this option specifies the number of workers
```

##### monitor

Monitor is just for debug usage, which periodically dumps the proxy status like active connections, number of used buffers. By default the monitor thread is enable, but we can disable it.

```
-DenableMonitor=true
-DmonitorUpdateInterval=30 # unit second
```

##### buffer pool 

The proxy application, by default, uses a off-heap buffer pool. When it is disable, the proxy will use on-heap buffer without pooling, which means the buffer is managed by JVM.

```
-DuseDirectBuffer=true
-DminNumBuffers=100
-DmaxNumBuffers=200
-DbufferSize=10 # unit KB, each client <-> proxy <-> host connection use two buffers
```

#### TODO
* use yaml or json for proxy configuration instead of the java options
* support HTTP traffic rewrite using a plugin-like way
  * no need to modify the original source code
  * the rewrite class just need to implement an interface and configure the proxy to use it
* Create Galting pefr-test program
