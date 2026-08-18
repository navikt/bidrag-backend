package no.nav.bidrag.dokument.journalpost.controller

import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.dokument.journalpost.AvvikshendelseBuilder
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles
import no.nav.bidrag.dokument.journalpost.TestDataManager
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager
import no.nav.bidrag.dokument.journalpost.dto.Oppgave
import no.nav.bidrag.dokument.journalpost.dto.OpprettOppgaveResponse
import no.nav.bidrag.dokument.journalpost.dto.Saksbehandler
import no.nav.bidrag.dokument.journalpost.entity.Journalpost
import no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger
import no.nav.bidrag.dokument.journalpost.entity.ReturDetaljerLogg
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.ENHETSNUMMER
import no.nav.bidrag.dokument.journalpost.model.Avvikstype
import no.nav.bidrag.dokument.journalpost.model.DokumentType
import no.nav.bidrag.dokument.journalpost.model.Enhet
import no.nav.bidrag.dokument.journalpost.model.Fagomrade
import no.nav.bidrag.dokument.journalpost.model.JoarkArkiveringStatus
import no.nav.bidrag.dokument.journalpost.model.Journalstatus
import no.nav.bidrag.dokument.journalpost.model.StatusAvviksbehandling
import no.nav.bidrag.dokument.journalpost.repository.JournalHendelseRepository
import no.nav.bidrag.dokument.journalpost.service.TilgangskontrollService
import no.nav.bidrag.dokument.journalpost.service.TokenInformationService
import no.nav.bidrag.dokument.journalpost.utils.CustomHeader
import no.nav.bidrag.dokument.journalpost.utils.hentMuligAdvarsel
import no.nav.bidrag.dokument.journalpost.utils.hentMuligAdvarseliUtenExceptionPrefix
import no.nav.bidrag.dokument.journalpost.utils.initHttpEntity
import no.nav.bidrag.dokument.journalpost.utils.prefixId
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.dokument.AvvikType
import no.nav.bidrag.transport.dokument.Avvikshendelse
import no.nav.bidrag.transport.dokument.BehandleAvvikshendelseResponse
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.bidrag.transport.person.PersonDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.opentest4j.AssertionFailedError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.boot.resttestclient.getForEntity
import org.springframework.boot.resttestclient.postForEntity
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.net.URI
import java.time.LocalDate
import java.util.Optional

@ActiveProfiles(BidragDokumentJournalpostProfiles.TEST, BidragDokumentJournalpostProfiles.SECURED_TEST)
@DisplayName("JournalpostController og avvik")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [BidragDokumentJournalpostLocalTest::class],
    properties = ["STS_URL=junit"],
)
@EnableWireMock(value = [ConfigureWireMock(port = 0)])
@EnableMockOAuth2Server
internal class JournalpostControllerAvvikTest {
    private fun listeMedAvvikTyper(): ParameterizedTypeReference<List<AvvikType>> = object : ParameterizedTypeReference<List<AvvikType>>() {}

    @Autowired
    private lateinit var testDataManager: TestDataManager

    @Autowired
    private lateinit var httpHeaderTestRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var journalHendelseRepository: JournalHendelseRepository

    @MockitoBean
    private lateinit var tilgangskontrollServiceMock: TilgangskontrollService

    @MockitoBean
    private lateinit var httpHeaderRestTemplateMock: HttpHeaderRestTemplate

    @MockitoBean
    private lateinit var journalpostKafkaEventProducerMock: JournalpostKafkaEventProducer

    @MockitoBean
    private lateinit var saksbehandlerOidcTokenManagerMock: SaksbehandlerOidcTokenManager

    @MockitoBean
    private lateinit var tokenInformationServiceMock: TokenInformationService

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(
            tilgangskontrollServiceMock,
            httpHeaderRestTemplateMock,
            tokenInformationServiceMock,
            saksbehandlerOidcTokenManagerMock,
        )
    }

    @BeforeEach
    fun opprettKodeJournalstatusForVisning() {
        Mockito
            .`when`(
                httpHeaderRestTemplateMock.exchange(
                    ArgumentMatchers.any<URI>(),
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any(),
                    ArgumentMatchers.eq(
                        Saksbehandler::class.java,
                    ),
                ),
            ).thenReturn(ResponseEntity(HttpStatus.OK))
        testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.JOURNALFORT)
    }

    @Nested
    @DisplayName("finne")
    internal inner class Finn {
        @Test
        @DisplayName("skal ha http status 400 (BAD_REQUEST) ved henting av avvik på journalpost, men journalpostId prefix er ugyldig")
        fun skalHaHttpStatusBadRequestVedUgyldJournalpostIdPrefixNarAvvikHentesPaJournalpost() {
            val response = httpHeaderTestRestTemplate.getForEntity<JournalpostDto>(String.format(AVVIK_PA_JOURNALPOST, "1"))
            Assertions.assertThat(response).extracting { it.statusCode }.isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("skal få 404 (Not Found) når man skal hente avvik på journalpost som ikke finnes")
        fun skalFaNotFoundVedHentingAvAvvikJoutnalpostSomIkkeFinnes() {
            val responseEntity =
                httpHeaderTestRestTemplate.getForEntity<List<AvvikType>>(
                    String.format(AVVIK_PA_JOURNALPOST, "BID-1234567"),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.NOT_FOUND) },
                Executable { Assertions.assertThat(responseEntity.body).`as`("response body").isNull() },
            )
        }

        @Test
        @DisplayName("skal finne SLETT_JOURNALPOST når journalpost har status 'Under produksjon'")
        fun skalFinneSlettJournalpostNarJournalpostHarStatusUnderProduksjon() {
            testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.UNDER_PRODUKSJON)
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalpost()
                        .medJournalstatus(Journalstatus.UNDER_PRODUKSJON)
                        .leggTilSaksnummer(sak1337),
                )
            val avvikshendelserResponse =
                httpHeaderTestRestTemplate
                    .exchange(
                        String.format(AVVIK_PA_JP_MED_SAK, prefixId(journalpost), sak1337),
                        HttpMethod.GET,
                        null,
                        listeMedAvvikTyper(),
                    )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(avvikshendelserResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable { Assertions.assertThat(avvikshendelserResponse.body).`as`("resultat").contains(AvvikType.SLETT_JOURNALPOST) },
            )
        }

        @Test
        @DisplayName("skal finne BESTILL_SPLITTING")
        fun skalFinneAvvikBestilleSplitting() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .medBatchNavn("IKKE BJOARK BATCH")
                        .medFilnavn("dokumentet.pdf")
                        .medSkannetDato(LocalDate.now())
                        .leggTilSaksnummer(sak1337),
                )
            val avvikshendelserResponse =
                httpHeaderTestRestTemplate
                    .exchange(
                        String.format(AVVIK_PA_JP_MED_SAK, prefixId(journalpost), sak1337),
                        HttpMethod.GET,
                        null,
                        listeMedAvvikTyper(),
                    )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(avvikshendelserResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable { Assertions.assertThat(avvikshendelserResponse.body).`as`("resultat").contains(AvvikType.BESTILL_SPLITTING) },
            )
        }

        @Test
        @DisplayName("skal finne avviket FEILFORE_SAK for journalpost")
        fun skalFinneFeilforeSakForJournalpost() {
            val sak1001 = "1001"
            val feilfortJournalpost = testDataManager.opprett(JournalpostBygger.enJournalfortJournalpost().leggTilSaksnummer(sak1001))
            val avvikshendelserResponse =
                httpHeaderTestRestTemplate.exchange(
                    String.format(AVVIK_PA_JP_MED_SAK, prefixId(feilfortJournalpost), sak1001),
                    HttpMethod.GET,
                    null,
                    listeMedAvvikTyper(),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(avvikshendelserResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable { Assertions.assertThat(avvikshendelserResponse.body).`as`("resultat").contains(AvvikType.FEILFORE_SAK) },
            )
        }

        @Test
        @DisplayName("skal ikke finne avviket FEILFORE_SAK når journalposten allerede er feilført")
        fun skalIkkeFinneFeilforeSakForAlleredeFeilfortJournalpost() {
            val sak1001 = "1001"
            val feilfortJournalpost =
                testDataManager.opprett(
                    JournalpostBygger.enJournalpostSomErFeilfort("1001").leggTilSaksnummer(sak1001),
                )
            val avvikshendelserResponse =
                httpHeaderTestRestTemplate
                    .exchange(
                        String.format(AVVIK_PA_JP_MED_SAK, prefixId(feilfortJournalpost), sak1001),
                        HttpMethod.GET,
                        null,
                        listeMedAvvikTyper(),
                    )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(avvikshendelserResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable { Assertions.assertThat(avvikshendelserResponse.body).`as`("resultat").contains(AvvikType.FEILFORE_SAK) },
            )
        }

        @Test
        @DisplayName("skal ikke finne avvik for elektronisk innsendte journalposter")
        fun skalIkkeFinneAvvikForElektroniskInnsendteJournalposter() {
            val elektroniskInnsendtJournalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .medSkannetDato(LocalDate.now())
                        .utenOrginalBestilt()
                        .medBatchNavn("BJOARK015")
                        .leggTilSaksnummer("1337"),
                )
            val avvikshendelserResponse =
                httpHeaderTestRestTemplate.getForEntity<List<AvvikType>>(
                    String.format(
                        AVVIK_PA_JOURNALPOST,
                        prefixId(elektroniskInnsendtJournalpost),
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(avvikshendelserResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(
                            avvikshendelserResponse.body,
                        ).`as`("resultat")
                        .doesNotContain(AvvikType.BESTILL_ORIGINAL)
                },
            )
        }

        @Test
        @DisplayName("skal finne avvik for manuelt innsendte journalposter")
        fun skalFinneAvvikForManueltInnsendteJournalposter() {
            val manueltInnsendtJournalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .medSkannetDato(LocalDate.now())
                        .utenOrginalBestilt()
                        .leggTilSaksnummer("1337"),
                )
            val avvikshendelserResponse =
                httpHeaderTestRestTemplate
                    .exchange(
                        String.format(AVVIK_PA_JP_MED_SAK, prefixId(manueltInnsendtJournalpost), "1337"),
                        HttpMethod.GET,
                        null,
                        listeMedAvvikTyper(),
                    )
            org.junit.jupiter.api.Assertions.assertAll(
                { Assertions.assertThat(avvikshendelserResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                { Assertions.assertThat(avvikshendelserResponse.body).`as`("resultat").contains(AvvikType.BESTILL_ORIGINAL) },
            )
        }
    }

    @Nested
    @DisplayName("behandle")
    internal inner class Behandle {
        @Test
        @DisplayName("skal ha http status (NOT_FOUND) når man prøver å registrere avvik på en journalpost som ikke eksisterer")
        fun skalFaNotFoundVedRegistreringAvAvvikPaJournalpostSomIkkeEksisterer() {
            val registrerJpResponse =
                httpHeaderTestRestTemplate.postForEntity<Unit>(
                    String.format(AVVIK_PA_JOURNALPOST, "BID-12345"),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.BESTILL_ORIGINAL.name, "4802"),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        @DisplayName("skal ha http status 400 (BAD_REQUEST) når man prøver å behandle avvik på journalpost med feil prefix i id")
        fun skalFaBadRequstNarBehandlingAvAvvikHarJournalpostIdMedFeilPrefix() {
            val registrerJpResponse =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, "1"),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.BESTILL_RESKANNING.name, "123"),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("skal få BAD_REQUEST ved ukjent avvikstype")
        fun skalVareHttpStatusBadRequestNarAvvikstypeErUkjent() {
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, "BID-123"),
                    initHttpEntity(
                        Avvikshendelse("UKJENT", "123"),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.BAD_REQUEST) },
                Executable { Assertions.assertThat(responseEntity.body).`as`("response body").isNull() },
            )
        }

        @Test
        @DisplayName(
            "skal ha http status (BAD_REQUEST) når man prøver å registrere avvik på en journalført journalpost uten å oppgi saksnummer",
        )
        fun skalFaBadRequstAvRegistreringAvAvvikPaJournalpostUtenEnhetsnummerForRegistreringen() {
            val journalpost = testDataManager.opprett(JournalpostBygger.enJournalfortJournalpost().medFagomrade(Fagomrade.BIDRAG))
            val registrerJpResponse =
                httpHeaderTestRestTemplate.postForEntity<Unit>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(Avvikshendelse(AvvikType.BESTILL_ORIGINAL.name, "", null)),
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("skal opprette en oppgave for enheten som requesten tilhører")
        fun skalOppretteOppgaveForEnhetenSomRequestenTilhorer() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .medSkannetDato(LocalDate.now())
                        .utenOrginalBestilt()
                        .leggTilSaksnummer(sak1337),
                )
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.postForEntity(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(
                            HttpEntity::class.java,
                        ),
                        ArgumentMatchers.eq(OpprettOppgaveResponse::class.java),
                    ),
                ).thenReturn(ResponseEntity(OpprettOppgaveResponse(101L, "", "", "123", "MOT", "SR"), HttpStatus.CREATED))
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.BESTILL_ORIGINAL.name, "", sak1337),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable {
                    val oppgaveCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
                    Mockito.verify(httpHeaderRestTemplateMock).postForEntity(
                        ArgumentMatchers.anyString(),
                        oppgaveCaptor.capture(),
                        ArgumentMatchers.eq(
                            OpprettOppgaveResponse::class.java,
                        ),
                    )
                    val oppgave = oppgaveCaptor.value.body
                    Assertions.assertThat(oppgave).`as`("oppgave").isNotNull
                    Assertions.assertThat((oppgave as Oppgave).opprettetAvEnhetsnr).`as`("oppgave.opprettetAvEnhetsnr").isEqualTo("1001")
                },
            )
        }

        @Test
        @DisplayName("skal behandle avvik for SLETT_JOURNALPOST")
        fun skalBehandleAvvikForSlettJournalpost() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger.enJournalfortJournalpost().medJournalstatus("D").leggTilSaksnummer(sak1337),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.SLETT_JOURNALPOST.name, "", sak1337),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
            val opprettOppgaveResponse = responseEntity.body
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(opprettOppgaveResponse)
                        .extracting { it!!.avvikType }
                        .`as`("avvikType")
                        .isEqualTo(AvvikType.SLETT_JOURNALPOST.name)
                },
                Executable {
                    Assertions
                        .assertThat(opprettOppgaveResponse)
                        .extracting<String> { it!!.oppgavetype }
                        .`as`("oppgavetype")
                        .isNull()
                },
            )
        }

        @Test
        @DisplayName("skal sette journalstatus til F hvis alle sakstilknytnigner er feilført")
        fun skalSetteJournalpostStatusTilFVedFeilførSak() {
            val sak1001001 = "1001001"
            val journalpost = testDataManager.opprett(JournalpostBygger.enJournalfortJournalpost().leggTilSaksnummer(sak1001001))
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.FEILFORE_SAK.name, "", sak1001001),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status, behandlet avvik").isEqualTo(HttpStatus.OK)
            val jpResponse =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(
                        "/journal/%s?saksnummer=%s",
                        prefixId(journalpost),
                        sak1001001,
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(jpResponse.statusCode).`as`("status, hent journalpost").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(jpResponse.body)
                        .`as`("hentet journalpost response")
                        .isNotNull
                        .extracting<JournalpostDto> { it!!.journalpost }
                        .extracting<Boolean> { it.feilfort }
                        .`as`("feilfort")
                        .isEqualTo(true)
                },
                Executable {
                    Assertions
                        .assertThat(jpResponse.body)
                        .`as`("hentet journalpost response")
                        .isNotNull
                        .extracting<JournalpostDto> { it!!.journalpost }
                        .extracting<String> { it.journalstatus }
                        .`as`("journalstatus")
                        .isEqualTo("F")
                },
            )
        }

        @Test
        @DisplayName("skal ikke sette journalstatus til F hvis journalpost har saker som ikke er feilført")
        fun skalIkkeSetteJournalpostStatusTilFVedFeilførSakHvisFlereSaker() {
            val sak1001001 = "1001001"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger.enJournalfortJournalpost().leggTilSaksnummer(sak1001001).leggTilSaksnummer("213123123"),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.FEILFORE_SAK.name, "", sak1001001),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status, behandlet avvik").isEqualTo(HttpStatus.OK)
            val jpResponse =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(
                        "/journal/%s?saksnummer=%s",
                        prefixId(journalpost),
                        sak1001001,
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(jpResponse.statusCode).`as`("status, hent journalpost").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(jpResponse.body)
                        .`as`("hentet journalpost response")
                        .isNotNull
                        .extracting<JournalpostDto> { it!!.journalpost }
                        .extracting<Boolean> { it.feilfort }
                        .`as`("feilfort")
                        .isEqualTo(true)
                },
                Executable {
                    Assertions
                        .assertThat(jpResponse.body)
                        .`as`("hentet journalpost response")
                        .isNotNull
                        .extracting<JournalpostDto> { it!!.journalpost }
                        .extracting<String> { it.journalstatus }
                        .`as`("journalstatus")
                        .isEqualTo("J")
                },
            )
        }

        @Test
        @DisplayName("skal opprette journalpost, feilføre den, deretter ikke finne avvikstype FEILFORE_SAK")
        fun skalOppretteFeilforeOgIkkeFinneAvvikstype() {
            val sak1001001 = "1001001"
            val journalpost = testDataManager.opprett(JournalpostBygger.enJournalfortJournalpost().leggTilSaksnummer(sak1001001))
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.FEILFORE_SAK.name, "", sak1001001),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status, behandlet avvik").isEqualTo(HttpStatus.OK)
            val avvikResponse = responseEntity.body
            val avvikshendelserResponse =
                httpHeaderTestRestTemplate.getForEntity<List<AvvikType>>(
                    String.format(AVVIK_PA_JP_MED_SAK, prefixId(journalpost), sak1001001),
                )
            val jpResponse =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(
                        "/journal/%s?saksnummer=%s",
                        prefixId(journalpost),
                        sak1001001,
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(avvikResponse)
                        .extracting { it!!.avvikType }
                        .`as`("avvikType")
                        .isEqualTo(AvvikType.FEILFORE_SAK.name)
                },
                Executable {
                    Assertions
                        .assertThat(
                            avvikshendelserResponse.statusCode,
                        ).`as`("status, hent avvik")
                        .isEqualTo(HttpStatus.OK)
                },
                Executable { Assertions.assertThat(jpResponse.statusCode).`as`("status, hent journalpost").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(
                            avvikshendelserResponse.body,
                        ).`as`("avvikstyper")
                        .doesNotContain(AvvikType.FEILFORE_SAK)
                },
                Executable {
                    Assertions
                        .assertThat(jpResponse.body)
                        .`as`("hentet journalpost response")
                        .isNotNull
                        .extracting<JournalpostDto> { it!!.journalpost }
                        .extracting<Boolean> { it.feilfort }
                        .`as`("feilfort")
                        .isEqualTo(true)
                },
            )
        }

        @Test
        @DisplayName("skal få BAD_REQUEST når TREKK_JOURNALPOST gjøres på journalpost som er slettet")
        fun skalFraBadRequestNarTrekkJournalpostGjoresPaAlleredeTrukketJournalpost() {
            val journalpost = testDataManager.opprett(JournalpostBygger.enJournalpost().utenSak().medJournalstatus(Journalstatus.SLETTET))
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.TREKK_JOURNALPOST.name, "123"),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.BAD_REQUEST) },
                Executable { Assertions.assertThat(responseEntity.body).`as`("response body").isNull() },
                Executable {
                    val headers = responseEntity.headers
                    Assertions.assertThat(headers).`as`("headers").isNotNull
                    val warningHeader = headers[HttpHeaders.WARNING]
                    Assertions.assertThat(warningHeader).`as`("warning header").isNotNull
                    Assertions
                        .assertThat(warningHeader!!.stream().findFirst())
                        .isEqualTo(Optional.of(String.format("Ugyldig behandling: %s", StatusAvviksbehandling.ER_IKKE_MOTTAKSREGISTRERT)))
                },
            )
        }
    }

    @Nested
    @DisplayName("BESTILL_ORIGINAL")
    internal inner class BestillOriginal {
        @Test
        @DisplayName("skal ikke finne avvik for å bestille orginal når orginal allerede er bestilt for journalpost")
        fun skalIkkeKunneFinneBestilleOrginalNarOrginalErBestiltTidligere() {
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger.enJournalfortJournalpost().leggTilSaksnummer("1001001").medOrginalBestilt(),
                )
            val avvikshendelserResponse =
                httpHeaderTestRestTemplate.getForEntity<List<AvvikType>>(String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)))
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(avvikshendelserResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(
                            avvikshendelserResponse.body,
                        ).`as`("resultat")
                        .doesNotContain(AvvikType.BESTILL_ORIGINAL)
                },
            )
        }

        @Test
        @DisplayName("skal opprette BESTILL_ORIGINAL på en journalpost")
        fun skalOppretteBestillOriginalPaJournalpost() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .medSkannetDato(LocalDate.now())
                        .leggTilSaksnummer(sak1337)
                        .medGjelder("3123213")
                        .utenOrginalBestilt(),
                )
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.postForEntity(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(
                            HttpEntity::class.java,
                        ),
                        ArgumentMatchers.eq(OpprettOppgaveResponse::class.java),
                    ),
                ).thenReturn(ResponseEntity(OpprettOppgaveResponse(101L, "", "", "123", "MOT", "SR"), HttpStatus.CREATED))
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.exchange(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.eq(HttpMethod.POST),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.eq(
                            PersonDto::class.java,
                        ),
                    ),
                ).thenReturn(
                    ResponseEntity(
                        PersonDto(
                            Personident("123213"),
                            "aaaaaa",
                            "",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "aaaaaa",
                            null,
                            null,
                            null,
                            "aaaaaa",
                        ),
                        HttpStatus.CREATED,
                    ),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.BESTILL_ORIGINAL.name, "", sak1337),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(responseEntity.body)
                        .extracting { it!!.avvikType }
                        .`as`("avvikType.name")
                        .isEqualTo(AvvikType.BESTILL_ORIGINAL.name)
                },
            )
        }

        @Test
        @DisplayName("skal behandle BESTILL_ORIGINAL på en journalpost")
        fun skalBehandleBestillOriginalPaJournalpost() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .medSkannetDato(LocalDate.now())
                        .leggTilSaksnummer(sak1337)
                        .utenOrginalBestilt(),
                )
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.postForEntity(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(
                            HttpEntity::class.java,
                        ),
                        ArgumentMatchers.eq(OpprettOppgaveResponse::class.java),
                    ),
                ).thenReturn(ResponseEntity(OpprettOppgaveResponse(101L, "", "", "123", "MOT", "SR"), HttpStatus.CREATED))
            val avvikshendelseBestillOriginal =
                """
                {
                  "avvikType":"BESTILL_ORIGINAL",
                  "detaljer":{
                    "enhetsnummer":""
                  },
                  "saksnummer":"$sak1337"}
                
                """.trimIndent()
            println(avvikshendelseBestillOriginal)
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        avvikshendelseBestillOriginal,
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(responseEntity.body)
                        .`as`("response")
                        .isNotNull
                        .`as`("response.avvikType.name")
                        .extracting { it!!.avvikType }
                        .isEqualTo(AvvikType.BESTILL_ORIGINAL.name)
                },
            )
        }

        @Test
        @DisplayName("skal opprette en oppgave for BESTILL_ORIGINAL på en journalpost")
        fun skalOppretteOppgaveForBestillOriginalPaJournalpost() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medSkannetDato(LocalDate.now())
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .leggTilSaksnummer(sak1337)
                        .utenOrginalBestilt(),
                )
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.postForEntity(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(
                            HttpEntity::class.java,
                        ),
                        ArgumentMatchers.eq(OpprettOppgaveResponse::class.java),
                    ),
                ).thenReturn(ResponseEntity(OpprettOppgaveResponse(101L, "", "", "123", "MOT", "SR"), HttpStatus.CREATED))
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.BESTILL_ORIGINAL.name, "", sak1337),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable {
                    val behandleAvvikshendelseResponse = responseEntity.body
                    Assertions.assertThat(behandleAvvikshendelseResponse).`as`("BehandleAvvikshendelseResponse").isNotNull
                    org.junit.jupiter.api.Assertions.assertAll(
                        Executable {
                            Assertions
                                .assertThat(behandleAvvikshendelseResponse!!.avvikType)
                                .`as`("avvikType")
                                .isEqualTo(Avvikstype.BESTILL_ORIGINAL.name)
                        },
                        Executable { Assertions.assertThat(behandleAvvikshendelseResponse!!.oppgaveId).`as`("oppgaveId").isEqualTo(101L) },
                        Executable {
                            Assertions
                                .assertThat(
                                    behandleAvvikshendelseResponse!!.oppgavetype,
                                ).`as`("oppgavetype")
                                .isEqualTo("SR")
                        },
                        Executable { Assertions.assertThat(behandleAvvikshendelseResponse!!.tema).`as`("tema").isEqualTo("MOT") },
                        Executable {
                            Assertions
                                .assertThat(
                                    behandleAvvikshendelseResponse!!.tildeltEnhetsnr,
                                ).`as`("tildelesEnhetsnr")
                                .isEqualTo("123")
                        },
                    )
                },
            )
        }

        @Test
        @DisplayName("skal berike avvik BESTILL_ORIGINAL med informasjon om saksbehandler")
        fun skalBerikeAvvikBestillOrginalMedInformasjonOmSaksbehandler() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .medSkannetDato(LocalDate.now())
                        .utenOrginalBestilt()
                        .leggTilSaksnummer(sak1337)
                        .medGjelder("asdsad"),
                )
            Mockito.`when`(saksbehandlerOidcTokenManagerMock.hentSaksbehandler()).thenReturn("tt")
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.postForEntity(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(
                            HttpEntity::class.java,
                        ),
                        ArgumentMatchers.eq(OpprettOppgaveResponse::class.java),
                    ),
                ).thenReturn(ResponseEntity(OpprettOppgaveResponse(101L, "", "", "123", "MOT", "SR"), HttpStatus.CREATED))
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.exchange(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.eq(HttpMethod.GET),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.eq(
                            Saksbehandler::class.java,
                        ),
                    ),
                ).thenReturn(ResponseEntity(Saksbehandler("tt", "Tore Tang"), HttpStatus.OK))
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.exchange(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.eq(HttpMethod.POST),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.eq(
                            PersonDto::class.java,
                        ),
                    ),
                ).thenReturn(
                    ResponseEntity(
                        PersonDto(
                            Personident("123213"),
                            null,
                            "",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "aaaaaa",
                            null,
                            null,
                            null,
                            "",
                        ),
                        HttpStatus.CREATED,
                    ),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.BESTILL_ORIGINAL.name, "", sak1337),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable {
                    val oppgaveCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
                    Mockito.verify(httpHeaderRestTemplateMock).postForEntity(
                        ArgumentMatchers.anyString(),
                        oppgaveCaptor.capture(),
                        ArgumentMatchers.eq(
                            OpprettOppgaveResponse::class.java,
                        ),
                    )
                    val oppgave = oppgaveCaptor.value.body
                    Assertions.assertThat(oppgave).`as`("oppgave").isNotNull
                    Assertions
                        .assertThat(
                            (oppgave as Oppgave).beskrivelse,
                        ).`as`("oppgave.beskrivelse")
                        .contains("og merkes med tt - Tore Tang")
                    Assertions.assertThat(oppgave.aktoerId).`as`("oppgave.aktoerId").isEqualTo("aaaaaa")
                },
            )
        }

        @Test
        @DisplayName("skal opprette avvik for bestill original")
        fun skalOppretteAvvikForBestillOriginal() {
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalpost()
                        .medBatchNavn("batchen")
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .medJournalstatus(Journalstatus.MOTTAKSREGISTRERT)
                        .medSkannetDato(LocalDate.now())
                        .utenOrginalBestilt(),
                )
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.postForEntity(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.eq(
                            OpprettOppgaveResponse::class.java,
                        ),
                    ),
                ).thenReturn(
                    ResponseEntity(
                        OpprettOppgaveResponse(java.lang.Long.valueOf(journalpost.journalpostId.toLong()), null, null, null, null, null),
                        HttpStatus.CREATED,
                    ),
                )
            val pathAvvik = String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost))
            val avvikshendelse =
                """
                {
                  "avvikType": "BESTILL_ORIGINAL",
                  "detaljer": {
                    "enhetsnummer":"1001"
                  }
                }
                
                """.trimIndent()
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    pathAvvik,
                    initHttpEntity(avvikshendelse, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status, behandlet avvik").isEqualTo(HttpStatus.OK)
            val avvikResponse = responseEntity.body
            val hentResponse = httpHeaderTestRestTemplate.getForEntity<List<AvvikType>>(pathAvvik)
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(avvikResponse)
                        .extracting { it!!.avvikType }
                        .`as`("avvikType")
                        .isEqualTo(AvvikType.BESTILL_ORIGINAL.name)
                },
                Executable {
                    Assertions
                        .assertThat(
                            hentResponse.statusCode,
                        ).`as`("status, hent avvik på journalpost")
                        .isEqualTo(HttpStatus.OK)
                },
                Executable {
                    Assertions
                        .assertThat(hentResponse.body)
                        .`as`("response, det er ikke mulig å bestille original")
                        .doesNotContain(AvvikType.BESTILL_ORIGINAL)
                },
            )
        }
    }

    @Nested
    @DisplayName("mottaksregistrert journalpost")
    internal inner class MottaksregistrertJournalpost {
        @Test
        @DisplayName("skal hente hente avvik for endre fagomrade")
        fun skalHenteAvvikForEndreFagomrade() {
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalpost()
                        .medJournalstatus(Journalstatus.MOTTAKSREGISTRERT)
                        .medFagomrade("FAR"),
                )
            val response =
                httpHeaderTestRestTemplate
                    .exchange(
                        String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                        HttpMethod.GET,
                        null,
                        listeMedAvvikTyper(),
                    )
            org.junit.jupiter.api.Assertions.assertAll(
                { Assertions.assertThat(response.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                {
                    val avvikstyper = response.body
                    Assertions.assertThat(avvikstyper).`as`("avvikstyper").contains(AvvikType.ENDRE_FAGOMRADE)
                },
            )
        }

        @Test
        @DisplayName("skal ha http status 400 (BAD_REQUEST) når man prøver å registrere avvik på en journalpost med feil prefix i id")
        fun skalFaBadRequstAvRegistreringAvAvvikPaJournalpostMedFeilJournalpostIdPrefix() {
            val registrerJpResponse =
                httpHeaderTestRestTemplate.exchange<Void>(
                    String.format(AVVIK_PA_JOURNALPOST, "BID-A"),
                    HttpMethod.POST,
                    HttpEntity(Avvikshendelse("")),
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName(
            "skal ha http status (BAD_REQUEST) når man prøver å registrere avvik på en journalpost uten å oppgi enhetsnummer for retgistreringen",
        )
        fun skalFaBadRequstAvRegistreringAvAvvikPaJournalpostUtenEnhetsnummerForRegistreringen() {
            val journalpost = testDataManager.opprett(JournalpostBygger.enJournalpost().medJournalstatus(Journalstatus.MOTTAKSREGISTRERT))
            val registrerJpResponse =
                httpHeaderTestRestTemplate.postForEntity<Unit>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(Avvikshendelse("", "")),
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("skal opprette avvik for endret fagomrade fra BID til FAR")
        fun skalOppretteAvvikForEndretFagomradeFraBidTilFar() {
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalpost()
                        .medFagomrade(Fagomrade.BIDRAG)
                        .medJournalforendeEnhet("4806")
                        .medJournalstatus(Journalstatus.MOTTAKSREGISTRERT),
                )
            val pathAvvik = String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost))
            val avvikshendelse =
                """
                {
                  "avvikType":"ENDRE_FAGOMRADE",
                  "detaljer": {
                    "fagomrade":"FAR"
                  }
                }
                
                """.trimIndent()
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    pathAvvik,
                    initHttpEntity(avvikshendelse, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status, behandlet avvik").isEqualTo(HttpStatus.OK)
            val avvikResponse = responseEntity.body
            val hentResponse =
                httpHeaderTestRestTemplate
                    .exchange(
                        pathAvvik,
                        HttpMethod.GET,
                        null,
                        listeMedAvvikTyper(),
                    )
            org.junit.jupiter.api.Assertions.assertAll(
                {
                    Assertions
                        .assertThat(avvikResponse)
                        .extracting { it!!.avvikType }
                        .`as`("avvikType")
                        .isEqualTo(AvvikType.ENDRE_FAGOMRADE.name)
                },
                {
                    Assertions
                        .assertThat(
                            hentResponse.statusCode,
                        ).`as`("status, hent avvik på journalpost")
                        .isEqualTo(HttpStatus.OK)
                },
                {
                    Assertions
                        .assertThat(
                            hentResponse.body,
                        ).`as`("response, kan fremdeles endre fagomrade")
                        .contains(AvvikType.ENDRE_FAGOMRADE)
                },
            )
        }
    }

    @Nested
    @DisplayName("oppdatere status på arkivering av journalpost i Joark")
    internal inner class OppdatereJoarkArkiveringStatus {
        private fun oppretteJournalpost(
            saksnr: String,
            journalstatus: String,
        ): Journalpost = testDataManager.opprett(
            JournalpostBygger
                .enJournalfortJournalpost()
                .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                .medJournalstatus(journalstatus)
                .medBatchNavn("IKKE BJOARK BATCH")
                .medFilnavn("dokumentet.pdf")
                .medSkannetDato(LocalDate.now())
                .leggTilSaksnummer(saksnr),
        )

        @Test
        @DisplayName("skal ha http status 400 (BAD_REQUEST) dersom oppgitt avvikstype er ugyldig")
        fun skalHaHttpStatusBadRequestVedUgyldigAvvikstype() {
            // given
            val saksnr = "1900000"
            val saksbehandlersPaaloggedeEnhet = "4802"
            val enhetsnummerTilAvviksbehandler = "9999"
            val ugyldigAvvikstype = AvvikType.ARKIVERE_JOURNALPOST.toString() + "UGYLDIG!"
            val journalpost = oppretteJournalpost(saksnr, Journalstatus.KLAR_TIL_PRINT)

            // when
            val response =
                httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(ugyldigAvvikstype, enhetsnummerTilAvviksbehandler),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, saksbehandlersPaaloggedeEnhet),
                    ),
                )

            // then
            Assertions.assertThat(response).extracting { it.statusCode }.isEqualTo(
                HttpStatus.BAD_REQUEST,
            )
        }

        @Test
        @DisplayName("skal gi http status NOT_FOUND dersom oppgitt journalpost ikke finnes")
        fun skalGiNotFoundDersomJournalpostIkkeFinnes() {
            // given
            val ukjentJpId = "123"
            val saksbehandlersPaaloggedeEnhet = "4802"
            val enhetsnummerTilAvviksbehandler = "9999"
            val gyldigAvvikstype = AvvikType.ARKIVERE_JOURNALPOST.name

            // when
            val registrerJpResponse =
                httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
                    String.format(AVVIK_PA_JOURNALPOST, "BID-$ukjentJpId"),
                    initHttpEntity(
                        Avvikshendelse(gyldigAvvikstype, enhetsnummerTilAvviksbehandler),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, saksbehandlersPaaloggedeEnhet),
                    ),
                )

            // then
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        @DisplayName("skal gi http status NOT_FOUND dersom oppgitt sak ikke finnes")
        fun skalGiNotFoundDersomSakIkkeFinnes() {
            // given
            val ukjentSaksnr = "8888888"
            val kjentSaksnr = "1900000"
            val saksbehandlersPaaloggedeEnhet = "4802"
            val joarkArkiveringStatus = JoarkArkiveringStatus.FULLFORT.toString()
            val joarkJournalpostId = "123456"
            val journalpost = oppretteJournalpost(kjentSaksnr, Journalstatus.KLAR_TIL_PRINT)

            // when
            val registrerJpResponse =
                httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(
                            AvvikType.ARKIVERE_JOURNALPOST.name,
                            saksbehandlersPaaloggedeEnhet,
                            mapOf("joarkArkiveringStatus" to joarkArkiveringStatus, "joarkJournalpostId" to joarkJournalpostId),
                            ukjentSaksnr,
                        ),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, saksbehandlersPaaloggedeEnhet),
                    ),
                )

            // then
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        @DisplayName("skal gi BAD_REQUEST dersom journalstatus ikke er klar til print")
        fun skalGiBadRequestDersomJournalstatusIkkeErKlarTilPrint() {
            // given
            val saksnr = "1900000"
            val saksbehandlersPaaloggedeEnhet = "4802"
            val journalpost = oppretteJournalpost(saksnr, Journalstatus.MOTTAKSREGISTRERT)

            // when
            val registrerJpResponse =
                httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(
                            AvvikType.ARKIVERE_JOURNALPOST.name,
                            null,
                            mapOf(
                                "joarkArkiveringStatus" to JoarkArkiveringStatus.STARTET.toString(),
                                ENHETSNUMMER to saksbehandlersPaaloggedeEnhet,
                            ),
                            saksnr,
                        ),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, saksbehandlersPaaloggedeEnhet),
                    ),
                )

            // then
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("skal returnere httpstatus 200 og avvikstype ARKIVERE_JOURNALPOST dersom angitt joarkArkiveringStatus er STARTET")
        fun skalReturnereOkDersomJoarkArkiveringStatusErStartet() {
            // given
            val saksnr = "1900000"
            val saksbehandlersPaaloggedeEnhet = "4802"
            val journalpost = oppretteJournalpost(saksnr, Journalstatus.KLAR_TIL_PRINT)
            val joarkArkiveringStatus = JoarkArkiveringStatus.STARTET
            val avviktype = AvvikType.ARKIVERE_JOURNALPOST.name

            // when
            val registrerJpResponse =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(
                            avviktype,
                            saksbehandlersPaaloggedeEnhet,
                            mapOf("joarkArkiveringStatus" to joarkArkiveringStatus.toString()),
                            saksnr,
                        ),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, saksbehandlersPaaloggedeEnhet),
                    ),
                )

            // then
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(registrerJpResponse.body)
                        .extracting { it!!.avvikType }
                        .isEqualTo(avviktype)
                },
            )
        }

        @Test
        @DisplayName("skal returnere httpstatus 200 og avvikstype ARKIVERE_JOURNALPOST dersom angitt joarkArkiveringStatus er FULLFORT")
        fun skalReturnereOkDersomJoarkArkiveringStatusErFullfort() {
            // given
            val saksnr = "1900000"
            val saksbehandlersPaaloggedeEnhet = "4802"
            val journalpost = oppretteJournalpost(saksnr, Journalstatus.KLAR_TIL_PRINT)
            val joarkArkiveringStatus = JoarkArkiveringStatus.FULLFORT
            val joarkJournalpostId = "123456"
            val avviktype = AvvikType.ARKIVERE_JOURNALPOST.name

            // when
            val registrerJpResponse =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(
                            avviktype,
                            saksbehandlersPaaloggedeEnhet,
                            mapOf("joarkArkiveringStatus" to joarkArkiveringStatus.toString(), "joarkJournalpostId" to joarkJournalpostId),
                            saksnr,
                        ),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, saksbehandlersPaaloggedeEnhet),
                    ),
                )
            val oppdatertJournalpost = testDataManager.hent(journalpost.journalpostId, Journalpost::class.java)

            // then
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(oppdatertJournalpost.get().journalstatus).isEqualTo(Journalstatus.EKSPEDERT_JOARK) },
                Executable {
                    Assertions
                        .assertThat(
                            oppdatertJournalpost.get().journalsaker[0].joarkJpId,
                        ).isEqualTo(Integer.valueOf(joarkJournalpostId))
                },
                Executable { Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(registrerJpResponse.body)
                        .extracting { it!!.avvikType }
                        .isEqualTo(avviktype)
                },
            )
        }

        @Test
        @DisplayName("skal returnere httpstatus 200 og avvikstype ARKIVERE_JOURNALPOST dersom angitt joarkArkiveringStatus er FEILET")
        fun skalReturnereOkDersomJoarkArkiveringStatusErFeilet() {
            // given
            val saksnr = "1900000"
            val saksbehandlersPaaloggedeEnhet = "4802"
            val journalpost = oppretteJournalpost(saksnr, Journalstatus.KLAR_TIL_PRINT)
            val joarkArkiveringStatus = JoarkArkiveringStatus.FEILET
            val avviktype = AvvikType.ARKIVERE_JOURNALPOST.name

            // when
            val registrerJpResponse =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(
                            avviktype,
                            saksbehandlersPaaloggedeEnhet,
                            mapOf("joarkArkiveringStatus" to joarkArkiveringStatus.toString()),
                            saksnr,
                        ),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, saksbehandlersPaaloggedeEnhet),
                    ),
                )

            // then
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(registrerJpResponse.body)
                        .extracting { it!!.avvikType }
                        .isEqualTo(avviktype)
                },
            )
        }
    }

    @Nested
    @DisplayName("REGISTRER_RETUR")
    internal inner class RegistrerRetur {
        @Test
        @DisplayName("skal finne avviket REGISTRER_RETUR når journalpost er utgaaende og reservert")
        fun skalFinneAvvikNarJpErMottaksregistrert() {
            testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.RESERVERT)
            val sak1337 = "1337"
            val enMottaksregistrertJournalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalpost()
                        .medJournalstatus("R")
                        .medDokumentType(DokumentType.UTGAAENDE_DOKUMENT)
                        .leggTilSaksnummer(sak1337),
                )
            val avvikshendelserResponse =
                httpHeaderTestRestTemplate
                    .exchange(
                        String.format(
                            AVVIK_PA_JP_MED_SAK,
                            prefixId(enMottaksregistrertJournalpost),
                            sak1337,
                        ),
                        HttpMethod.GET,
                        null,
                        listeMedAvvikTyper(),
                    )
            org.junit.jupiter.api.Assertions.assertAll(
                { Assertions.assertThat(avvikshendelserResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                {
                    Assertions
                        .assertThat(
                            avvikshendelserResponse.body,
                        ).`as`("avvikshendelser")
                        .contains(AvvikType.REGISTRER_RETUR)
                },
            )
        }

        @Test
        @DisplayName("skal behandle avvik for REGISTRER_RETUR")
        fun skalBehandleAvvikForRegistrerReturNarJournalpostHarIngenRetur() {
            val sak1337 = "1337"
            val returBeskrivelse = "Retur beskrivelse NANANANA"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medDokumentType("U")
                        .medJournalstatus("R")
                        .medFagomrade(Fagomrade.BIDRAG)
                        .leggTilSaksnummer(sak1337),
                )
            val returDato = LocalDate.parse("2020-01-02")
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        AvvikshendelseBuilder
                            .enAvvikshendelse()
                            .med(AvvikType.REGISTRER_RETUR)
                            .medBeskrivelse(returBeskrivelse)
                            .medReturDato(returDato)
                            .medSaksnummer(sak1337)
                            .bygg(),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            val jpResponseEntity =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(
                        "/journal/%s?saksnummer=%s",
                        prefixId(journalpost),
                        sak1337,
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
            val opprettOppgaveResponse = responseEntity.body
            val jpReturDetaljer = jpResponseEntity.body?.journalpost!!.returDetaljer
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(opprettOppgaveResponse)
                        .extracting { it!!.avvikType }
                        .`as`("avvikType")
                        .isEqualTo(AvvikType.REGISTRER_RETUR.name)
                },
                Executable {
                    Assertions
                        .assertThat(opprettOppgaveResponse)
                        .extracting<String> { it!!.oppgavetype }
                        .`as`("oppgavetype")
                        .isNull()
                },
                Executable { Assertions.assertThat(jpReturDetaljer!!.antall).isEqualTo(1) },
                Executable { Assertions.assertThat(jpReturDetaljer!!.dato).isEqualTo(returDato) },
                Executable { Assertions.assertThat(jpReturDetaljer!!.logg!!.size).isEqualTo(1) },
                Executable { Assertions.assertThat(jpReturDetaljer!!.logg!![0].dato).isEqualTo(returDato) },
                Executable { Assertions.assertThat(jpReturDetaljer!!.logg!![0].beskrivelse).isEqualTo(returBeskrivelse) },
            )
        }

        @Test
        @DisplayName("skal behandle avvik for REGISTRER_RETUR for journalpost med eksisterende retur uten logg")
        fun skalBehandleAvvikForRegistrerReturForJournalpostMedEksisterendeReturUtenLogg() {
            val sak1337 = "1337"
            val returBeskrivelse = "Retur beskrivelse NANANANA"
            val existingReturDato = LocalDate.parse("2019-01-02")
            val returDato = LocalDate.parse("2020-01-02")
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medDokumentType("U")
                        .medJournalstatus("R")
                        .medReturDato(existingReturDato)
                        .medFagomrade(Fagomrade.BIDRAG)
                        .leggTilSaksnummer(sak1337),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        AvvikshendelseBuilder
                            .enAvvikshendelse()
                            .med(AvvikType.REGISTRER_RETUR)
                            .medBeskrivelse(returBeskrivelse)
                            .medReturDato(returDato)
                            .medSaksnummer(sak1337)
                            .bygg(),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            val jpResponseEntity =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(
                        "/journal/%s?saksnummer=%s",
                        prefixId(journalpost),
                        sak1337,
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
            val opprettOppgaveResponse = responseEntity.body
            val jpReturDetaljer = jpResponseEntity.body?.journalpost!!.returDetaljer
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(opprettOppgaveResponse)
                        .extracting { it!!.avvikType }
                        .`as`("avvikType")
                        .isEqualTo(AvvikType.REGISTRER_RETUR.name)
                },
                Executable {
                    Assertions
                        .assertThat(opprettOppgaveResponse)
                        .extracting<String> { it!!.oppgavetype }
                        .`as`("oppgavetype")
                        .isNull()
                },
                Executable { Assertions.assertThat(jpReturDetaljer!!.antall).isEqualTo(2) },
                Executable { Assertions.assertThat(jpReturDetaljer!!.dato).isEqualTo(returDato) },
                Executable { Assertions.assertThat(jpReturDetaljer!!.logg!!.size).isEqualTo(2) },
                Executable {
                    Assertions
                        .assertThat(
                            testDataManager.findReturDetaljerLogByDate(returDato, jpReturDetaljer!!.logg).beskrivelse,
                        ).isEqualTo(
                            returBeskrivelse,
                        )
                },
                Executable {
                    Assertions
                        .assertThat(testDataManager.findReturDetaljerLogByDate(existingReturDato, jpReturDetaljer!!.logg).beskrivelse)
                        .isEqualTo("")
                },
            )
        }

        @Test
        @DisplayName("skal behandle avvik for REGISTRER_RETUR for journalpost med eksisterende retur")
        fun skalBehandleAvvikForRegistrerReturForJournalpostMedEksisterendeRetur() {
            val sak1337 = "1337"
            val returBeskrivelse = "Retur beskrivelse NANANANA"
            val returDato = LocalDate.parse("2020-01-02")
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medDokumentType("U")
                        .medJournalstatus("R")
                        .medRetur(LocalDate.parse("2019-05-05"), ReturDetaljerLogg(LocalDate.parse("2019-05-05"), "asdsadasd"))
                        .medFagomrade(Fagomrade.BIDRAG)
                        .leggTilSaksnummer(sak1337),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        AvvikshendelseBuilder
                            .enAvvikshendelse()
                            .med(AvvikType.REGISTRER_RETUR)
                            .medBeskrivelse(returBeskrivelse)
                            .medReturDato(returDato)
                            .medSaksnummer(sak1337)
                            .bygg(),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            val jpResponseEntity =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(
                        "/journal/%s?saksnummer=%s",
                        prefixId(journalpost),
                        sak1337,
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
            val opprettOppgaveResponse = responseEntity.body
            val jpReturDetaljer = jpResponseEntity.body?.journalpost!!.returDetaljer
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(opprettOppgaveResponse)
                        .extracting { it!!.avvikType }
                        .`as`("avvikType")
                        .isEqualTo(AvvikType.REGISTRER_RETUR.name)
                },
                Executable {
                    Assertions
                        .assertThat(opprettOppgaveResponse)
                        .extracting<String> { it!!.oppgavetype }
                        .`as`("oppgavetype")
                        .isNull()
                },
                Executable { Assertions.assertThat(jpReturDetaljer!!.antall).isEqualTo(2) },
                Executable { Assertions.assertThat(jpReturDetaljer!!.dato).isEqualTo(returDato) },
                Executable { Assertions.assertThat(jpReturDetaljer!!.logg!!.size).isEqualTo(2) },
                Executable {
                    Assertions
                        .assertThat(
                            testDataManager.findReturDetaljerLogByDate(returDato, jpReturDetaljer!!.logg).beskrivelse,
                        ).isEqualTo(
                            returBeskrivelse,
                        )
                },
            )
        }

        @Test
        @DisplayName("skal feile med BAD_REQUEST når avvik REGISTRER_RETUR registreres for en eksisterende dato")
        fun skalFeileNaarAvvikRegistrerReturRegistreresForEksisterendeDato() {
            val sak1337 = "1337"
            val returBeskrivelse = "Retur beskrivelse NANANANA"
            val returDato = LocalDate.parse("2020-01-02")
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medDokumentType("U")
                        .medJournalstatus("R")
                        .medRetur(returDato, ReturDetaljerLogg(returDato, "asdsadasd"))
                        .medFagomrade(Fagomrade.BIDRAG)
                        .leggTilSaksnummer(sak1337),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        AvvikshendelseBuilder
                            .enAvvikshendelse()
                            .med(AvvikType.REGISTRER_RETUR)
                            .medBeskrivelse(returBeskrivelse)
                            .medReturDato(returDato)
                            .medSaksnummer(sak1337)
                            .bygg(),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("skal feile med BAD_REQUEST når avvik REGISTRER_RETUR registreres for journalpost uten status reservert")
        fun skalFeileNaarAvvikRegistrerReturRegistreresForJournalpostUtenStatusReservert() {
            val sak1337 = "1337"
            val returBeskrivelse = "Retur beskrivelse NANANANA"
            val returDato = LocalDate.parse("2020-01-02")
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medDokumentType("U")
                        .medJournalstatus("D")
                        .medRetur(returDato, ReturDetaljerLogg(returDato, "asdsadasd"))
                        .medFagomrade(Fagomrade.BIDRAG)
                        .leggTilSaksnummer(sak1337),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        AvvikshendelseBuilder
                            .enAvvikshendelse()
                            .med(AvvikType.REGISTRER_RETUR)
                            .medBeskrivelse(returBeskrivelse)
                            .medReturDato(returDato)
                            .medSaksnummer(sak1337)
                            .bygg(),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("ENDRE_FAGOMRADE")
    internal inner class EndreFagomrade {
        @Test
        @DisplayName("skal behandle avvik for ENDRE_FAGOMRADE")
        fun skalBehandleAvvikForEndreFagomrade() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medFagomrade(Fagomrade.BIDRAG)
                        .medJournalforendeEnhet("4806")
                        .leggTilSaksnummer(sak1337),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        AvvikshendelseBuilder
                            .enAvvikshendelse()
                            .med(
                                AvvikType.ENDRE_FAGOMRADE,
                            ).medNyttFagomrade(Fagomrade.FARSKAP)
                            .medSaksnummer(sak1337)
                            .bygg(),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
            val opprettOppgaveResponse = responseEntity.body
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(opprettOppgaveResponse)
                        .extracting { it!!.avvikType }
                        .`as`("avvikType")
                        .isEqualTo(AvvikType.ENDRE_FAGOMRADE.name)
                },
                Executable {
                    Assertions
                        .assertThat(opprettOppgaveResponse)
                        .extracting<String> { it!!.oppgavetype }
                        .`as`("oppgavetype")
                        .isNull()
                },
            )
        }

        @Test
        @DisplayName(
            "skal opprette avvik for endret fagomrade på journalpost som ikke er bidrag eller farskap. Fører til ny journalstatus som ikke er i journalen",
        )
        fun skalOppretteAvvikForEndretFagomradeSomIkkeEridragFarskapSomForerTilAtJournalpostenErFjernetFraJournal() {
            val sak1001001 = "1001001"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medFagomrade(Fagomrade.BIDRAG)
                        .medJournalforendeEnhet("4806")
                        .leggTilSaksnummer(sak1001001),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        AvvikshendelseBuilder
                            .enAvvikshendelse()
                            .med(
                                AvvikType.ENDRE_FAGOMRADE,
                            ).medNyttFagomrade("NYTT_FAGOMRADE")
                            .somErSendtScanning()
                            .medSaksnummer(sak1001001)
                            .bygg(),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status, behandlet avvik").isEqualTo(HttpStatus.OK)
            val avvikResponse = responseEntity.body
            val avvikshendelserResponse =
                httpHeaderTestRestTemplate.getForEntity<List<AvvikType>>(
                    String.format(AVVIK_PA_JP_MED_SAK, prefixId(journalpost), sak1001001),
                )
            val jpResponse =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(
                        "/journal/%s?saksnummer=%s",
                        prefixId(journalpost),
                        sak1001001,
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(avvikResponse)
                        .extracting { it!!.avvikType }
                        .`as`("avvikType")
                        .isEqualTo(AvvikType.ENDRE_FAGOMRADE.name)
                },
                Executable {
                    Assertions
                        .assertThat(
                            avvikshendelserResponse.statusCode,
                        ).`as`("status, hent avvik")
                        .isEqualTo(HttpStatus.OK)
                },
                Executable {
                    Assertions
                        .assertThat(avvikshendelserResponse.body)
                        .`as`("innhold, hent avvik")
                        .asList()
                        .isEmpty()
                },
                Executable { Assertions.assertThat(jpResponse.statusCode).`as`("status, hent journalpost").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(jpResponse.body)
                        .`as`("hentet journalpost response")
                        .isNotNull
                        .extracting<JournalpostDto> { it!!.journalpost }
                        .extracting<Boolean> { it.feilfort }
                        .`as`("feilfort")
                        .isEqualTo(true)
                },
            )
        }

        @Test
        @DisplayName(
            "skal endre fagområade til ikke å tilhøre bidragsområadet noe som fører til at journalposten ikke lenger er i journalen",
        )
        fun skalEndreFagomradeOgHentJournal() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .leggTilSaksnummer(sak1337)
                        .medJournalforendeEnhet("4806")
                        .medFagomrade(Fagomrade.BIDRAG),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        AvvikshendelseBuilder
                            .enAvvikshendelse()
                            .med(AvvikType.ENDRE_FAGOMRADE)
                            .medNyttFagomrade("AAP")
                            .somErSendtScanning()
                            .medSaksnummer(sak1337)
                            .bygg(),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
            val response =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(
                        "/journal/%s?saksnummer=%s",
                        prefixId(journalpost),
                        sak1337,
                    ),
                )
            Assertions
                .assertThat(response)
                .extracting { it.statusCode }
                .isEqualTo(
                    HttpStatus.OK,
                )
            Assertions
                .assertThat(response)
                .extracting { it.body }
                .extracting<JournalpostDto> { it!!.journalpost }
                .isNotNull
                .extracting<String> { it.journalstatus }
                .`as`("journalstatus")
                .isEqualTo(Journalstatus.AVVIK_ENDRE_FAGOMRADE)
        }

        @Test
        @DisplayName("skal feile når (BAD_REQUEST) endret verdi finnes i databasen før endring")
        fun skalFeileNarEndretVerdiFinnes() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger.enJournalfortJournalpost().leggTilSaksnummer(sak1337).medFagomrade("FAR"),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.ENDRE_FAGOMRADE.name, "FAR", mapOf(), sak1337),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("skal feile når (BAD_REQUEST) endret verdi har samme funksjonell verdi i databasen før endring")
        fun skalFeileNarEndretVerdiErFunksjoneltLike() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .leggTilSaksnummer(sak1337)
                        .medJournalforendeEnhet("4806")
                        .medFagomrade("BNR"),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.ENDRE_FAGOMRADE.name, null, mapOf("fagomrade" to Fagomrade.BIDRAG), sak1337),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(responseEntity)
                        .isNotNull
                        .extracting { HttpStatus.valueOf(it.statusCode.value()) }
                        .isEqualTo(HttpStatus.BAD_REQUEST)
                },
                Executable {
                    val headers = responseEntity.headers
                    Assertions.assertThat(headers.toSingleValueMap()).`as`("contains warning").containsKey(HttpHeaders.WARNING)
                    Assertions.assertThat(hentMuligAdvarsel(headers)).`as`("warning").hasValueSatisfying { advarsel: String? ->
                        Assertions.assertThat(advarsel).containsSequence("ugyldigForklaring=Ugyldig avvik: ENDRE_FAGOMRADE")
                    }
                },
            )
        }

        @Test
        @DisplayName("skal feile ending av fagomrade fra FAR til FAR")
        fun skalFeileNarFagomradeEndresFraFarTilFar() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medFagomrade("FAR")
                        .medJournalforendeEnhet("4806")
                        .leggTilSaksnummer(sak1337),
                )
            val avvikshendelse =
                String.format(
                    """
                    {
                      "avvikType":"ENDRE_FAGOMRADE",
                      "saksnummer":"%s",
                      "detaljer": {
                        "fagomrade":"FAR"
                      }
                    }
                    
                    """.trimIndent(),
                    sak1337,
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(avvikshendelse, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(responseEntity)
                        .`as`("behandlet avvik")
                        .isNotNull
                        .extracting { HttpStatus.valueOf(it.statusCode.value()) }
                        .isEqualTo(HttpStatus.BAD_REQUEST)
                },
                Executable {
                    val headers = responseEntity.headers
                    Assertions.assertThat(headers.toSingleValueMap()).`as`("contains warning").containsKey(HttpHeaders.WARNING)
                    Assertions.assertThat(hentMuligAdvarsel(headers)).`as`("warning").hasValueSatisfying { advarsel: String? ->
                        Assertions.assertThat(advarsel).containsSequence("ugyldigForklaring=Ugyldig avvik: ENDRE_FAGOMRADE")
                    }
                },
            )
        }

        @Test
        @DisplayName("skal lage Journalhendelse med beskrivelse for avviket ENDRE_FAGOMRADE som forteller nytt og gammelt fagomrade")
        fun skalLageJournalHendelseMedBeskrivelseSomInneholderNyttOgGammeltFagomrade() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medFagomrade(Fagomrade.BIDRAG)
                        .medJournalforendeEnhet("4806")
                        .leggTilSaksnummer(sak1337),
                )
            val avvikshendelse =
                String.format(
                    """
                    {
                      "avvikType":"%s",
                      "saksnummer":"%s",
                      "detaljer": {
                        "fagomrade":"FAR"
                      }
                    }
                    
                    """.trimIndent(),
                    AvvikType.ENDRE_FAGOMRADE,
                    sak1337,
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(avvikshendelse, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
            val journalHendelser = testDataManager.lesJournalHendelser(journalpost.journalpostId)
            Assertions
                .assertThat(journalHendelser)
                .`as`("journalhendelse.beskrivelse")
                .extracting<String, RuntimeException> { it.beskrivelse }
                .isEqualTo(listOf("Endret fra BID til FAR"))
        }

        @Test
        @DisplayName("skal endre fagomrade til FAR og tilbake til BID igjen")
        fun skalEndreFagomradeTilFarOgTilbakeTilBid() {
            val sak1337 = "1337"
            val journalpostId =
                testDataManager
                    .opprett(
                        JournalpostBygger
                            .enJournalfortJournalpost()
                            .medFagomrade(Fagomrade.BIDRAG)
                            .medJournalforendeEnhet("4806")
                            .leggTilSaksnummer(sak1337),
                    ).journalpostId
            var avvikshendelse =
                String.format(
                    """
                    {
                      "avvikType":"%s",
                      "saksnummer":"%s",
                      "detaljer": {
                        "fagomrade":"%s"
                      }
                    }
                    
                    """.trimIndent(),
                    AvvikType.ENDRE_FAGOMRADE,
                    sak1337,
                    Fagomrade.FARSKAP,
                )
            var responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpostId)),
                    initHttpEntity(avvikshendelse, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
            var muligJournalpost = testDataManager.hent(journalpostId, Journalpost::class.java)
            Assertions
                .assertThat(muligJournalpost)
                .isPresent
                .get()
                .extracting { it.fagomrade }
                .isEqualTo(Fagomrade.FARSKAP)
            avvikshendelse =
                String.format(
                    """
                    {
                      "avvikType":"%s",
                      "saksnummer":"%s",
                      "detaljer": {
                        "fagomrade":"%s"
                      }
                    }
                    
                    """.trimIndent(),
                    AvvikType.ENDRE_FAGOMRADE,
                    sak1337,
                    Fagomrade.BIDRAG,
                )
            responseEntity =
                httpHeaderTestRestTemplate.postForEntity(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpostId)),
                    initHttpEntity(
                        avvikshendelse,
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
            muligJournalpost = testDataManager.hent(journalpostId, Journalpost::class.java)
            Assertions
                .assertThat(muligJournalpost)
                .isPresent
                .get()
                .extracting { it.fagomrade }
                .isEqualTo(Fagomrade.BIDRAG_DATABASE)
            val response =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format("/journal/BID-%d?saksnummer=%s", journalpostId, sak1337),
                )
            Assertions.assertThat(response.statusCode).`as`("hent journalpost status").isEqualTo(HttpStatus.OK)
            val journalpostResponse = response.body ?: error("En GET med status OK skal ikke gi journalpostResponse som er null")
            Assertions
                .assertThat(journalpostResponse.journalpost)
                .extracting<String> { it?.fagomrade }
                .`as`("endelig fagområde")
                .isEqualTo(
                    Fagomrade.BIDRAG,
                )
        }
    }

    @Nested
    @DisplayName("fører til journalhendelser")
    internal inner class JournalHendelser {
        @Test
        @DisplayName("skal lagre journalhendelser for avvik")
        fun skalLageJournalhendelserForAvvik() {
            val journalpostId =
                testDataManager
                    .opprett(
                        JournalpostBygger
                            .enJournalfortJournalpost()
                            .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                            .medSkannetDato(LocalDate.now())
                            .leggTilSaksnummer("1337")
                            .utenOrginalBestilt(),
                    ).journalpostId
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.postForEntity(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(
                            HttpEntity::class.java,
                        ),
                        ArgumentMatchers.eq(OpprettOppgaveResponse::class.java),
                    ),
                ).thenReturn(ResponseEntity(OpprettOppgaveResponse(101L, "", "", "123", "MOT", "SR"), HttpStatus.CREATED))
            Assertions
                .assertThat(
                    httpHeaderTestRestTemplate
                        .postForEntity<BehandleAvvikshendelseResponse>(
                            String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpostId)),
                            initHttpEntity(
                                AvvikshendelseBuilder
                                    .enAvvikshendelse()
                                    .med(AvvikType.BESTILL_ORIGINAL)
                                    .medOpprettetAvEnhet("1337")
                                    .bygg(),
                                CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                            ),
                        ).statusCode.is2xxSuccessful,
                ).`as`("behandlet avvik")
                .isTrue
            val hendelser = testDataManager.lesJournalHendelser(journalpostId)
            Assertions.assertThat(hendelser).hasSize(1)
        }

        @Test
        @DisplayName("skal resultere i BAD_REQUEST når man sender en beskrivelse som er for lang til å lagre")
        fun skalFaBadRequestNarForLangBeskrivelseSendes() {
            val journalpostId =
                testDataManager
                    .opprett(
                        JournalpostBygger.enJournalfortJournalpost().medDokumentType(DokumentType.INNGAENDE_DOKUMENT),
                    ).journalpostId
            val beskrivelse = StringUtils.rightPad("This is Sparta!", 1001, 'x')
            val avvikshendelse =
                AvvikshendelseBuilder
                    .enAvvikshendelse()
                    .med(
                        AvvikType.INNG_TIL_UTG_DOKUMENT,
                    ).medBeskrivelse(beskrivelse)
                    .bygg()
            val response =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpostId)),
                    initHttpEntity(avvikshendelse, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(response)
                        .`as`("behandlet avvik")
                        .isNotNull
                        .extracting { HttpStatus.valueOf(it.statusCode.value()) }
                        .isEqualTo(HttpStatus.BAD_REQUEST)
                },
                Executable {
                    val headers = response.headers
                    Assertions.assertThat(headers.toSingleValueMap()).`as`("contains warning").containsKey(HttpHeaders.WARNING)
                    Assertions
                        .assertThat(hentMuligAdvarseliUtenExceptionPrefix(headers))
                        .`as`("warning")
                        .isPresent
                        .contains("Beskrivelse kan max være 1000 tegn!")
                },
            )
        }

        @Test
        @DisplayName("skal legge enhetsbeskrivelse til journalhendelsens beskrivelse ved avvikshendelse for BESTILL_ORIGINAL")
        fun skalLeggeEnhetsbeskrivelseTilJournalHendelsensBesrkivelseVedBestillOriginal() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medBatchNavn("ikke batch fra joark")
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .leggTilSaksnummer(sak1337)
                        .medSkannetDato(LocalDate.now())
                        .utenOrginalBestilt(),
                )
            val avvikshendelse =
                String.format(
                    """
                    {
                      "avvikType":"%s",
                      "saksnummer":"%s",
                      "detaljer": {
                        "enhetsnummer":"007"
                      }
                    }
                    
                    """.trimIndent(),
                    AvvikType.BESTILL_ORIGINAL,
                    sak1337,
                )
            val enheten = Enhet("Hemmelig", "007", "SPESEN")
            Mockito
                .`when`(httpHeaderRestTemplateMock.exchange("/enhet/007", HttpMethod.GET, null, Enhet::class.java))
                .thenReturn(ResponseEntity.ok(enheten))
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.postForEntity(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(
                            HttpEntity::class.java,
                        ),
                        ArgumentMatchers.eq(OpprettOppgaveResponse::class.java),
                    ),
                ).thenReturn(ResponseEntity(OpprettOppgaveResponse(101L, "", "", "123", "MOT", "SR"), HttpStatus.CREATED))
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(avvikshendelse, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions
                .assertThat(responseEntity.statusCode)
                .`as`("status: " + responseEntity.headers[HttpHeaders.WARNING])
                .isEqualTo(HttpStatus.OK)
            val journalHendelser = testDataManager.lesJournalHendelser(journalpost.journalpostId)
            Assertions
                .assertThat(journalHendelser)
                .`as`("journalhendelse.beskrivelse")
                .extracting<String, RuntimeException> { it.beskrivelse }
                .isEqualTo(listOf(String.format("Originaldokumentet er bestilt til %s", enheten.hentEnhetsinformasjon())))
        }
    }

    @Nested
    @DisplayName("OVERFOR_TIL_ANNEN_ENHET")
    internal inner class OverforTilAnnenEnhet {
        @Test
        @DisplayName("skal finne avviket OVERFOR_TIL_ANNEN_ENHET når journalpost er mottaksregistrert")
        fun skalFinneAvvikNarJpErMottaksregistrert() {
            val enMottaksregistrertJournalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enMottaksregistrertJournalpost()
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT),
                )
            val avvikshendelserResponse =
                httpHeaderTestRestTemplate
                    .exchange(
                        String.format(
                            AVVIK_PA_JOURNALPOST,
                            prefixId(enMottaksregistrertJournalpost),
                        ),
                        HttpMethod.GET,
                        null,
                        listeMedAvvikTyper(),
                    )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(avvikshendelserResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(
                            avvikshendelserResponse.body,
                        ).`as`("avvikshendelser")
                        .contains(AvvikType.OVERFOR_TIL_ANNEN_ENHET)
                },
            )
        }

        @Test
        @DisplayName("skal ikke finne avviket OVERFOR_TIL_ANNEN_ENHET når journalpost er journalført")
        fun skalIkkeFinneAvvikNarJpErJournalfort() {
            val enJournalfortJournalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT),
                )
            val avvikshendelserResponse =
                httpHeaderTestRestTemplate.getForEntity<List<AvvikType>>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(enJournalfortJournalpost)),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(avvikshendelserResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(
                            avvikshendelserResponse.body,
                        ).`as`("avvikshendelser")
                        .doesNotContain(AvvikType.OVERFOR_TIL_ANNEN_ENHET)
                },
            )
        }

        @Test
        @DisplayName("skal ikke behandle avvik når journalpost er annen enn mottaksregistrert")
        fun skalBehandleAvvik() {
            val enjournalpost = testDataManager.opprett(JournalpostBygger.enJournalpost())
            val pathAvvik = String.format(AVVIK_PA_JOURNALPOST, prefixId(enjournalpost))
            val avvikshendelse =
                """
                {
                  "avvikType":"OVERFOR_TIL_ANNEN_ENHET",
                  "detaljer": {
                    "enhetsnummer":"1001"
                  }
                }
                
                """.trimIndent()
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    pathAvvik,
                    initHttpEntity(avvikshendelse, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("skal behandle avvik når journalpost er mottaksregistrert")
        fun skalBehandleAvvikNarJournalpostErMottaksregistrert() {
            val gammeltEnhetsnummer = "007"
            val enMottaksregistrertJournalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enMottaksregistrertJournalpost()
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .medFagomrade(Fagomrade.BIDRAG)
                        .medJournalforendeEnhet(gammeltEnhetsnummer),
                )
            val nyttEnhetsnummer = "1001"
            val pathAvvik = String.format(AVVIK_PA_JOURNALPOST, prefixId(enMottaksregistrertJournalpost))
            val avvikshendelse =
                String.format(
                    """
                    {
                      "avvikType":"OVERFOR_TIL_ANNEN_ENHET",
                      "detaljer": {
                        "gammeltEnhetsnummer":"%s",
                        "nyttEnhetsnummer"   :"%s"
                      }
                    }
                    
                    """.trimIndent(),
                    gammeltEnhetsnummer,
                    nyttEnhetsnummer,
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    pathAvvik,
                    initHttpEntity(avvikshendelse, CustomHeader(EnhetFilter.X_ENHET_HEADER, gammeltEnhetsnummer)),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
            val muligJournalpost = testDataManager.hent(enMottaksregistrertJournalpost.journalpostId, Journalpost::class.java)
            Assertions
                .assertThat(muligJournalpost)
                .isPresent
                .get()
                .extracting { it.journalforendeEnhet }
                .isEqualTo(nyttEnhetsnummer)
        }

        @Test
        @DisplayName("skal sende journalpost hendelse når avvik behandles")
        fun skalSendeJournalpostHendelseNarAvvikBehandles() {
            val gammeltEnhetsnummer = "007"
            val enMottaksregistrertJournalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enMottaksregistrertJournalpost()
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .medFagomrade(Fagomrade.BIDRAG_DATABASE)
                        .medJournalforendeEnhet(gammeltEnhetsnummer),
                )
            val nyttEnhetsnummer = "1001"
            val pathAvvik = String.format(AVVIK_PA_JOURNALPOST, prefixId(enMottaksregistrertJournalpost))
            val avvikshendelse =
                """
                {
                  "avvikType":"OVERFOR_TIL_ANNEN_ENHET",
                  "detaljer": {
                    "gammeltEnhetsnummer":"$gammeltEnhetsnummer",
                    "nyttEnhetsnummer"   :"$nyttEnhetsnummer"
                  }
                }
                
                """.trimIndent()
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    pathAvvik,
                    initHttpEntity(avvikshendelse, CustomHeader(EnhetFilter.X_ENHET_HEADER, gammeltEnhetsnummer)),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
            val hendelseCaptor =
                ArgumentCaptor.forClass(
                    JournalpostHendelse::class.java,
                )
            Mockito.verify(journalpostKafkaEventProducerMock).publish(hendelseCaptor.capture())
            val journalpostHendelse = hendelseCaptor.value
            Assertions.assertThat(journalpostHendelse).`as`("journalpostHendelse").isNotNull
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(journalpostHendelse.enhet).`as`("enhet").isEqualTo(nyttEnhetsnummer) },
                Executable { Assertions.assertThat(journalpostHendelse.fagomrade).`as`("fagomrade").isEqualTo(Fagomrade.BIDRAG) },
                Executable {
                    Assertions
                        .assertThat(
                            journalpostHendelse.journalstatus,
                        ).`as`("journalstatus")
                        .isEqualTo(Journalstatus.MOTTAKSREGISTRERT)
                },
            )
        }
    }

    @Nested
    @DisplayName("BESTILL_RESKANNING")
    internal inner class BestillReskanning {
        @Test
        @DisplayName("skal opprette en oppgave for BESTILL_RESKANNING på en journalpost")
        fun skalOppretteOppgaveForBestillReskanningPaJournalpost() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalpostSomErFeilfort("13123123")
                        .medDokumentreferanse("en dokumentreferanse")
                        .medSkannetDato(LocalDate.now())
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .medBatchNavn("batchen")
                        .medGjelder("13213213"),
                )
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.postForEntity(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(
                            HttpEntity::class.java,
                        ),
                        ArgumentMatchers.eq(OpprettOppgaveResponse::class.java),
                    ),
                ).thenReturn(ResponseEntity(OpprettOppgaveResponse(101L, "", "", "123", "MOT", "SR"), HttpStatus.CREATED))
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.exchange(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.eq(HttpMethod.POST),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.eq(
                            PersonDto::class.java,
                        ),
                    ),
                ).thenReturn(
                    ResponseEntity(
                        PersonDto(
                            Personident("123213"),
                            "aaaaaa",
                            "",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "aaaaaa",
                            null,
                            null,
                            null,
                            "",
                        ),
                        HttpStatus.CREATED,
                    ),
                )
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.BESTILL_RESKANNING.name, "", sak1337),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(responseEntity.body)
                        .`as`("BehandleAvvikshendelseResponse")
                        .isNotNull
                        .extracting { it!!.avvikType }
                        .`as`("avvikType")
                        .isEqualTo(Avvikstype.BESTILL_RESKANNING.name)
                },
                Executable {
                    val oppgaveCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
                    Mockito.verify(httpHeaderRestTemplateMock).postForEntity(
                        ArgumentMatchers.anyString(),
                        oppgaveCaptor.capture(),
                        ArgumentMatchers.eq(
                            OpprettOppgaveResponse::class.java,
                        ),
                    )
                    val oppgave = oppgaveCaptor.value.body
                    Assertions.assertThat(oppgave).`as`("oppgave").isNotNull
                    Assertions.assertThat((oppgave as Oppgave).saksreferanse).`as`("oppgave.saksreferanse").isEqualTo(sak1337)
                    Assertions.assertThat(oppgave.aktoerId).`as`("oppgave.aktoerid").isEqualTo("aaaaaa")
                    Assertions
                        .assertThat(
                            oppgave.beskrivelse,
                        ).`as`("oppgave.beskrivelse")
                        .contains("Dokumentet ble skannet " + LocalDate.now())
                },
            )
        }

        @Test
        @DisplayName("skal opprette avvik for reskanning som fører til at journalpost blir fjernet fra journalen")
        fun skalOppretteAvvikForReskanningSomForerTilFjerningAvJournalpostFraJournal() {
            val sak1001001 = "1001001"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medSkannetDato(LocalDate.now())
                        .leggTilSaksnummer(sak1001001)
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT),
                )
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.postForEntity(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(
                            HttpEntity::class.java,
                        ),
                        ArgumentMatchers.eq(OpprettOppgaveResponse::class.java),
                    ),
                ).thenReturn(ResponseEntity(OpprettOppgaveResponse(-1L, "", "", "", Fagomrade.BIDRAG, ""), HttpStatus.OK))
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(
                        Avvikshendelse(AvvikType.BESTILL_RESKANNING.name, "", sak1001001),
                        CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                    ),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status, behandle avvik").isEqualTo(HttpStatus.OK)
            val avvikResponse = responseEntity.body
            val avvikshendelserResponse =
                httpHeaderTestRestTemplate.getForEntity<List<AvvikType>>(
                    String.format(AVVIK_PA_JP_MED_SAK, prefixId(journalpost), sak1001001),
                )
            val jpResponse =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(
                        "/journal/%s?saksnummer=%s",
                        prefixId(journalpost),
                        sak1001001,
                    ),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(avvikResponse)
                        .extracting { it!!.avvikType }
                        .`as`("avvikType")
                        .isEqualTo(AvvikType.BESTILL_RESKANNING.name)
                },
                Executable {
                    Assertions
                        .assertThat(
                            avvikshendelserResponse.statusCode,
                        ).`as`("status, hent avvik")
                        .isEqualTo(HttpStatus.OK)
                },
                Executable {
                    Assertions
                        .assertThat(avvikshendelserResponse.body)
                        .`as`("innhold, hent avvik")
                        .asList()
                        .isEmpty()
                },
                Executable { Assertions.assertThat(jpResponse.statusCode).`as`("status, hent journalpost").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(jpResponse.body)
                        .`as`("hentet journalpost")
                        .isNotNull
                        .extracting<JournalpostDto> { it!!.journalpost }
                        .isNotNull
                        .extracting<Boolean> { it.feilfort }
                        .`as`("feilført")
                        .isEqualTo(true)
                },
            )
        }

        @Test
        @DisplayName("skal lagre beskrivelsen til avvikshendelsen i T_JP_LOGG")
        fun skalLagreBeskrivelsenTilAvviket() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enMottaksregistrertJournalpost()
                        .medDokumentreferanse("en dokumentreferanse")
                        .medSkannetDato(LocalDate.now())
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .medBatchNavn("batchen")
                        .leggTilSaksnummer(sak1337),
                )
            val beskrivelseFraSaksbehandler = "dette er en beskrivelse fra saksbehandler..."
            val avvikshendelse =
                String.format(
                    """
                    {
                      "avvikType":"BESTILL_RESKANNING",
                      "beskrivelse":"%s"
                    }
                    
                    """.trimIndent(),
                    beskrivelseFraSaksbehandler,
                )
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.postForEntity(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(
                            HttpEntity::class.java,
                        ),
                        ArgumentMatchers.eq(OpprettOppgaveResponse::class.java),
                    ),
                ).thenReturn(ResponseEntity(OpprettOppgaveResponse(101L, "", "", "123", "MOT", "SR"), HttpStatus.CREATED))
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(avvikshendelse, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
            val journalHendelser = journalHendelseRepository.findByJournalpostId(journalpost.journalpostId)
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(
                            journalHendelser,
                        ).`as`("antall journalhendelser for " + journalpost.journalpostId)
                        .hasSize(1)
                },
                Executable {
                    Assertions
                        .assertThat(journalHendelser)
                        .`as`("JournalHendelse")
                        .element(0)
                        .`as`("JournalHendelse::getBeskrivelse")
                        .extracting { it.beskrivelse }
                        .isEqualTo(beskrivelseFraSaksbehandler)
                },
            )
        }

        @Test
        @DisplayName("skal legge beskrivelse fra saksbehandler til oppgaveteksten")
        fun skalLeggeBeskrivelseFraSaksbehandlerTilOppgaveTeksten() {
            val sak1337 = "1337"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enMottaksregistrertJournalpost()
                        .medDokumentreferanse("en dokumentreferanse")
                        .medSkannetDato(LocalDate.now())
                        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
                        .medBatchNavn("batchen")
                        .leggTilSaksnummer(sak1337),
                )
            val beskrivelseFraSaksbehandler = "dette er en beskrivelse fra saksbehandler"
            val avvikshendelse =
                String.format(
                    """
                    {
                      "avvikType":"BESTILL_RESKANNING",
                      "beskrivelse":"%s"
                    }
                    
                    """.trimIndent(),
                    beskrivelseFraSaksbehandler,
                )
            Mockito
                .`when`(
                    httpHeaderRestTemplateMock.postForEntity(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(
                            HttpEntity::class.java,
                        ),
                        ArgumentMatchers.eq(OpprettOppgaveResponse::class.java),
                    ),
                ).thenReturn(ResponseEntity(OpprettOppgaveResponse(101L, "", "", "123", "MOT", "SR"), HttpStatus.CREATED))
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<BehandleAvvikshendelseResponse>(
                    String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                    initHttpEntity(avvikshendelse, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(responseEntity.statusCode).`as`("status med headers %s", responseEntity.headers).isEqualTo(HttpStatus.OK)
            val oppgaveArgumentCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
            Mockito.verify(httpHeaderRestTemplateMock).postForEntity(
                ArgumentMatchers.anyString(),
                oppgaveArgumentCaptor.capture(),
                ArgumentMatchers.eq(
                    OpprettOppgaveResponse::class.java,
                ),
            )
            val oppgaveTekst =
                Optional
                    .ofNullable(oppgaveArgumentCaptor.value)
                    .map { it.body }
                    .map { (it as Oppgave).beskrivelse }
                    .orElseThrow { AssertionFailedError("Fant ingen oppgavetekst") }
            Assertions.assertThat(oppgaveTekst).`as`("oppgave").contains(beskrivelseFraSaksbehandler)
        }
    }

    companion object {
        private const val AVVIK_PA_JOURNALPOST = "/journal/%s/avvik"
        private const val AVVIK_PA_JP_MED_SAK = "/journal/%s/avvik?saksnummer=%s"
    }
}
