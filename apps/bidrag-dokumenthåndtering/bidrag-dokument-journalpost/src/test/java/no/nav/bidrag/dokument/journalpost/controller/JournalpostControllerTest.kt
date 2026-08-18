package no.nav.bidrag.dokument.journalpost.controller

import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles
import no.nav.bidrag.dokument.journalpost.TestDataManager
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager
import no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger
import no.nav.bidrag.dokument.journalpost.entity.KodeBrevBygger
import no.nav.bidrag.dokument.journalpost.entity.ReturDetaljerLogg
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer
import no.nav.bidrag.dokument.journalpost.model.BATCH_NAVN_JOARK_15
import no.nav.bidrag.dokument.journalpost.model.Fagomrade
import no.nav.bidrag.dokument.journalpost.model.Journalstatus
import no.nav.bidrag.dokument.journalpost.service.TilgangskontrollService
import no.nav.bidrag.dokument.journalpost.service.TokenInformationService
import no.nav.bidrag.dokument.journalpost.utils.CustomHeader
import no.nav.bidrag.dokument.journalpost.utils.initHttpEntity
import no.nav.bidrag.dokument.journalpost.utils.prefixId
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.dokument.AktorDto
import no.nav.bidrag.transport.dokument.AvvikType
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.bidrag.transport.dokument.Kanal
import no.nav.bidrag.transport.dokument.KodeDto
import no.nav.bidrag.transport.dokument.ReturDetaljerLog
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.boot.resttestclient.getForEntity
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.util.UriComponentsBuilder
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.time.LocalDate
import java.util.Arrays

@ActiveProfiles(BidragDokumentJournalpostProfiles.TEST, BidragDokumentJournalpostProfiles.SECURED_TEST)
@DisplayName("JournalpostController")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [BidragDokumentJournalpostLocalTest::class])
@EnableWireMock(value = [ConfigureWireMock(port = 0)])
@EnableMockOAuth2Server
internal class JournalpostControllerTest {
    private fun listeMedJournalpost(): ParameterizedTypeReference<List<JournalpostDto>> = object : ParameterizedTypeReference<List<JournalpostDto>>() {}

    @LocalServerPort
    private var port: Int = 0

    @Value($$"${server.servlet.context-path}")
    private lateinit var contextPath: String

    @Autowired
    private lateinit var testDataManager: TestDataManager

    @Autowired
    private lateinit var httpHeaderTestRestTemplate: TestRestTemplate

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
            journalpostKafkaEventProducerMock,
            tokenInformationServiceMock,
            saksbehandlerOidcTokenManagerMock,
        )
        Mockito.`when`(tilgangskontrollServiceMock.harTilgangTilTema(ArgumentMatchers.anyString())).thenReturn(true)
    }

    @BeforeEach
    fun opprettKodeJournalstatusForVisning() {
        testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.JOURNALFORT)
    }

    @Test
    @DisplayName("skal ha forventet context path")
    fun skalHaForventetContextPath() {
        Assertions.assertThat(makeContextPath()).isEqualTo("http://localhost:$port/bidrag-dokument-journalpost")
    }

    private fun makeContextPath(): String = "http://localhost:$port$contextPath"

    @Nested
    @DisplayName("journalposter med saksnummer som parameter")
    internal inner class Journalposter {
        @Test
        @DisplayName("skal få 404 (NOT_FOUND) når id til journalpost ikke kan parses til int")
        fun skalFaNotFoundNarJournalpostIdErTallSomIkkeKanParsesTilInt() {
            val response = httpHeaderTestRestTemplate.getForEntity<JournalpostDto>(String.format(JOURNAL_MED_SAK, "BID-3838332453", "100"))
            Assertions.assertThat(response).extracting({ it.statusCode }).isEqualTo(
                HttpStatus.NOT_FOUND,
            )
        }

        @Test
        @DisplayName("skal ha http status 404 (NOT_FOUND) når journalpost ikke finnes")
        fun skalHaHttpStatusNoContentVedUkjentJournalpost() {
            val response = httpHeaderTestRestTemplate.getForEntity<JournalpostDto>(String.format(JOURNAL_MED_SAK, "BID-1001", "100"))
            Assertions.assertThat(response).extracting({ it.statusCode }).isEqualTo(
                HttpStatus.NOT_FOUND,
            )
        }

        @Test
        @DisplayName("skal ha http status 404 (NOT FOUND) når journalposten finnes på et annet saksnummer")
        fun skalHaHttpStatusNotFoundVedJournalpostSomErKnyttetTilEtAnnetSaksnummer() {
            val journalpost =
                testDataManager.opprett(JournalpostBygger.enJournalfortJournalpost().leggTilSaksnummer("007").medAvsender("Blund, Jon"))
            val response =
                httpHeaderTestRestTemplate.getForEntity<JournalpostDto>(
                    String.format(JOURNAL_MED_SAK, prefixId(journalpost), "100"),
                )
            Assertions.assertThat(response).extracting({ it.statusCode }).isEqualTo(
                HttpStatus.NOT_FOUND,
            )
        }

        @Test
        @DisplayName("skal ha http status 200 (OK) når journalpost finnes")
        fun skalHaHttpStatusOkNarJournalpostErHentet() {
            val journalpost =
                testDataManager.opprett(JournalpostBygger.enJournalfortJournalpost().leggTilSaksnummer("1015").medAvsender(", Theresa"))
            val response =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(JOURNAL_MED_SAK, prefixId(journalpost), "1015"),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(response)
                        .extracting(
                            { it.statusCode },
                        ).isEqualTo(HttpStatus.OK)
                },
                Executable {
                    Assertions
                        .assertThat(response.body)
                        .`as`("body")
                        .extracting<JournalpostDto> { it!!.journalpost }
//                        .`as`("journalpost")
                        .isNotNull
                        .extracting<String> { it: JournalpostDto -> it.avsenderNavn }
                        .`as`("avsenderNavn")
                        .isEqualTo(", Theresa")
                },
            )
        }

        @Test
        @DisplayName("skal mappe gjelder aktør")
        fun skalMappeGjelderAktor() {
            val gjelderIdent = genererFødselsnummer()
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .leggTilSaksnummer("1001")
                        .medAvsender("Me")
                        .medGjelder(gjelderIdent),
                )
            val response =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(JOURNAL_MED_SAK, prefixId(journalpost), "1001"),
                )
            Assertions
                .assertThat(response)
                .extracting({ it.statusCode })
                .isEqualTo(
                    HttpStatus.OK,
                )
            Assertions
                .assertThat(response)
                .extracting({ it.body })
                .extracting<JournalpostDto> { it!!.journalpost }
                .extracting<AktorDto> { it.gjelderAktor }
                .extracting { it.ident }
                .`as`("ident")
                .isEqualTo(gjelderIdent)
        }

        @Test
        @DisplayName("skal mappe brevkode")
        fun skalMappeBrevkode() {
            testDataManager.opprett(KodeBrevBygger.enGyldigBrevkode().medKode("brev").medDekode("Et laaangt brev"))
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger.enJournalfortJournalpost().leggTilSaksnummer("1001").medBrevkode("brev"),
                )
            val response =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(JOURNAL_MED_SAK, prefixId(journalpost), "1001"),
                )
            Assertions
                .assertThat(response)
                .extracting({ it.statusCode })
                .isEqualTo(
                    HttpStatus.OK,
                )
            Assertions
                .assertThat(response)
                .extracting({ it.body })
                .extracting<JournalpostDto> { it!!.journalpost }
                .isNotNull
                .extracting<KodeDto> { it.brevkode }
                .`as`("brevkode")
                .isEqualTo(KodeDto("brev", "Et laaangt brev", true))
        }

        @Test
        @DisplayName("skal hente feilført journalpost")
        fun skalHenteFeilfortJournalpost() {
            val feilfortJournalpost =
                testDataManager.opprett(
                    JournalpostBygger.enJournalpostSomErFeilfort("1001").leggTilSaksnummer("1001"),
                )
            val response =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(JOURNAL_MED_SAK, prefixId(feilfortJournalpost), "1001"),
                )
            Assertions.assertThat(response).extracting({ it.statusCode }).isEqualTo(HttpStatus.OK)
            Assertions
                .assertThat(response)
                .extracting({ it.body })
                .extracting<JournalpostDto> { it!!.journalpost }
                .isNotNull
                .extracting<Boolean> { it.feilfort }
                .`as`("feilført")
                .isEqualTo(true)
        }

        @Test
        @DisplayName("skal hente en journalpost med kilde NAV_NO_BID når journalposten er registrert av BJOARK015")
        fun skalHenteJournalpostMedKildeDittNav() {
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .leggTilSaksnummer("1001")
                        .medBatchNavn(BATCH_NAVN_JOARK_15 + "registrerteDenneJournalposten"),
                )
            val response =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(JOURNAL_MED_SAK, prefixId(journalpost), "1001"),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(response.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions
                        .assertThat(response.body)
                        .`as`("journalpostResponse")
                        .extracting<JournalpostDto> { it!!.journalpost }
                        .extracting<Kanal> { it.kilde }
                        .`as`("kanal")
                        .isEqualTo(Kanal.NAV_NO_BID)
                },
            )
        }

        @Test
        @DisplayName("skal hente en journalpost med returDetaljer")
        fun skalHenteJournalpostMedReturDetaljer() {
            val returDato = LocalDate.parse("2020-01-02")
            val returBeskrivelse = "Beskrivelse for retur"
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger
                        .enJournalfortJournalpost()
                        .leggTilSaksnummer("1001")
                        .medRetur(LocalDate.parse("2022-05-06"), ReturDetaljerLogg(LocalDate.parse("2022-05-06"), returBeskrivelse))
                        .medRetur(returDato, ReturDetaljerLogg(returDato, returBeskrivelse))
                        .medBatchNavn(BATCH_NAVN_JOARK_15 + "registrerteDenneJournalposten"),
                )
            val response =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(JOURNAL_MED_SAK, prefixId(journalpost), "1001"),
                )
            val journalpostResponse = response.body!!.journalpost
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(response.statusCode).`as`("status").isEqualTo(HttpStatus.OK) },
                Executable { Assertions.assertThat(journalpostResponse!!.returDetaljer).`as`("ReturDetaljer").isNotNull() },
                Executable { Assertions.assertThat(journalpostResponse!!.returDetaljer!!.dato).`as`("returDato").isEqualTo(returDato) },
                Executable { Assertions.assertThat(journalpostResponse!!.returDetaljer!!.antall).`as`("antall").isEqualTo(2) },
                Executable { Assertions.assertThat(journalpostResponse!!.returDetaljer!!.logg!!.size).`as`("antallLogg").isEqualTo(2) },
                Executable {
                    Assertions
                        .assertThat(findReturDetaljerLoggByDate(returDato, journalpostResponse!!.returDetaljer!!.logg)!!.beskrivelse)
                        .`as`("logg beskrivelse")
                        .isEqualTo(returBeskrivelse)
                },
            )
        }
    }

    @Nested
    @DisplayName("saksjournal")
    internal inner class Sakjournal {
        @Test
        @DisplayName("skal hente tom liste fra controller")
        fun skalHenteTomListeFraController() {
            val response = httpHeaderTestRestTemplate.getForEntity<List<JournalpostDto>>(fromUrl("/sak/1001/journal"))
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(response)
                        .extracting(
                            { it.statusCode },
                        ).isEqualTo(HttpStatus.OK)
                },
                Executable {
                    Assertions
                        .assertThat(response)
                        .extracting<List<*>>(
                            { it.body },
                        ).asList()
                        .isEmpty()
                },
            )
        }

        @Test
        @DisplayName("should get result from controller when entities are found in database")
        fun shouldGetResultFromControllerWhenEntitiesAreFoundInDb() {
            val saksnr = "1002"
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medAvsender("Bert, Sky")
                    .leggTilSaksnummer(saksnr)
                    .medFagomrade(Fagomrade.BIDRAG_DATABASE),
            )
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medAvsender(
                        "Åberg, A",
                    ).leggTilSaksnummer(saksnr)
                    .medFagomrade(Fagomrade.BIDRAG_DATABASE),
            )
            val response = httpHeaderTestRestTemplate.getForEntity<List<JournalpostDto>>(fromUrl("/sak/$saksnr/journal"))
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(response)
                        .extracting(
                            { it.statusCode },
                        ).isEqualTo(HttpStatus.OK)
                },
                Executable { Assertions.assertThat(response.body).hasSize(2) },
            )
        }

        @Test
        @DisplayName("skal filtrere journalposter som er arkivert i Joark")
        fun shouldFilterJournalpostArchivedInJoark() {
            val saksnr = "10002"
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medAvsender("Bert, Sky")
                    .leggTilSaksnummerArkivertIJoark(saksnr, 123)
                    .medFagomrade(Fagomrade.BIDRAG_DATABASE),
            )
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medAvsender(
                        "Åberg, A",
                    ).leggTilSaksnummer(saksnr)
                    .medFagomrade(Fagomrade.BIDRAG_DATABASE),
            )
            val response = httpHeaderTestRestTemplate.getForEntity<List<JournalpostDto>>(fromUrl("/sak/$saksnr/journal"))
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(response)
                        .extracting(
                            { it.statusCode },
                        ).isEqualTo(HttpStatus.OK)
                },
                Executable { Assertions.assertThat(response.body).hasSize(1) },
            )
        }

        @Test
        @DisplayName("skal lese journalpost")
        fun skalLeseJournalpost() {
            val saksnr = "1003"
            val avsenderNavn = "Cula, Dr. A."
            val beskrivelse = "Dette er et testnotat"
            val dokumentreferanse = "1001"
            val dokumenttype = "N"
            val brukerident = "123456789"
            val journalfoertAv = "S. Vindel"
            val enhet = "Trygdekontoret"
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medAvsender(avsenderNavn)
                    .medBeskrivelse(beskrivelse)
                    .medDokumentdato(LocalDate.now().minusDays(1))
                    .medDokumentreferanse(dokumentreferanse)
                    .medDokumentType(dokumenttype)
                    .medFagomrade(Fagomrade.BIDRAG_DATABASE)
                    .medGjelder(brukerident)
                    .medJournaldato(LocalDate.now())
                    .medJournalfortAv(journalfoertAv)
                    .medJournalforendeEnhet(enhet)
                    .leggTilSaksnummer(saksnr)
                    .medJournalstatus(Journalstatus.JOURNALFORT),
            )
            val response =
                httpHeaderTestRestTemplate
                    .exchange(
                        fromUrl("/sak/$saksnr/journal"),
                        HttpMethod.GET,
                        null,
                        listeMedJournalpost(),
                    )
            org.junit.jupiter.api.Assertions.assertAll(
                {
                    Assertions
                        .assertThat(response)
                        .isNotNull
                        .extracting { it.statusCode }
                        .isEqualTo(HttpStatus.OK)
                },
                {
                    Assertions.assertThat(response.body).isNotNull
                    val brevlagerJournalpostDto = response.body!![0]
                    org.junit.jupiter.api.Assertions.assertAll(
                        Executable {
                            Assertions
                                .assertThat(
                                    brevlagerJournalpostDto.avsenderNavn,
                                ).`as`("avsenderNavn")
                                .isEqualTo(avsenderNavn)
                        },
                        Executable { Assertions.assertThat(brevlagerJournalpostDto.innhold).`as`("innhold").isEqualTo(beskrivelse) },
                        Executable {
                            Assertions
                                .assertThat(
                                    brevlagerJournalpostDto.dokumentDato,
                                ).`as`("dokumentDato")
                                .isEqualTo(LocalDate.now().minusDays(1))
                        },
                        Executable {
                            Assertions
                                .assertThat(brevlagerJournalpostDto.dokumenter)
                                .extracting<String, RuntimeException> { it.dokumentreferanse }
                                .`as`("dokumentreferanse")
                                .isEqualTo(listOf(dokumentreferanse))
                        },
                        Executable {
                            Assertions
                                .assertThat(brevlagerJournalpostDto.dokumenter)
                                .extracting<String, RuntimeException> { it.dokumentType }
                                .`as`("dokumentType")
                                .isEqualTo(listOf(dokumenttype))
                        },
                        Executable {
                            Assertions
                                .assertThat(brevlagerJournalpostDto.gjelderAktor)
                                .extracting<String> { it?.ident }
                                .`as`("gjelderAktor")
                                .isEqualTo(brukerident)
                        },
                        Executable {
                            Assertions
                                .assertThat(
                                    brevlagerJournalpostDto.journalfortDato,
                                ).`as`("journalfortDato")
                                .isEqualTo(LocalDate.now())
                        },
                        Executable {
                            Assertions.assertThat(brevlagerJournalpostDto.journalforendeEnhet).`as`("journalforendeEnhet").isEqualTo(enhet)
                        },
                        Executable {
                            Assertions
                                .assertThat(
                                    brevlagerJournalpostDto.journalfortAv,
                                ).`as`("journalfortAv")
                                .isEqualTo(journalfoertAv)
                        },
                        Executable { Assertions.assertThat(brevlagerJournalpostDto.journalpostId).`as`("journalpostId").isNotNull() },
                        Executable {
                            Assertions
                                .assertThat(
                                    brevlagerJournalpostDto.mottattDato,
                                ).`as`("mottattDato")
                                .isEqualTo(LocalDate.now())
                        },
                    )
                },
            )
        }

        @Test
        @DisplayName("skal lese journalpost med returdetaljer")
        fun skalLeseJournalpostMedReturDetaljer() {
            val saksnr = "1003555"
            val avsenderNavn = "Cula, Dr. A."
            val beskrivelse = "Dette er et testnotat"
            val dokumentreferanse = "1001"
            val dokumenttype = "N"
            val brukerident = "123456789"
            val journalfoertAv = "S. Vindel"
            val enhet = "Trygdekontoret"
            val returDato = LocalDate.parse("2020-01-02")
            val returBeskrivelse = "Beskrivels"
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medAvsender(avsenderNavn)
                    .medBeskrivelse(beskrivelse)
                    .medDokumentdato(LocalDate.now().minusDays(1))
                    .medDokumentreferanse(dokumentreferanse)
                    .medDokumentType(dokumenttype)
                    .medFagomrade(Fagomrade.BIDRAG_DATABASE)
                    .medGjelder(brukerident)
                    .medJournaldato(LocalDate.now())
                    .medRetur(LocalDate.parse("2021-02-03"), ReturDetaljerLogg(LocalDate.parse("2021-02-03"), returBeskrivelse))
                    .medRetur(returDato, ReturDetaljerLogg(returDato, returBeskrivelse))
                    .medJournalfortAv(journalfoertAv)
                    .medJournalforendeEnhet(enhet)
                    .leggTilSaksnummer(saksnr)
                    .medJournalstatus(Journalstatus.JOURNALFORT),
            )
            val response =
                httpHeaderTestRestTemplate
                    .exchange(
                        fromUrl("/sak/$saksnr/journal"),
                        HttpMethod.GET,
                        null,
                        listeMedJournalpost(),
                    )
            org.junit.jupiter.api.Assertions.assertAll(
                {
                    Assertions
                        .assertThat(response)
                        .isNotNull
                        .extracting { it.statusCode }
                        .isEqualTo(HttpStatus.OK)
                },
                {
                    Assertions.assertThat(response.body).isNotNull
                    val brevlagerJournalpostDto = response.body!![0]
                    org.junit.jupiter.api.Assertions.assertAll(
                        {
                            Assertions
                                .assertThat(
                                    brevlagerJournalpostDto.returDetaljer!!.dato,
                                ).`as`("returDato")
                                .isEqualTo(returDato)
                        },
                        {
                            Assertions
                                .assertThat(
                                    brevlagerJournalpostDto.returDetaljer!!.antall,
                                ).`as`("antallRetur")
                                .isEqualTo(2)
                        },
                        {
                            Assertions.assertThat(brevlagerJournalpostDto.returDetaljer!!.logg!!.size).`as`("returLog lengde").isEqualTo(2)
                        },
                        {
                            Assertions
                                .assertThat(
                                    brevlagerJournalpostDto.returDetaljer!!.logg!!.stream().anyMatch { (dato): ReturDetaljerLog ->
                                        dato == returDato
                                    },
                                ).isTrue()
                        },
                        {
                            Assertions
                                .assertThat(
                                    findReturDetaljerLoggByDate(
                                        returDato,
                                        brevlagerJournalpostDto.returDetaljer!!.logg,
                                    )!!.beskrivelse,
                                ).`as`("returLog beskrivelse")
                                .isEqualTo(returBeskrivelse)
                        },
                    )
                },
            )
        }

        @Test
        @DisplayName("skal bruke avsender fornavn når avsender er null")
        fun skalBrukeAvsenderFornavn() {
            val avsenderNavn = ", Svintung" // PS! Komma blir ikke lagret i databasen, men brukes til å skille fornavnet fra etternavet på personer
            val saksnr = "1004"
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medAvsender(avsenderNavn)
                    .leggTilSaksnummer(saksnr)
                    .medFagomrade(Fagomrade.BIDRAG_DATABASE),
            )
            val response =
                httpHeaderTestRestTemplate.exchange(
                    fromUrl("/sak/$saksnr/journal"),
                    HttpMethod.GET,
                    null,
                    listeMedJournalpost(),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                { Assertions.assertThat(response).isNotNull() },
                { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                {
                    Assertions.assertThat(response.body).isNotNull
                    val brevlagerJournalpostDto = response.body!![0]
                    Assertions.assertThat(brevlagerJournalpostDto.avsenderNavn).isEqualTo(avsenderNavn)
                },
            )
        }

        @Test
        @DisplayName("skal bruke skannet dato når journaldato er null")
        fun skalBrukeSkannetDato() {
            val skannetDato = LocalDate.now()
            val saksnr = "1005"
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medAvsender("Gideon")
                    .leggTilSaksnummer(saksnr)
                    .medFagomrade(Fagomrade.BIDRAG_DATABASE),
                skannetDato,
            )
            val response =
                httpHeaderTestRestTemplate.exchange(
                    fromUrl("/sak/$saksnr/journal"),
                    HttpMethod.GET,
                    null,
                    listeMedJournalpost(),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                { Assertions.assertThat(response).isNotNull() },
                { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                {
                    Assertions.assertThat(response.body).isNotNull
                    val mottattDato = response.body!![0].mottattDato
                    Assertions.assertThat(skannetDato).isEqualTo(mottattDato)
                },
            )
        }

        @Test
        @DisplayName("skal hente informasjon om journalsaken er feilført")
        fun skalHenteFeilfortInformasjonOmJournalsak() {
            val saksnr = "1006"
            testDataManager.opprett(
                JournalpostBygger.enJournalpostSomErFeilfort(saksnr).medAvsender("Bert, Sky").medFagomrade(Fagomrade.BIDRAG_DATABASE),
            )
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medAvsender(
                        "Åberg, A",
                    ).leggTilSaksnummer(saksnr)
                    .medFagomrade(Fagomrade.BIDRAG_DATABASE),
            )
            val response =
                httpHeaderTestRestTemplate.exchange(
                    fromUrl("/sak/$saksnr/journal"),
                    HttpMethod.GET,
                    null,
                    listeMedJournalpost(),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                {
                    Assertions
                        .assertThat(response)
                        .extracting { it.statusCode }
                        .isEqualTo(HttpStatus.OK)
                },
                {
                    val journalposter = response.body!!
                    Assertions.assertThat(journalposter).isNotNull.hasSize(2)
                    val journalpostForSkybert =
                        journalposter.firstOrNull { it.feilfort == true }
                    val journalpostForAlbert = journalposter.firstOrNull { it.feilfort == false }
                    org.junit.jupiter.api.Assertions.assertAll(
                        Executable {
                            Assertions
                                .assertThat(journalpostForSkybert)
                                .isNotNull
                                .extracting<Boolean> { it?.feilfort }
                                .`as`("Skybert")
                                .isEqualTo(true)
                        },
                        Executable {
                            Assertions
                                .assertThat(journalpostForAlbert)
                                .isNotNull
                                .extracting<Boolean> { it?.feilfort }
                                .`as`("Albert")
                                .isEqualTo(false)
                        },
                    )
                },
            )
        }

        @Test
        @DisplayName("skal hente journalpost med journalpostens fagområde")
        fun skalHenteJournalpostForFagomrade() {
            val saksnr = "1007"
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medFagomrade("IKKE FARSKAP")
                    .medAvsender("Tang, Tore")
                    .leggTilSaksnummer(saksnr),
            )
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medFagomrade(Fagomrade.FARSKAP)
                    .medAvsender("Treigen, Banan")
                    .leggTilSaksnummer(saksnr),
            )
            val response =
                httpHeaderTestRestTemplate.exchange(
                    fromUrl("/sak/$saksnr/journal", "fagomrade=" + Fagomrade.FARSKAP),
                    HttpMethod.GET,
                    null,
                    listeMedJournalpost(),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                { Assertions.assertThat(response).isNotNull() },
                { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                {
                    Assertions.assertThat(response.body).hasSize(1)
                    Assertions
                        .assertThat(response.body)
                        .extracting<String, RuntimeException> { it.fagomrade }
                        .isEqualTo(listOf(Fagomrade.FARSKAP))
                },
            )
        }

        fun fromUrl(
            url: String,
            vararg querius: String,
        ): String {
            val uriBuilder = buildHttpUrlWithContextPathAndPort(url)
            if (querius.isNotEmpty()) {
                Arrays.stream(querius).forEach { query: String -> uriBuilder.query(query) }
            } else {
                uriBuilder.queryParam("fagomrade", Fagomrade.BIDRAG)
            }
            return uriBuilder.toUriString()
        }

        private fun buildHttpUrlWithContextPathAndPort(url: String): UriComponentsBuilder = UriComponentsBuilder.fromUriString(makeContextPath() + url)
    }

    @Nested
    @DisplayName("Journalpost, uten saksnummer som parameter")
    internal inner class JournalpostPathUtenSak {
        @Test
        @DisplayName("skal ha http status 400 (BAD_REQUEST) når journalpostId prefix er ugyldig")
        fun skalHaHttpStatusBadRequestVedUgyldJournalpostIdPrefix() {
            val response = httpHeaderTestRestTemplate.getForEntity<JournalpostDto>(String.format(JOURNAL_UTEN_SAK, "1"))
            Assertions.assertThat(response).extracting({ it.statusCode }).isEqualTo(
                HttpStatus.BAD_REQUEST,
            )
        }

        @Test
        @DisplayName("skal ha http status 404 (NOT_FOUND) når journalpost ikke finnes")
        fun skalHaHttpStatusNoContentVedUkjentJournalpost() {
            val response = httpHeaderTestRestTemplate.getForEntity<JournalpostDto>(String.format(JOURNAL_UTEN_SAK, "BID-12345"))
            Assertions.assertThat(response).extracting({ it.statusCode }).isEqualTo(
                HttpStatus.NOT_FOUND,
            )
        }

        @Test
        @DisplayName("skal ha http status 200 (OK) når man henter en journalpost filtrert på journalstatus.")
        fun skalReturnereStatusNarDetHentesJournalpostFiltrertPaJournalstatus() {
            val journalpost =
                testDataManager.opprett(JournalpostBygger.enJournalfortJournalpost().medJournalstatus(Journalstatus.MOTTAKSREGISTRERT))
            val hentJpResponse =
                httpHeaderTestRestTemplate.getForEntity<JournalpostDto>(String.format(JOURNAL_UTEN_SAK, "BID-" + journalpost.journalpostId))
            Assertions.assertThat(hentJpResponse.statusCode).isEqualTo(HttpStatus.OK)
        }

        @Test
        @DisplayName("skal hente journalpost med journalstatus reservert")
        fun skalHenteJournalpost() {
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger.enJournalfortJournalpost().medJournalstatus(Journalstatus.RESERVERT),
                )
            val response =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(JOURNAL_UTEN_SAK, "BID-" + journalpost.journalpostId),
                )
            Assertions
                .assertThat(response)
                .extracting({ it.statusCode })
                .isEqualTo(
                    HttpStatus.OK,
                )
            Assertions.assertThat(response.body).isNotNull
            Assertions.assertThat(response.body!!.journalpost).extracting<String> { it?.journalstatus }.isEqualTo(
                Journalstatus.RESERVERT,
            )
        }

        @Test
        @DisplayName("skal hente Journalpost sammen med sakstilknytninger")
        fun skalHenteJournalpostMedSakstilkn() {
            val journalpost =
                testDataManager.opprett(JournalpostBygger.enJournalfortJournalpost().medJournalstatus("M").leggTilSaksnummer("1001001"))
            val response =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(JOURNAL_UTEN_SAK, "BID-" + journalpost.journalpostId),
                )
            Assertions
                .assertThat(response)
                .extracting({ it.statusCode })
                .isEqualTo(
                    HttpStatus.OK,
                )
            Assertions.assertThat(response.body).isNotNull
            Assertions.assertThat(response.body!!.sakstilknytninger).isEqualTo(listOf("1001001"))
        }

        @Test
        @DisplayName("skal sjekke tilgang til gjelder aktør når det ikke er tilknyttede saker")
        fun skalSjekkeTilgangTilAktorNarDetIkkeErTilknyttedeSaker() {
            val gjelderIdent = genererFødselsnummer()
            val journalpost = testDataManager.opprett(JournalpostBygger.enJournalfortJournalpost().medGjelder(gjelderIdent).utenSak())
            val response =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(JOURNAL_UTEN_SAK, "BID-" + journalpost.journalpostId),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(response)
                        .extracting(
                            { it.statusCode },
                        ).isEqualTo(HttpStatus.OK)
                },
                Executable { Mockito.verify(tilgangskontrollServiceMock).sjekkTilgangPerson(gjelderIdent) },
            )
        }
    }

    @Nested
    @DisplayName("json-requests")
    internal inner class JsonRequests {
        @Test
        @DisplayName("skal sette avsendernavn ved journalføring av journalpost")
        fun skalSetteAvsendernavnVedJournalforingAvJournalpost() {
            val journalpost =
                testDataManager.opprett(
                    JournalpostBygger.enJournalfortJournalpost().utenSak().medJournalstatus(Journalstatus.MOTTAKSREGISTRERT),
                )
            val json =
                java.lang.String.join(
                    "\n",
                    "{",
                    "\"skalJournalfores\":true,",
                    "\"journalforendeEnhet\":\"4806\",",
                    "\"dokumentDato\":\"2020-01-01\",",
                    "\"fagomrade\":\"BID\",",
                    "\"journaldato\":\"2019-10-25\",",
                    "\"gjelder\":\"25018512345\",",
                    "\"tittel\":\"Millad\",",
                    "\"tilknyttSaker\":[ \"1500000\" ],",
                    "\"avsenderNavn\":\"DELEVIELEUSE BAIJAN-MASK\",",
                    "\"endreDokumenter\": []",
                    "}",
                )
            val registrerJpResponse =
                httpHeaderTestRestTemplate.exchange<Void>(
                    String.format(JOURNAL_UTEN_SAK, prefixId(journalpost)),
                    HttpMethod.PATCH,
                    initHttpEntity(json, CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
                )
            Assertions.assertThat(registrerJpResponse.statusCode).isEqualTo(HttpStatus.OK)
            val hentJpResponse =
                httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(
                    String.format(JOURNAL_UTEN_SAK, prefixId(journalpost)),
                )
            Assertions.assertThat(hentJpResponse.statusCode).`as`("status code").isEqualTo(HttpStatus.OK)
            val hentetJournalpost = if (hentJpResponse.body != null) hentJpResponse.body!!.journalpost else JournalpostDto()
            org.junit.jupiter.api.Assertions.assertAll(
                {
                    Assertions
                        .assertThat(hentetJournalpost)
                        .extracting<String> { it?.journalstatus }
                        .`as`("journalstatus")
                        .isEqualTo("J")
                },
                {
                    Assertions
                        .assertThat(hentJpResponse)
                        .extracting { it.body }
                        .extracting<List<String>> { it?.sakstilknytninger }
                        .`as`("sakstilknytninger")
                        .isEqualTo(listOf("1500000"))
                },
                {
                    Assertions
                        .assertThat(hentetJournalpost)
                        .extracting<String> { it?.avsenderNavn }
                        .isEqualTo("DELEVIELEUSE BAIJAN-MASK")
                },
            )
        }
    }

    private fun findReturDetaljerLoggByDate(
        date: LocalDate,
        returDetaljerLoggs: List<ReturDetaljerLog?>?,
    ): ReturDetaljerLog? = returDetaljerLoggs!!
        .stream()
        .filter { it: ReturDetaljerLog? -> it!!.dato == date }
        .findFirst()
        .orElse(null)

    companion object {
        private const val JOURNAL_MED_SAK = "/journal/%s?saksnummer=%s"
        private const val JOURNAL_UTEN_SAK = "/journal/%s"
    }
}
