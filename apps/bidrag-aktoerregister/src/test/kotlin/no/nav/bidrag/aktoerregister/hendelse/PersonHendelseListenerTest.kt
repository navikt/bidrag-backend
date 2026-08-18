package no.nav.bidrag.aktoerregister.hendelse

import no.nav.bidrag.aktoerregister.AktoerregisterApplicationTest
import no.nav.bidrag.generer.testdata.person.Kjønn
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.shaded.org.awaitility.Awaitility
import org.testcontainers.shaded.org.awaitility.Durations
import java.time.LocalDate

@SpringBootTest(classes = [AktoerregisterApplicationTest::class])
@Testcontainers
@EnableMockOAuth2Server
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = [
        "listeners=EXTERNAL://localhost:0,CONTROLLER://localhost:0",
        "listener.security.protocol.map=EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT",
        "controller.listener.names=CONTROLLER",
        "inter.broker.listener.name=EXTERNAL",
        "offsets.topic.num.partitions=1",
    ],
)
class PersonHendelseListenerTest {

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Value("\${TOPIC_PERSONHENDELSE}")
    private lateinit var topic: String

    companion object {

        @Container
        var database: PostgreSQLContainer = PostgreSQLContainer("postgres").apply {
            withDatabaseName("test_db")
            withUsername("root")
            withPassword("root")
            withInitScript("db-setup.sql")
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(propertyRegistry: DynamicPropertyRegistry) {
            propertyRegistry.add("spring.datasource.url") { database.jdbcUrl }
        }
    }

    @Test
    fun `skal lese inn person-hendelse via kafka`() {
        val personIdent1 = genererFødselsnummer(LocalDate.now().minusYears(30), Kjønn.MANN)
        val personIdent2 = genererFødselsnummer(LocalDate.now().minusYears(30), Kjønn.KVINNE)
        val hendelse = "{" +
            "\"aktørid\":\"123456\"," +
            "\"personidenter\":[" +
            "\"$personIdent1\"," +
            "\"$personIdent2\"" +
            "]}"

        kafkaTemplate.send(topic, hendelse)

        // Sørger for at kafka er ferdig med prosessering av hendelse
        Awaitility.await().timeout(Durations.ONE_SECOND)
    }
}
