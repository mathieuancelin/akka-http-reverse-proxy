# Code your own http reverse-proxy with akka-http in 45 minutes. Challenge accepted !

* [version 0: basic reverse proxy](https://github.com/mathieuancelin/akka-http-reverse-proxy/blob/master/src/main/scala/steps/proxy0.scala)
* [version 1: change Host header. Add forwarded headers](https://github.com/mathieuancelin/akka-http-reverse-proxy/blob/master/src/main/scala/steps/proxy1.scala)
* [version 2: round robin load balancing](https://github.com/mathieuancelin/akka-http-reverse-proxy/blob/master/src/main/scala/steps/proxy2.scala)
* [version 3: weighted round robin load balancing](https://github.com/mathieuancelin/akka-http-reverse-proxy/blob/master/src/main/scala/steps/proxy3.scala)
* [version 4: circuit breaker on network errors](https://github.com/mathieuancelin/akka-http-reverse-proxy/blob/master/src/main/scala/steps/proxy4.scala)
* [version 5: retry calls on network errors](https://github.com/mathieuancelin/akka-http-reverse-proxy/blob/master/src/main/scala/steps/proxy5.scala)
* [version 6: runtime changes from admin api](https://github.com/mathieuancelin/akka-http-reverse-proxy/blob/master/src/main/scala/steps/proxy6.scala)
* [version 7: http/2 support](https://github.com/mathieuancelin/akka-http-reverse-proxy/blob/master/src/main/scala/steps/proxy7.scala)

## Helpers

for http2

```sh
sbt run
curl2 -k -H 'Host: test.foo.bar' https://127.0.0.1:8443 --include
curl2 -k --http2-H 'Host: test.foo.bar' https://127.0.0.1:8443 --include
curl2 -k -v -H 'Host: test.foo.bar' https://127.0.0.1:8443 --include
```

```sh
wrk -t2 -c200 -d60s -H "Host: test.foo.bar" --latency http://127.0.0.1:8080/
```