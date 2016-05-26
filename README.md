Simple Http Forward Proxy
--
This project contains Java implementation of HTTP/HTTPS (Tunneling over HTTP) Forward Proxy.

Requires JDK 1.8 or higher.

### N.I.O Based

#### overview
`nio-simple-http-proxy` contains the NIO based implementaion of Forward Proxy, which is robust, memory efficient and support high concurrent connections.

Script to start up the proxy application. For detail configuration of the proxy, please look at the run script.

```bash
cd nio-simple-http-proxy
export JAVA_HOME=path
./run.sh
```
#### TODO

##### Use direct byte buffer

* most of time, we don't need to process the buffer data, what we do just read from socket channel and write to another socket channel, so there is no need to copy it into the heap space
* Allocation and deallocation of direct bytebuffer are so more expensive than the heap-resident buffer, so it may be good to create a directy buffer pool

##### Create Galting pefr-test program
