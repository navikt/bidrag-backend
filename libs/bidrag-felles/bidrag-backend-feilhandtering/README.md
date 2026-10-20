## Bidrag-feilhåndtering

### MDC

### Logging

### Kafka
`no.nav.bidrag.kafka.KafkaRetryListener` kan kobles til en Spring Kafka
`DefaultErrorHandler` med `setRetryListeners`. 
En egen feilhåndterer som stopper containeren, kan bruke `logFailure(record, exception)`
uten å oppgi et forsøksnummer som den ikke kjenner.

Listeneren styrer ikke ventetiden mellom forsøk. Appene med `DefaultErrorHandler`
bør sette en back-off grense på under standardverdien på fem
minutter for `max.poll.interval.ms`. Lang behandlingstid kan fortsatt
overskride denne grensen.

### Feilmeldinger
