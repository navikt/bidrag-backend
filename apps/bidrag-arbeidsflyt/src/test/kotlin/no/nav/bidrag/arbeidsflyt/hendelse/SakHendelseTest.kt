package no.nav.bidrag.arbeidsflyt.hendelse

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.client.WireMock.verify
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.bidrag.arbeidsflyt.UnleashFeatures
import no.nav.bidrag.arbeidsflyt.dto.MappeDto
import no.nav.bidrag.arbeidsflyt.dto.MapperResponse
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import no.nav.bidrag.arbeidsflyt.service.BehandleSakHendelseService
import no.nav.bidrag.arbeidsflyt.utils.disableUnleashFeature
import no.nav.bidrag.arbeidsflyt.utils.enableUnleashFeature
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Fogdårsak
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.sak.Tilgangstype
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.FogdhistorikkDto
import no.nav.bidrag.transport.sak.SakHendelse
import no.nav.bidrag.transport.sak.SakKafkaHendelsestype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.HttpServerErrorException
import java.time.LocalDate

internal class SakHendelseTest : AbstractBehandleHendelseTest() {
    @Autowired
    lateinit var service: BehandleSakHendelseService

    @Autowired
    lateinit var scheduler: KafkaDLQRetryScheduler

    private val sak = BidragssakDto(
        eierfogd = Enhetsnummer("4806"),
        saksnummer = Saksnummer("1234567"),
        saksstatus = Bidragssakstatus.IN,
        kategori = Sakskategori.NASJONAL,
        opprettetDato = LocalDate.parse("2026-01-01"),
        levdeAdskilt = false,
        ukjentPart = false,
    )
    private val hendelse = SakHendelse(sak.saksnummer, SakKafkaHendelsestype.ENDRING)
    private val oppgave = OppgaveData(
        id = 1001,
        versjon = 3,
        saksreferanse = sak.saksnummer.verdi,
        tildeltEnhetsnr = "4833",
        tema = "BID",
        status = OppgaveStatus.AAPNET,
    )

    @BeforeEach
    fun settOppSak() {
        stubHentSak(sak)
        stubOppgaver(listOf(oppgave))
        testDataGenerator.opprettSak("1234567", "4833")
        enableUnleashFeature(UnleashFeatures.FLYTT_OPPGAVER_VED_ENHETSBYTTE)
    }

    @Test
    fun `skal bare flytte oppgaver fra enheten saken ble flyttet fra`() {
        stubOppgaver(listOf(oppgave, oppgave.copy(id = 1002, tildeltEnhetsnr = "4802"), oppgave.copy(id = 1003, tema = "FAR")))

        service.behandleHendelse(hendelse)

        verify(
            1,
            patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/1001"))
                .withRequestBody(matchingJsonPath("$.versjon", equalTo("3")))
                .withRequestBody(matchingJsonPath("$.tildeltEnhetsnr", equalTo("4806")))
                .withRequestBody(matchingJsonPath("$.status", equalTo("OPPRETTET"))),
        )
        verify(0, patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/1002")))
        verify(1, patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/1003")))
    }

    @Test
    fun `skal lagre ny eierfogd etter flytting`() {
        service.behandleHendelse(hendelse)

        testDataGenerator.hentSak("1234567")!!.eierfogd shouldBe "4806"
    }

    @Test
    fun `skal bare lagre eierfogd når flagget er av`() {
        disableUnleashFeature(UnleashFeatures.FLYTT_OPPGAVER_VED_ENHETSBYTTE)

        service.behandleHendelse(hendelse)

        verify(0, getRequestedFor(urlPathEqualTo("/oppgave/api/v1/oppgaver/")))
        testDataGenerator.hentSak("1234567")!!.eierfogd shouldBe "4806"
    }

    @Test
    fun `skal bruke fogdhistorikk når saken ikke er lagret`() {
        testDataGenerator.sakRepository.deleteAll()
        stubFogdhistorikk(
            listOf(
                eier(1, "4802", LocalDate.now().minusYears(1)),
                eier(2, "4833", LocalDate.now()),
                eier(3, "4806", null),
            ),
        )

        service.behandleHendelse(hendelse)

        verify(1, patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/1001")))
        testDataGenerator.hentSak("1234567")!!.eierfogd shouldBe "4806"
    }

    @Test
    fun `skal ikke flytte når saken ikke er lagret og eierfogd ikke er endret nylig`() {
        testDataGenerator.sakRepository.deleteAll()
        stubFogdhistorikk(
            listOf(
                eier(1, "4833", LocalDate.now().minusYears(1)),
                eier(2, "4806", null),
            ),
        )

        service.behandleHendelse(hendelse)

        verify(0, getRequestedFor(urlPathEqualTo("/oppgave/api/v1/oppgaver/")))
        testDataGenerator.hentSak("1234567")!!.eierfogd shouldBe "4806"
    }

    @Test
    fun `skal flytte til dagens eierfogd når saken er flyttet videre`() {
        stubHentSak(sak.copy(eierfogd = Enhetsnummer("4802")))

        service.behandleHendelse(hendelse)

        verify(
            1,
            patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/1001"))
                .withRequestBody(matchingJsonPath("$.tildeltEnhetsnr", equalTo("4802"))),
        )
    }

    @Test
    fun `skal ikke flytte oppgaver når saken er flyttet tilbake`() {
        stubHentSak(sak.copy(eierfogd = Enhetsnummer("4833")))

        service.behandleHendelse(hendelse)

        verify(0, getRequestedFor(urlPathEqualTo("/oppgave/api/v1/oppgaver/")))
    }

    @Test
    fun `skal beholde saksbehandler som tilhører ny enhet`() {
        stubOppgaver(listOf(oppgave.copy(tilordnetRessurs = "Z123456")))
        stubFor(
            get("/organisasjon/saksbehandler/enhetsliste/Z123456").willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(listOf(EnhetDto(Enhetsnummer("4806"))))),
            ),
        )

        service.behandleHendelse(hendelse)

        val request = hentPatch()
        request.has("tilordnetRessurs") shouldBe false
        request["tildeltEnhetsnr"].asText() shouldBe "4806"
        request["status"].asText() shouldBe "OPPRETTET"
    }

    @Test
    fun `skal fjerne saksbehandler som ikke tilhører ny enhet`() {
        stubOppgaver(listOf(oppgave.copy(tilordnetRessurs = "Z123456")))
        stubFor(
            get("/organisasjon/saksbehandler/enhetsliste/Z123456").willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(listOf(EnhetDto(Enhetsnummer("4833"))))),
            ),
        )

        service.behandleHendelse(hendelse)

        hentPatch()["tilordnetRessurs"].asText() shouldBe ""
    }

    @Test
    fun `skal fjerne saksbehandler når organisasjon returnerer ingen enheter`() {
        stubOppgaver(listOf(oppgave.copy(tilordnetRessurs = "Z123456")))
        stubFor(get("/organisasjon/saksbehandler/enhetsliste/Z123456").willReturn(aResponse().withStatus(204)))

        service.behandleHendelse(hendelse)

        hentPatch()["tilordnetRessurs"].asText() shouldBe ""
    }

    @Test
    fun `skal fjerne saksbehandler når organisasjon ikke finner saksbehandleren`() {
        stubOppgaver(listOf(oppgave.copy(tilordnetRessurs = "Z123456")))
        stubFor(get("/organisasjon/saksbehandler/enhetsliste/Z123456").willReturn(aResponse().withStatus(404)))

        service.behandleHendelse(hendelse)

        hentPatch()["tilordnetRessurs"].asText() shouldBe ""
    }

    @Test
    fun `skal ikke tolke ugyldig tomt enhetssvar som manglende medlemskap`() {
        stubOppgaver(listOf(oppgave.copy(tilordnetRessurs = "Z123456")))
        stubFor(get("/organisasjon/saksbehandler/enhetsliste/Z123456").willReturn(aResponse().withStatus(200)))

        shouldThrow<RuntimeException> { service.behandleHendelse(hendelse) }

        verify(0, patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/1001")))
    }

    @Test
    fun `skal bruke mappe med samme navn i ny enhet og samme tema`() {
        stubOppgaver(listOf(oppgave.copy(mappeId = "10")))
        stubMappe()
        stubMapper(listOf(MappeDto(20, "4806", "Annen mappe"), MappeDto(30, "4806", "Til behandling")))

        service.behandleHendelse(hendelse)

        hentPatch()["mappeId"].asInt() shouldBe 30
        verify(
            1,
            getRequestedFor(urlPathEqualTo("/oppgave/api/v1/mapper"))
                .withQueryParam("enhetsnr", equalTo("4806"))
                .withQueryParam("tema", equalTo("BID")),
        )
    }

    @Test
    fun `skal nullstille mappe eksplisitt når tilsvarende mappe ikke finnes`() {
        stubOppgaver(listOf(oppgave.copy(mappeId = "10")))
        stubMappe()
        stubMapper(listOf(MappeDto(20, "4806", "Annen mappe")))

        service.behandleHendelse(hendelse)

        val request = hentPatch()
        request.has("mappeId") shouldBe true
        request["mappeId"].isNull shouldBe true
    }

    @Test
    fun `skal finne tilsvarende mappe på neste side`() {
        stubOppgaver(listOf(oppgave.copy(mappeId = "10")))
        stubMappe()
        stubFor(
            get(urlPathEqualTo("/oppgave/api/v1/mapper"))
                .withQueryParam("offset", equalTo("0"))
                .willReturn(
                    aResponse().withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(MapperResponse(2, listOf(MappeDto(20, "4806", "Annen mappe"))))),
                ),
        )
        stubFor(
            get(urlPathEqualTo("/oppgave/api/v1/mapper"))
                .withQueryParam("offset", equalTo("1"))
                .willReturn(
                    aResponse().withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(MapperResponse(2, listOf(MappeDto(30, "4806", "Til behandling"))))),
                ),
        )

        service.behandleHendelse(hendelse)

        hentPatch()["mappeId"].asInt() shouldBe 30
    }

    @Test
    fun `skal ikke slå opp mapper når oppgaven ikke har mappe`() {
        service.behandleHendelse(hendelse)

        verify(0, getRequestedFor(urlPathMatching("/oppgave/api/v1/mapper.*")))
        hentPatch().has("mappeId") shouldBe false
    }

    @Test
    fun `skal ikke flytte oppgaver uten sakstilgang`() {
        stubHentSak(sak.copy(begrensetTilgang = true, avsluttet = true))

        service.behandleHendelse(hendelse)

        verify(0, getRequestedFor(urlPathEqualTo("/oppgave/api/v1/oppgaver/")))
    }

    @Test
    fun `skal kaste feil når oppdatering av oppgave feiler`() {
        stubFor(patch("/oppgave/api/v1/oppgaver/1001").willReturn(aResponse().withStatus(500)))

        shouldThrow<HttpServerErrorException> { service.behandleHendelse(hendelse) }

        testDataGenerator.hentSak("1234567")!!.eierfogd shouldBe "4833"
        testDataGenerator.hentSak("1234567")!!.flytterTilEierfogd shouldBe "4806"
    }

    @Test
    fun `skal fullføre avbrutt flytting før saken flyttes videre`() {
        stubOppgaver(listOf(oppgave, oppgave.copy(id = 1002)))
        stubFor(patch("/oppgave/api/v1/oppgaver/1002").willReturn(aResponse().withStatus(500)))
        shouldThrow<HttpServerErrorException> { service.behandleHendelse(hendelse) }
        stubOppgaver(listOf(oppgave.copy(tildeltEnhetsnr = "4806"), oppgave.copy(id = 1002)))
        stubFor(patch("/oppgave/api/v1/oppgaver/1002").willReturn(aResponse().withStatus(200)))
        stubHentSak(sak.copy(eierfogd = Enhetsnummer("4802")))

        service.behandleHendelse(hendelse)

        verify(
            2,
            patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/1002"))
                .withRequestBody(matchingJsonPath("$.tildeltEnhetsnr", equalTo("4806"))),
        )
        verify(
            1,
            patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/1001"))
                .withRequestBody(matchingJsonPath("$.tildeltEnhetsnr", equalTo("4802"))),
        )
        testDataGenerator.hentSak("1234567")!!.eierfogd shouldBe "4802"
        testDataGenerator.hentSak("1234567")!!.flytterTilEierfogd shouldBe null
    }

    @Test
    fun `skal hente alle sider med oppgaver før flytting`() {
        stubOppgaver(listOf(oppgave), antall = 2)
        stubFor(
            get(urlPathEqualTo("/oppgave/api/v1/oppgaver/"))
                .withQueryParam("offset", equalTo("1"))
                .willReturn(
                    aResponse().withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(OppgaveSokResponse(2, listOf(oppgave.copy(id = 1002, tema = "BII"))))),
                ),
        )

        service.behandleHendelse(hendelse)

        verify(1, patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/1001")))
        verify(1, patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/1002")))
        verify(
            2,
            getRequestedFor(urlPathEqualTo("/oppgave/api/v1/oppgaver/"))
                .withQueryParam("statuskategori", equalTo("AAPEN"))
                .withQueryParam("saksreferanse", equalTo("1234567")),
        )
    }

    @Test
    fun `skal reprosessere sakshendelse fra DLQ`() {
        testDataGenerator.opprettDLQMelding(
            DLQKafka(
                messageKey = "1234567",
                topicName = "topic_sak",
                payload = objectMapper.writeValueAsString(hendelse),
                retry = true,
            ),
        )

        scheduler.processMessages()

        testDataGenerator.hentDlKafka() shouldBe emptyList()
        verify(1, patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/1001")))
    }

    private fun stubOppgaver(oppgaver: List<OppgaveData>, antall: Int = oppgaver.size) {
        stubFor(
            get(urlPathEqualTo("/oppgave/api/v1/oppgaver/"))
                .withQueryParam("offset", equalTo("0"))
                .willReturn(
                    aResponse().withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(OppgaveSokResponse(antall, oppgaver))),
                ),
        )
    }

    private fun eier(tilgangId: Int, enhetsnummer: String, tilgangTomDato: LocalDate?) = FogdhistorikkDto(
        tilgangId = tilgangId,
        enhetsnummer = enhetsnummer,
        tilgangFomDato = LocalDate.parse("2020-01-01"),
        tilgangTomDato = tilgangTomDato,
        arsak = Fogdårsak.EIER,
        type = Tilgangstype.EIER,
    )

    private fun stubFogdhistorikk(historikk: List<FogdhistorikkDto>) {
        stubFor(
            get("/sak/sak/1234567/fogdhistorikk").willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(historikk)),
            ),
        )
    }

    private fun stubMappe() {
        stubFor(
            get("/oppgave/api/v1/mapper/10").willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(MappeDto(10, "4833", "Til behandling"))),
            ),
        )
    }

    private fun stubMapper(mapper: List<MappeDto>) {
        stubFor(
            get(urlPathEqualTo("/oppgave/api/v1/mapper")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(MapperResponse(mapper.size, mapper))),
            ),
        )
    }

    private fun hentPatch() = objectMapper.readTree(
        com.github.tomakehurst.wiremock.client.WireMock
            .findAll(patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/1001"))).single().bodyAsString,
    )
}
