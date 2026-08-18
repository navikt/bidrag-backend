package no.nav.bidrag.dokument.journalpost.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles
import no.nav.bidrag.dokument.journalpost.TestDataManager
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager
import no.nav.bidrag.dokument.journalpost.consumer.BidragPersonConsumer
import no.nav.bidrag.dokument.journalpost.consumer.NorgConsumer
import no.nav.bidrag.dokument.journalpost.consumer.OppgaveConsumer
import no.nav.bidrag.dokument.journalpost.consumer.SaksbehandlerConsumer
import no.nav.bidrag.dokument.journalpost.dto.CommandBuilder
import no.nav.bidrag.dokument.journalpost.dto.Saksbehandler
import no.nav.bidrag.dokument.journalpost.dto.Violation
import no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger
import no.nav.bidrag.dokument.journalpost.entity.ReturDetaljerLogg
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostHendelseListener
import no.nav.bidrag.dokument.journalpost.model.Fagomrade
import no.nav.bidrag.dokument.journalpost.model.Journalstatus
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository
import no.nav.bidrag.dokument.journalpost.repository.JournalsakReposistory
import no.nav.bidrag.dokument.journalpost.service.TilgangskontrollService
import no.nav.bidrag.dokument.journalpost.service.TokenInformationService
import no.nav.bidrag.dokument.journalpost.utils.CustomHeader
import no.nav.bidrag.dokument.journalpost.utils.initHttpEntity
import no.nav.bidrag.dokument.journalpost.utils.prefixId
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.dokument.EndreDokument
import no.nav.bidrag.transport.dokument.EndreJournalpostCommand
import no.nav.bidrag.transport.dokument.EndreReturDetaljer
import no.nav.bidrag.transport.dokument.HendelseType
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions
import org.assertj.core.api.Condition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.boot.resttestclient.getForEntity
import org.springframework.boot.resttestclient.patchForObject
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.RequestEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.net.URI
import java.time.LocalDate
import java.util.Optional

@ActiveProfiles(BidragDokumentJournalpostProfiles.TEST, BidragDokumentJournalpostProfiles.SECURED_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [BidragDokumentJournalpostLocalTest::class])
@EnableWireMock(value = [ConfigureWireMock(port = 0)])
@EnableMockOAuth2Server
@ExtendWith(SpringExtension::class)
internal class JournalpostControllerEndreTest {
    @Autowired
    private lateinit var journalsakReposistory: JournalsakReposistory

    @Autowired
    private lateinit var journalpostRepository: JournalpostRepository

    @Autowired
    private lateinit var testDataManager: TestDataManager

    @Autowired
    private lateinit var httpHeaderTestRestTemplate: TestRestTemplate

    @MockkBean
    private lateinit var tilgangskontrollServiceMock: TilgangskontrollService

    @MockkBean
    private lateinit var bidragPersonConsumer: BidragPersonConsumer

    @MockkBean(relaxed = true)
    private lateinit var journalpostHendelseListenerMock: JournalpostHendelseListener

    @MockkBean
    private lateinit var saksbehandlerOidcTokenManagerMock: SaksbehandlerOidcTokenManager

    @MockkBean(relaxed = true)
    private lateinit var tokenInformationServiceMock: TokenInformationService

    @MockkBean
    private lateinit var norgConsumer: NorgConsumer

    @MockkBean(relaxed = true)
    private lateinit var oppgaveConsumer: OppgaveConsumer

    @MockkBean(relaxed = true)
    private lateinit var saksbehandlerConsumer: SaksbehandlerConsumer

    @BeforeEach
    fun resetMocks() {
        clearAllMocks()
        every { tilgangskontrollServiceMock.sjekkTilgangSak(any()) }.returns(Unit)
        every { tokenInformationServiceMock.hentSaksbehandler(any()) }.returns(
            Optional.of(
                Saksbehandler(""),
            ),
        )
    }

    @BeforeEach
    fun opprettKodeJournalstatusForVisning() {
        testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.JOURNALFORT)
    }

    @Nested
    @DisplayName("oppdater journalpost med returdetaljer")
    internal inner class JournalposterReturDetaljer {
        @Test
        @DisplayName("skal endre journalpost med returdetaljer")
        fun skalEndreJournalpostMedReturDetaljer() {
            val returDetaljer1 = ReturDetaljerLogg(LocalDate.parse("2020-01-05"), "Test test test")
            val returDetaljer2 = ReturDetaljerLogg(LocalDate.parse("2020-01-06"), "Test test test")
            val nyReturDato = LocalDate.parse("2222-12-12")
            val nyReturBeskrivelse = "Dette er endre beskrivelse"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medRetur(returDetaljer1.dato, returDetaljer1)
                        .medRetur(returDetaljer2.dato, returDetaljer2),
                )
            val endreJournalpostCommand =
                CommandBuilder()
                    .medJournalpostId(journalpost.journalpostId)
                    .medEndreReturDetaljer(
                        EndreReturDetaljer(
                            returDetaljer1.dato,
                            nyReturDato,
                            nyReturBeskrivelse,
                        ),
                    ).medEndreReturDetaljer(
                        EndreReturDetaljer(
                            returDetaljer2.dato,
                            null,
                            nyReturBeskrivelse,
                        ),
                    ).medEndreReturDetaljer(
                        EndreReturDetaljer(
                            LocalDate.parse("2111-01-01"),
                            null,
                            "asdasdasdasdsa",
                        ),
                    ).tilEndreJournalpostCommand()
            httpHeaderTestRestTemplate.patchForObject<Unit>(
                String.format(JOURNAL_UTEN_SAK, prefixId(journalpost)),
                initHttpEntity(
                    endreJournalpostCommand,
                    CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001"),
                ),
            )
            val endretJournalpost =
                journalpostRepository.findById(journalpost.journalpostId).orElseThrow()
            val returDetaljerLogg = endretJournalpost.returDetaljerLogg
            assertSoftly {
                returDetaljerLogg.size shouldBe 2
                returDetaljerLogg
                    .stream()
                    .anyMatch { it: ReturDetaljerLogg -> it.dato == nyReturDato } shouldBe true
                returDetaljerLogg
                    .stream()
                    .anyMatch { it: ReturDetaljerLogg -> it.dato == returDetaljer2.dato } shouldBe true
                returDetaljerLogg.forEach { it: ReturDetaljerLogg ->
                    it.beskrivelse shouldBe nyReturBeskrivelse
                }
            }
        }
    }

    @Nested
    @DisplayName("journalposter med saksnummer som parameter")
    internal inner class JournalposterMedSak {
        @Test
        @DisplayName("skal endre journalpost")
        fun skalEndreJournalpost() {
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medAvsender("Cula, Dr. A.")
                        .medBeskrivelse("Dette er et testnotat")
                        .medDokumentdato(LocalDate.now().minusDays(2))
                        .medDokumentreferanse("1001")
                        .medDokumentType("I")
                        .medFagomrade(Fagomrade.FARSKAP)
                        .medGjelder("Guess!!!")
                        .medJournaldato(LocalDate.now())
                        .medJournalfortAv("S. Vindel")
                        .medJournalforendeEnhet("Trygdekontoret")
                        .medJournalstatus(Journalstatus.MOTTAKSREGISTRERT)
                        .leggTilSaksnummer("12345"),
                )
            val endreJournalpostCommand =
                CommandBuilder()
                    .medJournalpostId(journalpost.journalpostId)
                    .medAvsenderNavn("Dauden, Svarte")
                    .medBeskrivelse("Dette er en endring av notatbeskrivelse")
                    .medGjelder("Gjelder before")
                    .medJournaldato(LocalDate.now().plusDays(1))
                    .medTilknyttSaker("54321")
                    .tilEndreJournalpostCommand()
            val hendelseCaptor = slot<JournalpostHendelse>()
            every { journalpostHendelseListenerMock.publish(capture(hendelseCaptor)) } returns Unit
            val endretJournalpostResponse =
                httpHeaderTestRestTemplate.exchange<Unit>(
                    URI.create(String.format(JOURNAL_MED_SAK, prefixId(journalpost), "54321")),
                    HttpMethod.PATCH,
                    initHttpEntity(endreJournalpostCommand, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions
                .assertThat(endretJournalpostResponse)
                .extracting { it.statusCode }
                .`as`("statusCode")
                .isEqualTo(HttpStatus.OK)
            val journalsaker = journalsakReposistory.findBySaksnummer("54321")
            Assertions.assertThat(journalsaker).`as`("journalsaker").isNotEmpty
            val endretJournalpost = journalsaker[0].journalpost
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(endretJournalpost.avsender).`as`("avsender").isEqualTo("Dauden") },
                Executable { Assertions.assertThat(endretJournalpost.avsenderFornavn).`as`("avsender fornavn").isEqualTo("Svarte") },
                Executable {
                    Assertions
                        .assertThat(
                            endretJournalpost.beskrivelse,
                        ).`as`("beskrivelse")
                        .isEqualTo("Dette er en endring av notatbeskrivelse")
                },
                Executable { Assertions.assertThat(endretJournalpost.gjelder).`as`("gjelder").isEqualTo("Gjelder before") },
                Executable { Assertions.assertThat(endretJournalpost.journalforendeEnhet).`as`("JournalforendeEnhet").isEqualTo("1001") },
                Executable {
                    Assertions
                        .assertThat(
                            endretJournalpost.journaldato,
                        ).`as`("journaldato")
                        .isEqualTo(LocalDate.now().plusDays(1))
                },
                Executable {
                    val (journalpostId, _, fnr, _, tittel, fagomrade, _, _, journalposttype, hendelseType, enhet, journalstatus, _, _, _, dokumentDato, journalfortDato) = hendelseCaptor.captured
                    org.junit.jupiter.api.Assertions.assertAll(
                        Executable { Assertions.assertThat(enhet).`as`("enhet").isEqualTo("1001") },
                        Executable { Assertions.assertThat(fnr).`as`("fnr").isEqualTo("Gjelder before") },
                        Executable { Assertions.assertThat(fagomrade).`as`("fagomrade").isEqualTo(Fagomrade.FARSKAP) },
                        Executable {
                            Assertions
                                .assertThat(
                                    journalstatus,
                                ).`as`("journalstatus")
                                .isEqualTo(Journalstatus.MOTTAKSREGISTRERT)
                        },
                        Executable { Assertions.assertThat(hendelseType).`as`("hendelsetype").isEqualTo(HendelseType.ENDRING) },
                        Executable { Assertions.assertThat(journalposttype).`as`("journalpostType").isEqualTo("I") },
                        Executable { Assertions.assertThat(tittel).`as`("tittel").isEqualTo("Dette er en endring av notatbeskrivelse") },
                        Executable { Assertions.assertThat(dokumentDato).`as`("dokumentDato").isEqualTo(LocalDate.now().minusDays(2)) },
                        Executable {
                            Assertions
                                .assertThat(
                                    journalfortDato,
                                ).`as`("journalfortDato")
                                .isEqualTo(LocalDate.now().plusDays(1))
                        },
                        Executable {
                            Assertions
                                .assertThat(
                                    journalpostId,
                                ).`as`("journalpostId")
                                .isEqualTo("BID-" + journalpost.journalpostId)
                        },
                    )
                },
            )
        }
    }

    @Nested
    @DisplayName("Registrer mottaksregistrert journalpost")
    internal inner class RegistrerMottaksregistrertJournalpost {
        @Test
        @DisplayName("skal ha http status 400 (BAD_REQUEST) når man prøver å registrere journalpost som ikke eksisterer")
        fun skalFaBadRequstAvRegistreringAvJournalpostSomIkkeEksisterer() {
            val registrerJpResponse =
                httpHeaderTestRestTemplate.exchange<Unit>(
                    URI.create(String.format(JOURNAL_UTEN_SAK, "BID-12345")),
                    HttpMethod.PATCH,
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("skal ha http status 200 (OK) når en journalpost blir registrert")
        fun skalFaOkNarEnJournalpostBlirRegistrert() {
            val journalpost =
                testDataManager.opprett(JournalpostBygger.enJournalfortJournalpost().medJournalstatus(Journalstatus.MOTTAKSREGISTRERT))
            val endreJournalpostCommand =
                CommandBuilder()
                    .medGjelder("dr. a. cula")
                    .medSkalJournalfores()
                    .tilEndreJournalpostCommand()
            val registrerJpResponse =
                httpHeaderTestRestTemplate.exchange<Unit>(
                    URI.create(String.format(JOURNAL_UTEN_SAK, prefixId(journalpost))),
                    HttpMethod.PATCH,
                    initHttpEntity(endreJournalpostCommand, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.OK)
        }

        @Test
        @DisplayName("skal sette journalstatus til 'J' når journalpost blir registrert")
        fun skalSetteJournalstatusTilJournalfortNarJournalpostBlirRegistrert() {
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger.enJournalfortJournalpost().utenSak().medJournalstatus(Journalstatus.MOTTAKSREGISTRERT),
                )
            val endreJournalpostCommand =
                CommandBuilder()
                    .medGjelder("dr. a. cula")
                    .medSkalJournalfores()
                    .medTilknyttSaker("2002")
                    .tilEndreJournalpostCommand()
            val registrerJpResponse =
                httpHeaderTestRestTemplate.exchange<Unit>(
                    URI.create(String.format(JOURNAL_UTEN_SAK, prefixId(journalpost))),
                    HttpMethod.PATCH,
                    initHttpEntity(endreJournalpostCommand, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.OK)
            val hentJpResponse =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(JOURNAL_UTEN_SAK, prefixId(journalpost)),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(hentJpResponse.statusCode).`as`("status code").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(hentJpResponse.body)
                        .`as`("journalpost response")
                        .extracting<JournalpostDto> { it!!.journalpost }
                        .extracting<String> { it.journalstatus }
                        .`as`("journalstatus")
                        .isEqualTo("J")
                },
                Executable {
                    Assertions
                        .assertThat(hentJpResponse.body)
                        .`as`("journalpost response")
                        .extracting<JournalpostDto> { it!!.journalpost }
                        .extracting<String> { it.journalforendeEnhet }
                        .isEqualTo("1001")
                },
                Executable {
                    Assertions
                        .assertThat(hentJpResponse.body)
                        .`as`("journalpost response")
                        .extracting { it!!.sakstilknytninger }
                        .`as`("sakstilknytninger")
                        .isEqualTo(listOf("2002"))
                },
            )
        }

        @Test
        @DisplayName("skal ha http-status UNAUTHORIZED dersom id-token er ugyldig")
        fun skalReturnereUnauthorizedDersomTokenErUgyldig() {
            val journalpost = testDataManager.opprett(JournalpostBygger.enJournalfortJournalpost())
            val registrerJpResponse =
                httpHeaderTestRestTemplate.exchange<Unit>(
                    URI.create(String.format(JOURNAL_UTEN_SAK, prefixId(journalpost))),
                    HttpMethod.PATCH,
                    initHttpEntity(
                        EndreJournalpostCommand(),
                        CustomHeader(
                            HttpHeaders.AUTHORIZATION,
                            "",
                        ),
                    ),
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        @DisplayName(
            "skal ha http status 400 (BAD_REQUEST) når man prøver å journalføre journalposter som har annen status enn mottaksregistrert",
        )
        fun skalFraBadRequestVedJournalforingAvAlleredeMottaksregistrertJournalpost() {
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medJournalstatus(Journalstatus.JOURNALFORT)
                        .leggTilSaksnummer("1001001"),
                )
            val endreJournalpostCommand =
                CommandBuilder()
                    .medSkalJournalfores()
                    .medGjelder("dr. a. cula")
                    .tilEndreJournalpostCommand()
            val registrerJpResponse =
                httpHeaderTestRestTemplate.exchange<Void>(
                    URI.create(String.format(JOURNAL_UTEN_SAK, prefixId(journalpost))),
                    HttpMethod.PATCH,
                    initHttpEntity(endreJournalpostCommand, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("skal ha http status 400 (BAD_REQUEST) når man prøver å journalføre journalposter uten sakstilknytning")
        fun skalFraBadRequestVedJournalforingAvJournalpostUtenSakstilknytning() {
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .medJournalstatus(Journalstatus.MOTTAKSREGISTRERT)
                        .utenSak(),
                )
            val endreJournalpostCommand =
                CommandBuilder()
                    .medGjelder("dr. a. cula")
                    .medSkalJournalfores()
                    .tilEndreJournalpostCommand()
            val registrerJpResponse =
                httpHeaderTestRestTemplate.exchange<Void>(
                    URI.create(String.format(JOURNAL_UTEN_SAK, prefixId(journalpost))),
                    HttpMethod.PATCH,
                    initHttpEntity(endreJournalpostCommand, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("journalføring")
    internal inner class Journalforing {
        @Test
        @DisplayName("skal journalføre en mottaksregistrert journalpost")
        fun skalJournalforeMottaksregistrertJournalpost() {
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger.enMottaksregistrertJournalpost().utenSak().medFagomrade(Fagomrade.BIDRAG),
                )
            val json =
                java.lang.String.join(
                    "\n",
                    "{",
                    "\"skalJournalfores\":true,",
                    "\"gjelder\":\"25018512345\",",
                    "\"tittel\":\"journalfør\",",
                    "\"tilknyttSaker\":[\"0703467\"],",
                    "\"endreDokumenter\": [],",
                    "\"fagomrade\":\"BID\",",
                    "\"dokumentDato\":\"2020-01-01\"",
                    "}",
                )
            val registrerJpResponse =
                httpHeaderTestRestTemplate.exchange<Unit>(
                    URI.create(String.format(JOURNAL_UTEN_SAK, prefixId(journalpost))),
                    HttpMethod.PATCH,
                    initHttpEntity(json, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.OK)
            val hentJpResponse =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(JOURNAL_UTEN_SAK, prefixId(journalpost)),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(hentJpResponse.statusCode).`as`("status code").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(hentJpResponse.body)
                        .`as`("journalpost response")
                        .extracting<JournalpostDto> { it!!.journalpost }
                        .extracting<String> { it.journalstatus }
                        .`as`("journalstatus")
                        .isEqualTo("J")
                },
                Executable {
                    Assertions
                        .assertThat(hentJpResponse.body)
                        .`as`("journalpost response")
                        .extracting { it!!.sakstilknytninger }
                        .`as`("sakstilknytninger")
                        .isEqualTo(listOf("0703467"))
                },
            )
        }

        @Test
        @DisplayName("skal publisere journalpostHendelse med aktuelle data for journalføring")
        fun skalPublisereJournalpostHendelseMedAktuelleDataForJournalforing() {
            val dokumentDato = LocalDate.parse("2020-01-01")
            val journalDato = LocalDate.parse("2022-01-01")
            val gjelderIdent = genererFødselsnummer()
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enMottaksregistrertJournalpost()
                        .utenSak()
                        .medFagomrade(Fagomrade.BIDRAG)
                        .medDokumentType("I")
                        .medJournaldato(journalDato),
                )
            val json =
                """
                {
                  "skalJournalfores":true,
                  "gjelder":"$gjelderIdent",
                  "tittel":"journalfør",
                  "tilknyttSaker":["0703467", "13213123"],
                  "endreDokumenter": [],
                  "fagomrade":"BID",
                  "dokumentDato":"2020-01-01"
                }
                
                """.trimIndent().trim {
                    it <= ' '
                }
            val hendelseCaptor = slot<JournalpostHendelse>()
            every { journalpostHendelseListenerMock.publish(capture(hendelseCaptor)) } returns Unit
            val registrerJpResponse =
                httpHeaderTestRestTemplate.exchange<Unit>(
                    URI.create(String.format(JOURNAL_UTEN_SAK, prefixId(journalpost))),
                    HttpMethod.PATCH,
                    initHttpEntity(json, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.OK)

            val hentJpResponse =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(JOURNAL_UTEN_SAK, prefixId(journalpost)),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(hentJpResponse)
                        .extracting<HttpStatusCode>(
                            { it.statusCode },
                        ).`as`("http status")
                        .isEqualTo(HttpStatus.OK)
                },
                Executable {
                    val (journalpostId, _, fnr, _, tittel, fagomrade, _, _, journalposttype, hendelseType, enhet, journalstatus, _, _, sakstilknytninger, dokumentDato1, journalfortDato) = hendelseCaptor.captured
                    org.junit.jupiter.api.Assertions.assertAll(
                        Executable { Assertions.assertThat(enhet).`as`("enhet").isEqualTo("1001") },
                        Executable { Assertions.assertThat(fnr).`as`("fnr").isEqualTo(gjelderIdent) },
                        Executable { Assertions.assertThat(fagomrade).`as`("fagomrade").isEqualTo(Fagomrade.BIDRAG) },
                        Executable { Assertions.assertThat(journalstatus).`as`("journalstatus").isEqualTo(Journalstatus.JOURNALFORT) },
                        Executable { Assertions.assertThat(hendelseType).`as`("hendelsetype").isEqualTo(HendelseType.JOURNALFORING) },
                        Executable { Assertions.assertThat(journalposttype).`as`("journalpostType").isEqualTo("I") },
                        Executable { Assertions.assertThat(tittel).`as`("tittel").isEqualTo("journalfør") },
                        Executable { Assertions.assertThat(dokumentDato1).`as`("dokumentDato").isEqualTo(dokumentDato) },
                        Executable { Assertions.assertThat(journalfortDato).`as`("journalfortDato").isEqualTo(journalDato) },
                        Executable { Assertions.assertThat(sakstilknytninger).`as`("sakstilknytninger").contains("0703467", "13213123") },
                        Executable {
                            Assertions
                                .assertThat(
                                    journalpostId,
                                ).`as`("journalpostId")
                                .isEqualTo("BID-" + journalpost.journalpostId)
                        },
                    )
                },
            )
        }
    }

    @Nested
    @DisplayName("EndreJournalpostCommand - @ControllerAdvice")
    internal inner class EndreJournalpostCommandControllerAdvice {
        @Test
        @DisplayName("skal få violation når det er flere dokumenter tilknyttet en journalpost og det mangler gjelder for journalføring")
        fun skalFaViolationNarDetErFlereDokumenterTilknytttetEnSakOgDetManglerGjelderForJournalforing() {
            val journalpost = testDataManager.opprett(JournalpostBygger.enJournalfortJournalpost())
            val endreJournalpostCommand =
                CommandBuilder()
                    .medDokumenter(EndreDokument(), EndreDokument())
                    .medSkalJournalfores()
                    .tilEndreJournalpostCommand()
            val badRequestResponse =
                httpHeaderTestRestTemplate.exchange(
                    URI.create(String.format(JOURNAL_UTEN_SAK, prefixId(journalpost))),
                    HttpMethod.PATCH,
                    initHttpEntity(endreJournalpostCommand, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                    object : ParameterizedTypeReference<List<Violation>>() {},
                )
            org.junit.jupiter.api.Assertions.assertAll(
                { Assertions.assertThat(badRequestResponse.statusCode).`as`("status").isEqualTo(HttpStatus.BAD_REQUEST) },
                {
                    Assertions.assertThat(badRequestResponse.body).`as`("body").isNotNull
                    Assertions.assertThat(badRequestResponse.body).`as`("violations").has(
                        Assertions.allOf(
                            violation("endreDokumenter"),
                            violation("gjelder"),
                        ),
                    )
                },
            )
        }

        @Test
        @DisplayName("skal få 404 NOT_FOUND når journalpostId er ukjent")
        fun skalFa404NotFoundNarJournalpostIdErUkjent() {
            val endreJournalpostCommand = CommandBuilder().tilEndreJournalpostCommand()
            val badRequestResponse =
                httpHeaderTestRestTemplate.exchange<Unit>(
                    URI.create(String.format(JOURNAL_UTEN_SAK, prefixId(123))),
                    HttpMethod.PATCH,
                    initHttpEntity(endreJournalpostCommand, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(badRequestResponse.statusCode).`as`("status").isEqualTo(HttpStatus.NOT_FOUND)
        }

        private fun violation(property: String): Condition<in List<Violation>> = Condition(
            { list: List<Violation> -> list.stream().anyMatch { (property1): Violation -> property1 == property } },
            "Property '%s'",
            property,
        )
    }

    companion object {
        private const val JOURNAL_MED_SAK = "/journal/%s?saksnummer=%s"
        private const val JOURNAL_UTEN_SAK = "/journal/%s"
    }
}
