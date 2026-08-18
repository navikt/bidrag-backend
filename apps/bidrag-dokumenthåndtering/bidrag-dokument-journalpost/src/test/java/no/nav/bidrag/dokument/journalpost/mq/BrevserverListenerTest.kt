package no.nav.bidrag.dokument.journalpost.mq

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import jakarta.jms.Queue
import jakarta.jms.TextMessage
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles
import no.nav.bidrag.dokument.journalpost.TestDataManager
import no.nav.bidrag.dokument.journalpost.consumer.mq.BrevserverKvitteringListener
import no.nav.bidrag.dokument.journalpost.entity.Journalpost
import no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger
import no.nav.bidrag.dokument.journalpost.exception.BehandlingAvBrevkvitteringFeilet
import no.nav.bidrag.dokument.journalpost.hendelse.DokumentKafkaHendelseProdusent
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer
import no.nav.bidrag.dokument.journalpost.model.DokumentType
import no.nav.bidrag.dokument.journalpost.model.Journalstatus
import no.nav.bidrag.transport.dokument.DokumentArkivSystemDto
import no.nav.bidrag.transport.dokument.DokumentHendelse
import no.nav.bidrag.transport.dokument.DokumentHendelseType
import no.nav.bidrag.transport.dokument.DokumentStatusDto
import no.nav.bidrag.transport.dokument.HendelseType
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.activemq.artemis.api.core.client.ClientSession
import org.apache.activemq.artemis.jms.client.ActiveMQTextMessage
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jms.core.JmsTemplate
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.io.StringWriter
import java.time.Duration

@ActiveProfiles(BidragDokumentJournalpostProfiles.TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [BidragDokumentJournalpostLocalTest::class])
@EnableWireMock(ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
@Disabled
class BrevserverListenerTest {
    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    lateinit var brevserverKvitteringListener: BrevserverKvitteringListener

    @Autowired
    lateinit var testDataManager: TestDataManager

    @Autowired
    lateinit var brevkvitterinQueue: Queue

    @Autowired
    lateinit var jmsTemplate: JmsTemplate

    @MockkBean
    lateinit var journalpostKafkaEventProducerMock: JournalpostKafkaEventProducer

    @MockkBean
    lateinit var dokumentKafkaProdusentMock: DokumentKafkaHendelseProdusent

    @BeforeEach
    fun initMocks() {
        testDataManager.slettAlt()
        every { journalpostKafkaEventProducerMock.publish(any()) } returns Unit
        every { dokumentKafkaProdusentMock.publish(any()) } returns Unit
    }

    @AfterEach
    fun cleanupDatabase() {
        testDataManager.slettAlt()
    }

    private fun sendMessage(
        queue: Queue,
        message: String,
    ) {
        transactionTemplate.execute {
            jmsTemplate.send(queue) {
                val msg: TextMessage = ActiveMQTextMessage(it as ClientSession?)
                msg.text = message
                msg
            }
        }
    }

    fun konvertTilXml(brevKvittering: BrevKvittering): String {
        val marshaller = Jaxb2Marshaller()
        marshaller.setClassesToBeBound(BrevKvittering::class.java)
        val sw = StringWriter()
        marshaller.jaxbContext.createMarshaller().marshal(brevKvittering, sw)
        return sw.toString()
    }

    @Test
    fun shouldUpdateJournalstatusToMottattWhenInngaaende() {
        val hendelseCaptor = slot<JournalpostHendelse>()
        val hendelseCaptorDokument = slot<DokumentHendelse>()
        every { journalpostKafkaEventProducerMock.publish(capture(hendelseCaptor)) } returns Unit
        every { dokumentKafkaProdusentMock.publish(capture(hendelseCaptorDokument)) } returns Unit

        val dokumentRef = "4441241233123213"
        val saksnummer1 = "15454523333"
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enMottaksregistrertJournalpost()
                    .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                    .medJournalstatus(null)
                    .medJournalforendeEnhet("4806")
                    .leggTilSaksnummer(saksnummer1)
                    .medDokumentreferanse(dokumentRef),
            )

        val kvittering =
            "" +
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
                "<rtv-brevkvitt>\n" +
                "  <brevref>$dokumentRef</brevref>\n" +
                "  <sysid>BI12</sysid>\n" +
                "  <type>application/pdf</type>\n" +
                "  <status>FERDIG</status>\n" +
                "  <feilkode>null</feilkode>\n" +
                "</rtv-brevkvitt>"
        sendMessage(brevkvitterinQueue, kvittering)

        await.pollInterval(Duration.ofMillis(200)).atMost(Duration.ofSeconds(10)).untilAsserted {
            sendMessage(brevkvitterinQueue, kvittering)
            val updatedJournalpost = testDataManager.hent(journalpost.journalpostId, Journalpost::class.java).orElse(null)
            updatedJournalpost.journalstatus shouldBe Journalstatus.MOTTAKSREGISTRERT
            hendelseCaptor.isCaptured shouldBe true
            hendelseCaptorDokument.isCaptured shouldBe true
            val hendelse = hendelseCaptor.captured
            val dokumentHendelse = hendelseCaptorDokument.captured

            hendelse.journalstatus shouldBe Journalstatus.MOTTAKSREGISTRERT
            hendelse.enhet shouldBe "4806"
            hendelse.journalposttype shouldBe DokumentType.INNGAENDE_DOKUMENT
            hendelse.hendelseType shouldBe HendelseType.ENDRING
            hendelse.sakstilknytninger?.shouldContain(saksnummer1)
            hendelse.sporing?.enhetsnummer shouldBe "9999"
            hendelse.sporing?.saksbehandlersNavn shouldBe "bidrag-dokument-journalpost"

            dokumentHendelse.dokumentreferanse shouldBe dokumentRef
            dokumentHendelse.hendelseType shouldBe DokumentHendelseType.FERDIGSTILT
            dokumentHendelse.arkivSystem shouldBe DokumentArkivSystemDto.MIDLERTIDLIG_BREVLAGER
            dokumentHendelse.status shouldBe DokumentStatusDto.FERDIGSTILT
        }
    }

    @Test
    fun shouldSendWithNullEnhetWhenJournalpostEnhetIsNull() {
        val hendelseCaptor = slot<JournalpostHendelse>()
        val hendelseCaptorDokument = slot<DokumentHendelse>()
        every { journalpostKafkaEventProducerMock.publish(capture(hendelseCaptor)) } returns Unit
        every { dokumentKafkaProdusentMock.publish(capture(hendelseCaptorDokument)) } returns Unit

        val dokumentRef = "BIF6632513"
        val saksnummer1 = "123345454533"
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enMottaksregistrertJournalpost()
                    .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                    .medJournalstatus(null)
                    .medJournalforendeEnhet(null)
                    .leggTilSaksnummer(saksnummer1)
                    .medDokumentreferanse(dokumentRef),
            )
        val brevKvittering = BrevKvittering(dokumentRef, BrevStatus.FERDIG, "BI12")
        sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))

        await.pollInterval(Duration.ofMillis(200)).atMost(Duration.ofSeconds(10)).untilAsserted {
            sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
            val updatedJournalpost = testDataManager.hent(journalpost.journalpostId, Journalpost::class.java).orElse(null)
            updatedJournalpost.journalstatus shouldBe Journalstatus.MOTTAKSREGISTRERT
            hendelseCaptor.isCaptured shouldBe true
            val hendelse = hendelseCaptor.captured
            hendelse.enhet shouldBe null
        }
    }

    @Test
    fun shouldUpdateJournalstatusToMottattWhenInngaaendeAndSendHendelseWithCorrectEnhet() {
        val hendelseCaptor = slot<JournalpostHendelse>()
        val hendelseCaptorDokument = slot<DokumentHendelse>()
        every { journalpostKafkaEventProducerMock.publish(capture(hendelseCaptor)) } returns Unit
        every { dokumentKafkaProdusentMock.publish(capture(hendelseCaptorDokument)) } returns Unit

        val dokumentRef = "32131256576547"
        val saksnummer1 = "123334545454553"
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enMottaksregistrertJournalpost()
                    .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                    .medJournalstatus(null)
                    .medJournalforendeEnhet("2101")
                    .leggTilSaksnummer(saksnummer1)
                    .medDokumentreferanse(dokumentRef),
            )
        val brevKvittering = BrevKvittering(dokumentRef, BrevStatus.FERDIG, "BI12")
        sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))

        await
            .pollInterval(Duration.ofMillis(200))
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
                val updatedJournalpost = testDataManager.hent(journalpost.journalpostId, Journalpost::class.java).orElse(null)
                updatedJournalpost.journalstatus shouldBe Journalstatus.MOTTAKSREGISTRERT

                val hendelse = hendelseCaptor.captured
                val dokumentHendelse = hendelseCaptorDokument.captured

                hendelse.journalstatus shouldBe Journalstatus.MOTTAKSREGISTRERT
                hendelse.enhet shouldBe "4865"
                hendelse.journalposttype shouldBe DokumentType.INNGAENDE_DOKUMENT
                hendelse.hendelseType shouldBe HendelseType.ENDRING

                dokumentHendelse.dokumentreferanse shouldBe dokumentRef
                dokumentHendelse.hendelseType shouldBe DokumentHendelseType.FERDIGSTILT
                dokumentHendelse.arkivSystem shouldBe DokumentArkivSystemDto.MIDLERTIDLIG_BREVLAGER
                dokumentHendelse.status shouldBe DokumentStatusDto.FERDIGSTILT
            }
    }

    @Test
    fun shouldUpdateJournalstatusToKlarTilPrintWhenUtgaaende() {
        val dokumentRef = "5123213123"
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalpost()
                    .medDokumentType(DokumentType.UTGAAENDE_DOKUMENT)
                    .medJournalstatus(null)
                    .medDokumentreferanse(dokumentRef),
            )
        val brevKvittering = BrevKvittering(dokumentRef, BrevStatus.FERDIG, "BI12")
        sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
        await
            .pollInterval(Duration.ofMillis(200))
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
                val updatedJournalpost = testDataManager.hent(journalpost.journalpostId, Journalpost::class.java).orElse(null)
                updatedJournalpost.journalstatus shouldBe Journalstatus.KLAR_TIL_PRINT
            }
    }

    @Test
    fun shouldUpdateJournalstatusToReservertWhenNotat() {
        val dokumentRef = "52355535"
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalpost()
                    .medDokumentType(DokumentType.NOTAT)
                    .medJournalstatus(null)
                    .medDokumentreferanse(dokumentRef),
            )
        val brevKvittering = BrevKvittering(dokumentRef, BrevStatus.FERDIG, "BI12")
        sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
        await
            .pollInterval(Duration.ofMillis(200))
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
                val updatedJournalpost = testDataManager.hent(journalpost.journalpostId, Journalpost::class.java).orElse(null)
                updatedJournalpost.journalstatus shouldBe Journalstatus.RESERVERT
            }
    }

    @Test
    fun shouldUpdateJournalstatusToUnderProduksjonWhenUtgaaendeAndBrevstatusLagret() {
        val dokumentRef = "515551"
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalpost()
                    .medDokumentType(DokumentType.UTGAAENDE_DOKUMENT)
                    .medJournalstatus(null)
                    .medDokumentreferanse(dokumentRef),
            )
        val brevKvittering = BrevKvittering(dokumentRef, BrevStatus.LAGRET, "BI12")
        sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
        await
            .pollInterval(Duration.ofMillis(200))
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
                val updatedJournalpost = testDataManager.hent(journalpost.journalpostId, Journalpost::class.java).orElse(null)
                updatedJournalpost.journalstatus shouldBe Journalstatus.UNDER_PRODUKSJON
            }
    }

    @Test
    fun shouldUpdateJournalstatusToUnderProduksjonWhenNotatAndBrevstatusLagret() {
        val dokumentRef = "1231231255"
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalpost()
                    .medDokumentType(DokumentType.UTGAAENDE_DOKUMENT)
                    .medJournalstatus(null)
                    .medDokumentreferanse(dokumentRef),
            )
        val brevKvittering = BrevKvittering(dokumentRef, BrevStatus.LAGRET, "BI12")
        sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
        await
            .pollInterval(Duration.ofMillis(200))
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
                val updatedJournalpost = testDataManager.hent(journalpost.journalpostId, Journalpost::class.java).orElse(null)
                updatedJournalpost.journalstatus shouldBe Journalstatus.UNDER_PRODUKSJON
            }
    }

    @Test
    fun skalSendeMeldingTilDokumentKafka() {
        val dokumentRef = "1231231255"
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalpost()
                    .medDokumentType(DokumentType.UTGAAENDE_DOKUMENT)
                    .medJournalstatus(null)
                    .medDokumentreferanse(dokumentRef),
            )
        val brevKvittering = BrevKvittering(dokumentRef, BrevStatus.LAGRET, "BI12")
        sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
        await
            .pollInterval(Duration.ofMillis(200))
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
                val updatedJournalpost = testDataManager.hent(journalpost.journalpostId, Journalpost::class.java).orElse(null)
                updatedJournalpost.journalstatus shouldBe Journalstatus.UNDER_PRODUKSJON
            }
    }

    @Test
    fun shouldSendDokumentHendelseWhenDokumentrefIsForsendelse() {
        val hendelseCaptor = slot<JournalpostHendelse>()
        val hendelseCaptorDokument = slot<DokumentHendelse>()
        every { journalpostKafkaEventProducerMock.publish(capture(hendelseCaptor)) } returns Unit
        every { dokumentKafkaProdusentMock.publish(capture(hendelseCaptorDokument)) } returns Unit

        val dokumentRef = "BIF_32131256576547"

        val brevKvittering = BrevKvittering(dokumentRef, BrevStatus.LAGRET, "BI12")
        sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))

        await
            .pollInterval(Duration.ofMillis(200))
            .atMost(Duration.ofSeconds(10))
            .untilAsserted {
                sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
                verify { dokumentKafkaProdusentMock.publish(any()) }
                hendelseCaptor.isCaptured shouldBe false
                hendelseCaptorDokument.isCaptured shouldBe true

                val dokumentHendelse = hendelseCaptorDokument.captured

                dokumentHendelse.dokumentreferanse shouldBe dokumentRef
                dokumentHendelse.hendelseType shouldBe DokumentHendelseType.ENDRING
                dokumentHendelse.arkivSystem shouldBe DokumentArkivSystemDto.MIDLERTIDLIG_BREVLAGER
                dokumentHendelse.status shouldBe DokumentStatusDto.UNDER_REDIGERING
            }
    }

    @Test
    @Disabled("Tar veldig lang tid pga retry")
    fun shouldThrowWhenDokumentreferanseNotFound() {
        val dokumentRef = "32131256576547"

        val brevKvittering = BrevKvittering(dokumentRef, BrevStatus.LAGRET, "BI12")
        sendMessage(brevkvitterinQueue, konvertTilXml(brevKvittering))
        shouldThrow<BehandlingAvBrevkvitteringFeilet> { brevserverKvitteringListener.receiveMessage(brevKvittering) }
    }
}
