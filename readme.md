# Code your own http reverse-proxy with akka-http in 45 minutes. Challenge accepted !

* version 0: basic reverse proxy
* version 1: change Host header. Add forwarded headers
* version 2: round robin load balancing
* version 3: weighted round robin load balancing
* version 4: circuit breaker on network errors
* version 5: retry calls on network errors
* version 6: http/2 support
* version 7: runtime changes from ???

## Helpers

for http2

```sh
sbt run
curl2 -k -H 'Host: test.foo.bar' https://127.0.0.1:8443 --include
curl2 -k -v -H 'Host: test.foo.bar' https://127.0.0.1:8443 --include
```