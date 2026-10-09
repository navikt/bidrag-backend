## Bidrag-feilhåndtering

### MDC

### Logging

### Kafka
`no.nav.bidrag.kafka.KafkaRetryListener` kan kobles til en Spring Kafka
`DefaultErrorHandler` med `setRetryListeners`. 
En egen feilhåndterer som stopper containeren, kan bruke `logFailure(record, exception)`
uten å oppgi et forsøksnummer som den ikke kjenner.

Listeneren styrer ikke ventetiden mellom forsøk. Ved lang backoff må appens
`DefaultErrorHandler` også konfigureres til å pause consumeren mens den
fortsetter å polle, slik at `max.poll.interval.ms` ikke overskrides.

### Feilmeldinger
