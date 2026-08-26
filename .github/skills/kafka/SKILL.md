---
name: kafka
description: Rapids & Rivers, eventdrevet arkitektur, Kafka-mønstre og schema-design for Nav-applikasjoner
license: MIT
compatibility: Kotlin/JVM application with Kafka on Nais
metadata:
  domain: backend
  tags: kafka events event-driven nais
---

# Kafka & Rapids & Rivers Skill

Mønstre, maler og prosedyrer for å bygge eventdrevne systemer med Kafka på Nais. Dekker Rapids & Rivers-rammeverket, event-schema-design og consumer/producer-mønstre.

## Når skal denne brukes

- Sette opp Kafka i en Nais-applikasjon
- Implementere en Rapids & Rivers-consumer (River)
- Designe event-schemas
- Teste eventdrevet kode med TestRapid
- Feilsøke Kafka-tilkobling eller consumer lag

## Kommandoer

```bash
# Sjekk at Kafka-miljøvariabler er tilstede (kun navn, ikke verdier)
kubectl exec -it <pod> -n <namespace> -- env | grep -o '^KAFKA[^=]*'

# Verifiser at Kafka-credentials er montert
kubectl exec -it <pod> -n <namespace> -- ls -la /var/run/secrets/nais.io/kafka/

# Se pod-logger for Kafka-events
kubectl logs -n <namespace> <pod> --tail=50 | grep -i "event\|kafka\|river"
```

## Sette opp Kafka

### Nais-manifest

```yaml
apiVersion: nais.io/v1alpha1
kind: Application
metadata:
  name: my-app
spec:
  kafka:
    pool: nav-dev # eller nav-prod
```

Dette gjør automatisk:

- Oppretter Kafka-credentials
- Monterer credentials i `/var/run/secrets/nais.io/kafka/`
- Gir miljøvariabler

### Konfigurasjon

```kotlin
// Miljøvariabler satt automatisk av Nais
val kafkaConfig = mapOf(
    "KAFKA_BROKERS" to System.getenv("KAFKA_BROKERS"),
    "KAFKA_TRUSTSTORE_PATH" to System.getenv("KAFKA_TRUSTSTORE_PATH"),
    "KAFKA_CREDSTORE_PASSWORD" to System.getenv("KAFKA_CREDSTORE_PASSWORD"),
    "KAFKA_KEYSTORE_PATH" to System.getenv("KAFKA_KEYSTORE_PATH"),
    "KAFKA_CONSUMER_GROUP_ID" to "my-app-v1",
    "KAFKA_RAPID_TOPIC" to "teamname.rapid-v1"
)
```

## Rapids & Rivers

### Kjernebegreper

- **Rapid**: Kafka-topicen der alle events flyter
- **River**: En consumer som lytter på spesifikke event-typer
- **Need**: En forespørsel om data/handling
- **Demand**: Påkrevde felt i et event
- **Require**: Påkrevde verdier i et event
- **Reject**: Betingelser som ekskluderer et event
- **Interested In**: Valgfrie felt å fange opp

### Oppsett av applikasjonen

```kotlin
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() {
    val env = System.getenv()

    RapidApplication.create(env).apply {
        UserCreatedRiver(this, userRepository)
        PaymentProcessedRiver(this, paymentService)
    }.start()
}
```

### Opprette en River

```kotlin
import no.nav.helse.rapids_rivers.*

class UserCreatedRiver(
    rapidsConnection: RapidsConnection,
    private val userRepository: UserRepository
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "user_created") }
            validate { it.requireKey("user_id", "email", "name") }
            validate { it.require("@created_at", JsonNode::asLocalDateTime) }
            validate { it.interestedIn("phone_number") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val userId = packet["user_id"].asText()
        val email = packet["email"].asText()
        val name = packet["name"].asText()
        val createdAt = packet["created_at"].asLocalDateTime()

        userRepository.save(User(id = userId, email = email, name = name, createdAt = createdAt))
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        logger.error("Failed to validate message: ${problems.toExtendedReport()}")
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
```

### Valideringsalternativer

```kotlin
// Preconditions — "gjelder denne meldingen meg i det hele tatt?"
// Feil → onPreconditionError() (stille, ikke logget — høyt volum)
precondition { packet ->
    packet.requireValue("@event_name", "payment_processed")
    packet.forbid("@cancelled")
    packet.forbidValue("status", "cancelled")
}

// Validations — "er meldingen jeg bryr meg om velformet?"
// Feil → onError() (logget — indikerer kontraktsbrudd)
validate { packet ->
    packet.requireKey("transaction_id", "amount")
    packet.require("amount", JsonNode::asDouble)
    packet.require("processed_at", JsonNode::asLocalDateTime)
    packet.requireAny("user_id", "session_id")
    packet.interestedIn("metadata", "correlation_id")
}
```

## Publisere events

```kotlin
fun publishUserCreatedEvent(user: User, context: MessageContext) {
    val event = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "user_created",
            "@id" to UUID.randomUUID().toString(),
            "@created_at" to LocalDateTime.now(),
            "@produced_by" to "my-service",
            "user_id" to user.id,
            "email" to user.email,
            "name" to user.name
        )
    )
    context.publish(event.toJson())
}
```

### Event-metadata

Inkluder alltid standard metadata:

```kotlin
"@event_name" to "payment_processed",  // Event-type
"@id" to UUID.randomUUID().toString(), // Unik event-ID
"@created_at" to LocalDateTime.now(),  // Når eventet ble opprettet
"@produced_by" to "payment-service",   // Tjenesten som opprettet det
"@correlation_id" to correlationId     // Correlation ID for requesten (valgfritt)
```

## Event-schema-design

```kotlin
// ✅ Bra - fortid, spesifikk, uforanderlige fakta
"user_created", "payment_processed", "application_approved"

// ❌ Dårlig - imperativ, vag
"create_user", "process", "handle_application"
```

### Event-versjonering

```kotlin
// Alternativ 1: Versjon i event-navnet
"@event_name" to "user_created_v2"

// Alternativ 2: Versjonsfelt
"@event_name" to "user_created",
"@version" to 2
```

## Testing med TestRapid

```kotlin
import no.nav.helse.rapids_rivers.testsupport.TestRapid

class UserCreatedRiverTest {
    private lateinit var testRapid: TestRapid
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setup() {
        testRapid = TestRapid()
        userRepository = InMemoryUserRepository()
        UserCreatedRiver(testRapid, userRepository)
    }

    @Test
    fun `processes user_created event`() {
        testRapid.sendTestMessage("""
            {
                "@event_name": "user_created",
                "@id": "550e8400-e29b-41d4-a716-446655440000",
                "@created_at": "2024-01-15T10:30:00",
                "user_id": "12345",
                "email": "user@nav.no",
                "name": "Test User"
            }
        """)

        val user = userRepository.findById("12345")
        assertEquals("user@nav.no", user.email)
    }

    @Test
    fun `publishes downstream event`() {
        testRapid.sendTestMessage(/* ... */)

        val published = testRapid.inspektør.message(0)
        assertEquals("need_user_permissions", published["@event_name"].asText())
    }
}
```

## Feilhåndtering

### Retries og DLQ

```kotlin
override fun onPacket(packet: JsonMessage, context: MessageContext) {
    try {
        processEvent(packet)
    } catch (e: TemporaryException) {
        throw e // La Kafka retry-e
    } catch (e: PermanentException) {
        logger.error("Permanent error processing event", e)
        dlqProducer.send(eventName = "user_created", originalMessage = packet.toJson(), error = e.message)
    }
}
```

### Idempotens

```kotlin
override fun onPacket(packet: JsonMessage, context: MessageContext) {
    val eventId = packet["@id"].asText()
    if (eventRepository.exists(eventId)) {
        logger.info("Event $eventId already processed, skipping")
        return
    }
    processEvent(packet)
    eventRepository.markProcessed(eventId)
}
```

## Overvåking

```kotlin
private val eventsProcessed = meterRegistry.counter("events_processed_total", "event_name", "user_created")
private val processingDuration = meterRegistry.timer("event_processing_duration_seconds", "event_name", "user_created")

override fun onPacket(packet: JsonMessage, context: MessageContext) {
    processingDuration.record {
        processEvent(packet)
        eventsProcessed.increment()
    }
}
```

## Fallgruver

- Å endre `KAFKA_CONSUMER_GROUP_ID` forårsaker reprosessering av alle meldinger
- `precondition`-feil er stille (høyt volum) — bruk for filtrering av event-type
- `validate`-feil kaller `onError()` — bruk for schema-validering
- Inkluder alltid `@id` for idempotens
- Ikke publiser PII i event-payloads uten kryptering
- Test med `TestRapid` — mock aldri Kafka direkte

## Grenser

### ✅ Alltid

- Bruk fortid for event-navn (`user_created`, ikke `create_user`)
- Inkluder standard metadata (`@event_name`, `@id`, `@created_at`)
- Implementer idempotens (sjekk `@id` før prosessering)
- Skriv TestRapid-tester for alle Rivers
- Bruk `precondition` for filtrering av event-type
- Logg med `event_id` for sporbarhet

### ⚠️ Spør først

- Opprette nye Kafka-topics
- Endre consumer group-IDer
- Publisere høyvolum-events (> 1000/sek)
- Endre event-schemas (breaking changes)

### 🚫 Aldri

- Bruk imperative event-navn
- Hopp over `@id`-feltet
- Endre consumer group uten migreringsplan
- Publiser PII i event-payloads uten kryptering
- Ignorer `onError`-handleren i Rivers
