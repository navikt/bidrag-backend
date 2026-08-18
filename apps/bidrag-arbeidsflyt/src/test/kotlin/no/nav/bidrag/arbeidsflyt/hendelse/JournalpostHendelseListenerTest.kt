package no.nav.bidrag.arbeidsflyt.hendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.persistence.repository.DLQKafkaRepository
import no.nav.bidrag.arbeidsflyt.utils.bidragJournalpostIdNy
import no.nav.bidrag.arbeidsflyt.utils.createDLQKafka
import no.nav.bidrag.arbeidsflyt.utils.createJournalpostHendelse
import no.nav.bidrag.arbeidsflyt.utils.journalpostId4Ny
import no.nav.bidrag.arbeidsflyt.utils.personident2
import no.nav.bidrag.commons.util.secureLogger
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import java.util.concurrent.TimeUnit

private val LOGGER = KotlinLogging.logger {}

internal class JournalpostHendelseListenerTest(
    @Autowired private val dLQKafkaRepository: DLQKafkaRepository,
) : AbstractKafkaHendelseTest() {
    @Value($$"${TOPIC_JOURNALPOST}")
    private val topic: String? = null

    @BeforeEach
    fun setup() {
        testDataGenerator.deleteAll()
        dLQKafkaRepository.deleteAll()
    }

    @AfterEach
    fun cleanup() {
        testDataGenerator.deleteAll()
        dLQKafkaRepository.deleteAll()
    }

    @Test
    fun `skal slette feilede meldinger fra dlqkafka nar behandling av melding gar ok`() {
        stubHentOppgaveContaining(listOf())
        stubHentPerson()
        stubHentJournalforendeEnheter()
        stubHentEnhet()
        stubHentGeografiskEnhet(enhet = "1234")
        val journalpostIdMedJoarkPrefix = "JOARK-$journalpostId4Ny"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)

        testDataGenerator.opprettDLQMelding(
            createDLQKafka(objectMapper.writeValueAsString(journalpostHendelse), messageKey = journalpostIdMedJoarkPrefix),
        )
        testDataGenerator.opprettDLQMelding(
            createDLQKafka(objectMapper.writeValueAsString(journalpostHendelse), messageKey = journalpostIdMedJoarkPrefix),
        )

        val dlqMessagesBefore = testDataGenerator.hentDlKafka().filter { it.topicName == topic }
        assertThat(dlqMessagesBefore.size).isEqualTo(2)

        val hendelseString =
            objectMapper.writeValueAsString(
                journalpostHendelse.copy(
                    aktorId = "123213213",
                    fnr = "123123123",
                ),
            )
        configureProducer()?.send(ProducerRecord(topic, hendelseString))

        await.atMost(4, TimeUnit.SECONDS).untilAsserted {
            val dlqMessagesAfter = testDataGenerator.hentDlKafka().filter { it.topicName == topic }
            secureLogger.info { "MELDINGER: $dlqMessagesAfter" }
            assertThat(dlqMessagesAfter.size).isEqualTo(0)
        }
    }

    @Test
    fun `skal legge melding i dead_letter_kafka tabellen hvis behandling feiler`() {
        stubHentOppgaveSok(emptyList())
        stubOpprettOppgave(status = HttpStatus.INTERNAL_SERVER_ERROR)
        stubHentPerson(personident2)
        stubHentEnhet()
        val journalpostHendelse = createJournalpostHendelse(bidragJournalpostIdNy)
        val hendelseString = objectMapper.writeValueAsString(journalpostHendelse)
        configureProducer()?.send(ProducerRecord(topic, bidragJournalpostIdNy, hendelseString))

        await.atMost(6, TimeUnit.SECONDS).untilAsserted {
            val dlMessages = testDataGenerator.hentDlKafka().filter { it.topicName == topic }
            assertThat(dlMessages.size).isEqualTo(1)
            secureLogger.info { "MELDINGER: $dlMessages" }
            assertThat(dlMessages[0].messageKey).isEqualTo(bidragJournalpostIdNy)
        }
    }

    @Test
    fun `skal opprette oppgave med BID prefix nar journalpost mottatt uten oppgave`() {
        val geografiskEnhet = "4812"
        stubHentGeografiskEnhet("0101")
        stubHentOppgaveSok(emptyList())
        stubOpprettOppgave()
        stubHentEnhet()
        stubHentPerson(personident2)
        val journalpostHendelse = createJournalpostHendelse(bidragJournalpostIdNy).copy(enhet = geografiskEnhet)
        val hendelseString = objectMapper.writeValueAsString(journalpostHendelse)
        configureProducer()?.send(ProducerRecord(topic, hendelseString))

        await.atMost(4, TimeUnit.SECONDS).untilAsserted {
            verifyOppgaveOpprettetWith(
                "\"tildeltEnhetsnr\":\"$geografiskEnhet\"",
                "\"oppgavetype\":\"JFR\"",
                "\"journalpostId\":\"${bidragJournalpostIdNy}\"",
                "\"opprettetAvEnhetsnr\":\"9999\"",
                "\"prioritet\":\"HOY\"",
                "\"tema\":\"BID\"",
            )
            verifyOppgaveNotEndret()
        }
    }
}
