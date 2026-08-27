---
name: Kotlin Testing
description: "Kotlin-spesifikke testmønstre for bidrag-backend: Kotest-matchers, Spring Kafka-testing, Testcontainers og MockOAuth2Server."
applyTo: "**/*.test.{kt,kts}"
---

# Kotlin-testing (Kotest & JUnit 5)

Kotlin-spesifikke testmønstre for `bidrag-backend`: Kotest-matchers, MockK, Spring Kafka-testing, Testcontainers og MockOAuth2Server.

## Teststruktur

```kotlin
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll

class ServiceTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            // Oppsettskode
        }
    }

    @Test
    fun `should process event correctly`() {
        // Arrange
        val input = createTestInput()

        // Act
        val result = service.process(input)

        // Assert
        result shouldBe expectedResult
        result.status shouldBe "completed"
    }
}
```

## Kotest-matchers

```kotlin
// Likhet
result shouldBe expected
result shouldNotBe unexpected

// Null-sjekker
result shouldNotBe null
nullableValue shouldBe null

// Collections
list.size shouldBe 3
list shouldContain item
list shouldContainAll listOf(item1, item2)

// Exceptions
shouldThrow<IllegalArgumentException> {
    service.processInvalid()
}

// Numeriske sammenligninger
value shouldBeGreaterThan 0
value shouldBeLessThanOrEqual 100
```

## MockK

```kotlin
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

val repository = mockk<ResourceRepository>()

every { repository.findById(any()) } returns testEntity
val result = service.findById(id)

verify(exactly = 1) { repository.findById(id) }
```

## Testing av Kafka (Spring Kafka)

```kotlin
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = ["mitt-topic"])
class EventHandlerTest {
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Test
    fun `should publish event after processing`() {
        kafkaTemplate.send("mitt-topic", "key", testMessage)

        val consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafka)
        val consumer = DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer()
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "mitt-topic")

        val record = KafkaTestUtils.getSingleRecord(consumer, "mitt-topic")
        record.value() shouldBe expectedPayload
    }
}
```

## Testing med Testcontainers

```kotlin
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class RepositoryTest {
    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("testdb")
        }
    }

    private lateinit var dataSource: HikariDataSource
    private lateinit var repository: Repository

    @BeforeEach
    fun setup() {
        dataSource = HikariDataSource().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
        }

        // Kjør migrasjoner
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()

        repository = RepositoryPostgres(dataSource)
    }

    @Test
    fun `should save and retrieve entity`() {
        val entity = Entity(name = "test")
        val id = repository.save(entity)

        val retrieved = repository.findById(id)

        retrieved shouldNotBe null
        retrieved?.name shouldBe "test"
    }
}
```

> Merk: Enkelte apper i dette repoet (f.eks. de som kobler mot Bisys/DB2) låser `testcontainers.version` til 1.x lokalt i sin `pom.xml` fordi root-BOM-en sin 2.x-versjon mangler DB2-modulen. Sjekk appens egen `pom.xml` før du antar hvilken Testcontainers-API du kan bruke.

## Testing av autentisering (MockOAuth2Server)

```kotlin
import no.nav.security.mock.oauth2.MockOAuth2Server

class AuthenticationTest {
    private val mockOAuth2Server = MockOAuth2Server()

    @BeforeEach
    fun setup() {
        mockOAuth2Server.start()
    }

    @AfterEach
    fun tearDown() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `should authenticate with valid token`() {
        val token = mockOAuth2Server.issueToken(
            issuerId = "azuread",
            subject = "test-user",
            claims = mapOf("preferred_username" to "test@nav.no")
        )

        val response = client.get("/api/protected") {
            bearerAuth(token.serialize())
        }

        response.status shouldBe HttpStatusCode.OK
    }
}
```

## Kjøre tester

```bash
mvn test                       # Kjør tester for hele reactoren
mvn -pl apps/<app> -am test    # Kjør tester kun for én app (+ moduler den er avhengig av)
mvn verify                     # Inkluderer ktlint-sjekk
```
