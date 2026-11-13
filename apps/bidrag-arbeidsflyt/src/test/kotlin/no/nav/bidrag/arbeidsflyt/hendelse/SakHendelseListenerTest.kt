package no.nav.bidrag.arbeidsflyt.hendelse

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.SakHendelse
import no.nav.bidrag.transport.sak.SakKafkaHendelsestype
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDate

@Transactional(propagation = Propagation.NOT_SUPPORTED)
internal class SakHendelseListenerTest : AbstractKafkaHendelseTest() {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun postgresProperties(registry: DynamicPropertyRegistry) = BehandlingRepositoryPostgresTest.postgresProperties(registry)
    }

    @Test
    fun `skal behandle sakshendelse fra Kafka og flytte oppgaver`() {
        val sak = BidragssakDto(
            eierfogd = Enhetsnummer("4806"),
            saksnummer = Saksnummer("1234567"),
            saksstatus = Bidragssakstatus.IN,
            kategori = Sakskategori.NASJONAL,
            opprettetDato = LocalDate.parse("2026-01-01"),
            levdeAdskilt = false,
            ukjentPart = false,
        )
        stubHentSak(sak)
        stubFor(
            get(urlPathEqualTo("/oppgave/api/v1/oppgaver/")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        objectMapper.writeValueAsString(
                            OppgaveSokResponse(1, listOf(OppgaveData(id = 1001, versjon = 3, tildeltEnhetsnr = "4833", tema = "BID", status = OppgaveStatus.AAPNET))),
                        ),
                    ),
            ),
        )
        val hendelse = SakHendelse(sak.saksnummer, SakKafkaHendelsestype.ENDRING, forrigeEierfogd = Enhetsnummer("4833"))

        configureProducer().shouldNotBeNull().use { producer ->
            producer.send(ProducerRecord("topic_sak", "1234567", objectMapper.writeValueAsString(hendelse))).get()
        }

        await().atMost(Duration.ofSeconds(15)).untilAsserted {
            verify(1, patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/1001")))
        }
    }
}
