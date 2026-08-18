package no.nav.bidrag.dokument.journalpost.service

import no.nav.bidrag.commons.web.HttpResponse.Companion.from
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager
import no.nav.bidrag.dokument.journalpost.consumer.NorgConsumer
import no.nav.bidrag.dokument.journalpost.consumer.SaksbehandlerConsumer
import no.nav.bidrag.dokument.journalpost.dto.BehandleAvvikRequestBuilder
import no.nav.bidrag.dokument.journalpost.dto.OpprettOppgaveResponse
import no.nav.bidrag.dokument.journalpost.dto.Saksbehandler
import no.nav.bidrag.dokument.journalpost.entity.Journalpost
import no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger
import no.nav.bidrag.dokument.journalpost.exception.JournalpostIkkeFunnetException
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer
import no.nav.bidrag.dokument.journalpost.model.Avviksbehandling
import no.nav.bidrag.dokument.journalpost.model.Avvikstype
import no.nav.bidrag.dokument.journalpost.model.BehandleAvvikRequest
import no.nav.bidrag.dokument.journalpost.model.BehandleAvvikResponse
import no.nav.bidrag.dokument.journalpost.model.Fagomrade
import no.nav.bidrag.dokument.journalpost.model.GyldigAvviksbehandling
import no.nav.bidrag.dokument.journalpost.model.Journalstatus
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository
import no.nav.bidrag.dokument.journalpost.utils.timestampCorrelationIdForThread
import no.nav.bidrag.transport.dokument.AvvikType
import no.nav.bidrag.transport.dokument.Avvikshendelse
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.time.LocalDate
import java.util.*
import java.util.Map

private const val TREKK_AVVIK = "This is SPARTA!"

@Transactional
@ActiveProfiles(BidragDokumentJournalpostProfiles.TEST)
@DisplayName("AvvikService")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [BidragDokumentJournalpostLocalTest::class],
)
@EnableWireMock(value = [ConfigureWireMock(port = 0)])
@EnableMockOAuth2Server
internal class AvvikServiceTest {
    @Autowired
    private val avvikService: AvvikService? = null

    @MockitoBean
    private val journalpostKafkaEventProducerMock: JournalpostKafkaEventProducer? = null

    @MockitoBean
    private val journalpostRepositoryMock: JournalpostRepository? = null

    @MockitoBean
    private val kodeServiceMock: KodeService? = null

    @MockitoBean
    private val norgConsumerMock: NorgConsumer? = null

    @MockitoBean
    private val oppgaveServiceMock: OppgaveService? = null

    @MockitoBean
    private val saksbehandlerConsumerMock: SaksbehandlerConsumer? = null

    @MockitoBean
    private val saksbehandlerOidcTokenManagerMock: SaksbehandlerOidcTokenManager? = null

    @MockitoBean
    private val tokenInformationServiceMock: TokenInformationService? = null

    @BeforeEach
    fun mockTokenInformation() {
        Mockito.`when`(tokenInformationServiceMock!!.hentSaksbehandlersBrukerid()).thenReturn("na")
        Mockito.`when`(tokenInformationServiceMock.hentSaksbehandlersNavn()).thenReturn("na")
    }

    @BeforeEach
    fun createCorrelationIdForThread() {
        timestampCorrelationIdForThread("avvik-service-test")
    }

    @Nested
    @DisplayName("finne avvik")
    internal inner class FinneAvvik {
        @Test
        @DisplayName("skal ikke finne avviket 'BESTILL_ORIGINAL' på journalpost når orginal allerede er bestilt")
        fun skalIkkeFinneAvviketBestillOrginalPaJournalpost() {
            Mockito.`when`(journalpostRepositoryMock!!.findById(123)).thenReturn(
                JournalpostBygger.enJournalpost().medOrginalBestilt().hentMuligJournalpost(),
            )
            val listeMedAvvik =
                avvikService!!
                    .finnAvvik(123, "007")
                    .hentListeMedAvvik()
            Assertions.assertThat(listeMedAvvik).doesNotContain(AvvikType.BESTILL_ORIGINAL)
        }

        @Test
        @DisplayName("skal finne avviket 'BESTILL_ORIGINAL' på journalpost når den ikke er bestilt fra før")
        fun skalFinneAvviketBestillOrginalPaJournalpost() {
            Mockito.`when`(kodeServiceMock!!.skalVise(ArgumentMatchers.any())).thenReturn(true)
            Mockito.`when`(journalpostRepositoryMock!!.findById(123)).thenReturn(
                JournalpostBygger
                    .enJournalpost()
                    .medSkannetDato(LocalDate.now())
                    .medDokumentType("I")
                    .utenOrginalBestilt()
                    .leggTilSaksnummer("007")
                    .hentMuligJournalpost(),
            )
            val listeMedAvvik =
                avvikService!!
                    .finnAvvik(123, "007")
                    .hentListeMedAvvik()
            Assertions.assertThat(listeMedAvvik).contains(AvvikType.BESTILL_ORIGINAL)
        }

        @Test
        @DisplayName("skal ikke finne avviket 'TREKK_JOURNALPOST' når det er en journalpost uten journalstatus mottaksregistrert")
        fun skalIkkeFinneAvvikUtenMottaksregistrertJournalpost() {
            Mockito.`when`(journalpostRepositoryMock!!.findById(101)).thenReturn(
                JournalpostBygger
                    .enJournalpost()
                    .medJournalstatus(Journalstatus.JOURNALFORT)
                    .hentMuligJournalpost(),
            )
            val funnetAvvik =
                avvikService!!
                    .finnAvvik(101)
                    .hentListeMedAvvik()
            Assertions.assertThat(funnetAvvik).doesNotContain(AvvikType.TREKK_JOURNALPOST)
        }

        @Test
        @DisplayName("skal finne avviket 'TREKK_JOURNALPOST' når det er en journalpost med journalstatus mottaksregistrert")
        fun skalFinneAvvikMedMottaksregistrertJournalpost() {
            Mockito.`when`(kodeServiceMock!!.skalVise(ArgumentMatchers.any())).thenReturn(false)
            Mockito.`when`(journalpostRepositoryMock!!.findById(101)).thenReturn(
                JournalpostBygger
                    .enJournalpost()
                    .medJournalstatus(Journalstatus.MOTTAKSREGISTRERT)
                    .hentMuligJournalpost(),
            )
            val funnetAvvik =
                avvikService!!
                    .finnAvvik(101)
                    .hentListeMedAvvik()
            Assertions.assertThat(funnetAvvik).contains(AvvikType.TREKK_JOURNALPOST)
        }
    }

    @Nested
    @DisplayName("behandle avvik")
    internal inner class BehandleAvvik {
        @Test
        @DisplayName("skal feile når journalpost ikke finnes")
        fun skalFeileNarJournalpostIkkeFinnes() {
            Mockito
                .`when`(journalpostRepositoryMock!!.findById(ArgumentMatchers.anyInt()))
                .thenReturn(
                    Optional.empty(),
                )
            Assertions
                .assertThatExceptionOfType(
                    JournalpostIkkeFunnetException::class.java,
                ).isThrownBy {
                    avvikService!!.behandleAvvik(
                        BehandleAvvikRequest(
                            Avvikshendelse(Avvikstype.BESTILL_ORIGINAL.name, ""),
                            "101",
                            1,
                        ),
                    )
                }.withMessage("Fant ikke journalpost med id lik 1")
        }

        @Test
        @DisplayName("skal feile ved oppdatering av arkiveringsstatus hvis journalpost ikke er reservert")
        fun skalFeileVedOppdateringAvArkiveringsstatusHvisJournalpostIkkeErReservert() {
            val journalpost =
                JournalpostBygger
                    .enJournalpost()
                    .medJournalpostId(1)
                    .medJournalstatus(Journalstatus.MOTTAKSREGISTRERT)
                    .hent()
            Mockito
                .`when`(journalpostRepositoryMock!!.findById(ArgumentMatchers.anyInt()))
                .thenReturn(
                    Optional.of(journalpost),
                )
            val statusResponse =
                avvikService!!.behandleAvvik(
                    BehandleAvvikRequest(
                        Avvikshendelse(Avvikstype.ARKIVERE_JOURNALPOST.name, "", "en sak"),
                        "4802",
                        journalpost.journalpostId,
                    ),
                )
            Assertions
                .assertThat(statusResponse)
                .extracting { obj: BehandleAvvikResponse -> obj.erUgyldig() }
                .isEqualTo(true)
        }

        @Test
        @DisplayName("skal skal gjennomføre oppdatering av arkiveringsstatus hvis journalpost er klar til print")
        fun skalGjennomforeOppdateringAvArkiveringsstatusHvisJournalpostErKlarTilPrint() {
            val saksnr = "1900000"
            val enhet = "4802"
            val journalpost =
                JournalpostBygger
                    .enJournalpost()
                    .medJournalpostId(1)
                    .medJournalstatus(Journalstatus.KLAR_TIL_PRINT)
                    .leggTilSaksnummer(saksnr)
                    .hent()
            Mockito
                .`when`(journalpostRepositoryMock!!.findById(ArgumentMatchers.anyInt()))
                .thenReturn(
                    Optional.of(journalpost),
                )
            val statusResponse =
                avvikService!!.behandleAvvik(
                    BehandleAvvikRequest(
                        Avvikshendelse(
                            Avvikstype.ARKIVERE_JOURNALPOST.name,
                            enhet,
                            Map.of("joarkArkiveringStatus", "STARTET"),
                            saksnr,
                        ),
                        enhet,
                        journalpost.journalpostId,
                    ),
                )
            Assertions
                .assertThat(statusResponse)
                .extracting { obj: BehandleAvvikResponse -> obj.erUgyldig() }
                .isEqualTo(false)
        }

        @Test
        @DisplayName(
            "skal ikke være ugyldig avviksbehandling når status 201 CREATED er gitt fra OppgaveConsumer når det er bestilt orginal",
        )
        fun skalOppretteAvvikshendelsePaJournalpost() {
            val journalpost =
                JournalpostBygger
                    .enJournalpost()
                    .medJournalpostId(1)
                    .medDokumentType("I")
                    .medSkannetDato(LocalDate.now())
                    .utenOrginalBestilt()
                    .hent()
            Mockito
                .`when`(journalpostRepositoryMock!!.findById(ArgumentMatchers.anyInt()))
                .thenReturn(
                    Optional.of(journalpost),
                )
            Mockito
                .`when`(saksbehandlerConsumerMock!!.hentSaksbehandler(ArgumentMatchers.any()))
                .thenReturn(
                    Optional.empty(),
                )
            Mockito
                .`when`(
                    oppgaveServiceMock!!.opprettOppgave(
                        ArgumentMatchers.any(
                            Avviksbehandling::class.java,
                        ),
                    ),
                ).thenReturn(from(HttpStatus.CREATED, OpprettOppgaveResponse()))
            val statusResponse =
                avvikService!!.behandleAvvik(
                    BehandleAvvikRequest(
                        Avvikshendelse(Avvikstype.BESTILL_ORIGINAL.name, "", "en sak"),
                        "1001",
                        journalpost.journalpostId,
                    ),
                )
            Assertions
                .assertThat(statusResponse)
                .extracting { obj: BehandleAvvikResponse -> obj.erUgyldig() }
                .isEqualTo(false)
        }

        @Test
        @DisplayName("skal være ugyldig avviksbehandling når orginal er bestilt tidligere")
        fun skalVaereUgyldigAvviksbehandlingNarOrginalErBestiltTidligere() {
            val journalpost = JournalpostBygger.enJournalpost().medOrginalBestilt().hent()
            Mockito
                .`when`(journalpostRepositoryMock!!.findById(ArgumentMatchers.anyInt()))
                .thenReturn(
                    Optional.of(journalpost),
                )
            val behandleAvvikResponse =
                avvikService!!.behandleAvvik(
                    BehandleAvvikRequest(
                        Avvikshendelse(Avvikstype.BESTILL_ORIGINAL.name, ""),
                        "1001",
                        1,
                    ),
                )
            Assertions
                .assertThat(behandleAvvikResponse)
                .extracting { obj: BehandleAvvikResponse -> obj.erUgyldig() }
                .isEqualTo(true)
        }

        @Test
        @DisplayName("skal lage oppgave når avvik opprettes")
        fun skalLageOppgaveNarAvvikOpprettes() {
            val journalpost =
                JournalpostBygger
                    .enJournalpost()
                    .medJournalpostId(1)
                    .medDokumentType("I")
                    .medSkannetDato(LocalDate.now())
                    .utenOrginalBestilt()
                    .hent()
            Mockito
                .`when`(journalpostRepositoryMock!!.findById(ArgumentMatchers.anyInt()))
                .thenReturn(
                    Optional.of(journalpost),
                )
            Mockito
                .`when`(
                    oppgaveServiceMock!!.opprettOppgave(ArgumentMatchers.any()),
                ).thenReturn(
                    from(
                        HttpStatus.CREATED,
                        OpprettOppgaveResponse(),
                    ),
                )
            Mockito
                .`when`(saksbehandlerConsumerMock!!.hentSaksbehandler(ArgumentMatchers.any()))
                .thenReturn(
                    Optional.empty(),
                )
            avvikService!!.behandleAvvik(
                BehandleAvvikRequest(
                    Avvikshendelse(Avvikstype.BESTILL_ORIGINAL.name, "", "en sak"),
                    "1001",
                    1,
                ),
            )
            val avviksbehandlingCaptor =
                ArgumentCaptor.forClass(
                    Avviksbehandling::class.java,
                )
            Mockito.verify(oppgaveServiceMock).opprettOppgave(avviksbehandlingCaptor.capture())
            val avviksbehandling = avviksbehandlingCaptor.value
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(avviksbehandling.avvikstype)
                        .`as`("avvikstype")
                        .isEqualTo(Avvikstype.BESTILL_ORIGINAL)
                },
                Executable {
                    Assertions
                        .assertThat(avviksbehandling.hentOppgave())
                        .`as`("oppgave")
                        .isPresent()
                },
            )
        }

        @Test
        @DisplayName("skal ikke lage oppgave når avviket allerede er opprettet")
        fun skalIkkeLageOppgaveNarAvviketAlleredeErOpprettet() {
            val journalpost = JournalpostBygger.enJournalpost().medOrginalBestilt().hent()
            Mockito
                .`when`(journalpostRepositoryMock!!.findById(ArgumentMatchers.anyInt()))
                .thenReturn(
                    Optional.of(journalpost),
                )
            avvikService!!.behandleAvvik(
                BehandleAvvikRequest(
                    Avvikshendelse(Avvikstype.BESTILL_ORIGINAL.name, ""),
                    "1001",
                    1,
                ),
            )
            Mockito.verify(oppgaveServiceMock, Mockito.never())?.opprettOppgave(
                ArgumentMatchers.any(
                    Avviksbehandling::class.java,
                ),
            )
        }

        @Test
        @DisplayName("skal hente saksbehandler navn og ident til avvik av typen BESTILL_ORIGINAL")
        fun skalHenteSaksbehandlersinfoTilAvvikstypeBestillOrginal() {
            val journalpost =
                JournalpostBygger
                    .enJournalpost()
                    .medJournalpostId(1001)
                    .medSkannetDato(LocalDate.now())
                    .medDokumentType("I")
                    .utenOrginalBestilt()
                    .hent()
            Mockito
                .`when`(saksbehandlerOidcTokenManagerMock!!.hentSaksbehandler())
                .thenReturn("z123456")
            Mockito
                .`when`(journalpostRepositoryMock!!.findById(ArgumentMatchers.anyInt()))
                .thenReturn(
                    Optional.of(journalpost),
                )
            Mockito
                .`when`(saksbehandlerConsumerMock!!.hentSaksbehandler("z123456"))
                .thenReturn(Optional.of(Saksbehandler("z123456", "Tore Tang")))
            Mockito
                .`when`(
                    oppgaveServiceMock!!.opprettOppgave(
                        ArgumentMatchers.any(
                            Avviksbehandling::class.java,
                        ),
                    ),
                ).thenReturn(from(HttpStatus.CREATED, OpprettOppgaveResponse()))
            avvikService!!.behandleAvvik(
                BehandleAvvikRequest(
                    Avvikshendelse(Avvikstype.BESTILL_ORIGINAL.name, "666", "en sak"),
                    "123",
                    1,
                ),
            )
            val oppgaveCaptor =
                ArgumentCaptor.forClass(
                    Avviksbehandling::class.java,
                )
            Mockito.verify(oppgaveServiceMock).opprettOppgave(oppgaveCaptor.capture())
            val avviksoppgave = oppgaveCaptor.value
            Assertions
                .assertThat(avviksoppgave.hentOppgave())
                .`as`("oppgaveBeskrivelse")
                .isPresent()
            val oppgaveBeskrivelse = avviksoppgave.hentOppgave().get().beskrivelse
            Assertions
                .assertThat(oppgaveBeskrivelse)
                .`as`("oppgave.beskrivelse")
                .contains("og merkes med z123456 - Tore Tang")
        }

        @Test
        @DisplayName("skal opprette avviksoppgave ved BESTILL SPLITTING")
        fun skalOppretteAvviksoppgaveVedBestillSplitting() {
            val journalpost =
                JournalpostBygger
                    .enJournalpost()
                    .medJournalpostId(101)
                    .medDokumentType("I")
                    .medFagomrade(Fagomrade.BIDRAG)
                    .medBatchNavn("IKKE BJOARK BATCH")
                    .medFilnavn("dokumentet.pdf")
                    .medSkannetDato(LocalDate.now())
                    .utenSak()
                    .hent()
            Mockito
                .`when`(journalpostRepositoryMock!!.findById(ArgumentMatchers.anyInt()))
                .thenReturn(
                    Optional.of(journalpost),
                )
            Mockito
                .`when`(
                    oppgaveServiceMock!!.opprettOppgave(
                        ArgumentMatchers.any(
                            Avviksbehandling::class.java,
                        ),
                    ),
                ).thenReturn(
                    from(HttpStatus.CREATED, OpprettOppgaveResponse()),
                )
            val behandleAvvikResponse =
                avvikService!!.behandleAvvik(
                    BehandleAvvikRequest(
                        Avvikshendelse(
                            Avvikstype.BESTILL_SPLITTING.name,
                            "deromkring",
                            Map.of(),
                            "en sak",
                        ),
                        "123",
                        1,
                    ),
                )
            val avviksbehandlingCaptor =
                ArgumentCaptor.forClass(
                    Avviksbehandling::class.java,
                )
            Mockito.verify(oppgaveServiceMock).opprettOppgave(avviksbehandlingCaptor.capture())
            val avviksbehandling = avviksbehandlingCaptor.value
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(behandleAvvikResponse.erUgyldig())
                        .`as`("ugyldig status")
                        .isEqualTo(false)
                },
                Executable {
                    Assertions
                        .assertThat(avviksbehandling)
                        .`as`("gyldig avviksbehandling")
                        .isInstanceOf(
                            GyldigAvviksbehandling::class.java,
                        )
                },
                Executable {
                    Assertions.assertThat(avviksbehandling.hentOppgave()).isPresent()
                    val beskrivelse = avviksbehandling.hentOppgave().get().beskrivelse
                    org.junit.jupiter.api.Assertions.assertAll(
                        Executable {
                            Assertions
                                .assertThat(beskrivelse)
                                .`as`("beskrivelse")
                                .contains("Bestill splitting av dokument")
                        },
                        Executable {
                            Assertions
                                .assertThat(beskrivelse)
                                .`as`("beskrivelse")
                                .contains("\"deromkring\"")
                        },
                        Executable {
                            Assertions
                                .assertThat(beskrivelse)
                                .`as`("beskrivelse")
                                .contains("Dokumentet har filnavn \"dokumentet.pdf\" og ble skannet " + LocalDate.now() + ".")
                        },
                        Executable {
                            Assertions
                                .assertThat(beskrivelse)
                                .`as`("beskrivelse")
                                .contains("Batchnavn: IKKE BJOARK BATCH.")
                        },
                    )
                },
            )
        }
    }

    @Nested
    @DisplayName("TREKK_JOURNALPOST")
    internal inner class TrekkJournalpost {
        @Test
        @DisplayName("skal få ugyldig behandling i response når journalpost som trekkes ikke har journalstatus mottaksregistrert")
        fun skalFaUgyldigBehandleAvvikResponseNarJournalpostIkkeHarJournalstatusMottaksregistrert() {
            val journalpostId = 101
            val journalpost =
                JournalpostBygger
                    .enJournalpost()
                    .medJournalpostId(journalpostId)
                    .medJournalstatus(Journalstatus.JOURNALFORT)
                    .hent()
            Mockito
                .`when`(journalpostRepositoryMock!!.findById(journalpostId))
                .thenReturn(Optional.of(journalpost))
            val behandleAvvikResponse =
                avvikService!!.behandleAvvik(
                    BehandleAvvikRequestBuilder
                        .enBehandleAvvikRequest(Avvikstype.TREKK_JOURNALPOST)
                        .medJournalpostId(journalpostId)
                        .medBeskrivelse(TREKK_AVVIK)
                        .bygg(),
                )
            Assertions
                .assertThat(behandleAvvikResponse)
                .extracting { obj: BehandleAvvikResponse -> obj.erUgyldig() }
                .isEqualTo(true)
        }

        @Test
        @DisplayName("skal sette journalstatusen til utgår (U) når journalposten trekkes")
        fun skalSetteJournalstatusForJournalpostSomTrekkes() {
            val journalpostId = 101
            val journalpost =
                JournalpostBygger
                    .enJournalpost()
                    .medJournalpostId(journalpostId)
                    .medJournalstatus(Journalstatus.MOTTAKSREGISTRERT)
                    .hent()
            Mockito
                .`when`(journalpostRepositoryMock!!.findById(journalpostId))
                .thenReturn(Optional.of(journalpost))
            val behandleAvvikResponse =
                avvikService!!.behandleAvvik(
                    BehandleAvvikRequestBuilder
                        .enBehandleAvvikRequest(Avvikstype.TREKK_JOURNALPOST)
                        .medJournalpostId(journalpostId)
                        .bygg(),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(behandleAvvikResponse)
                        .extracting { obj: BehandleAvvikResponse -> obj.erUgyldig() }
                        .isEqualTo(false)
                },
                Executable {
                    Assertions
                        .assertThat(journalpost)
                        .extracting { obj: Journalpost -> obj.journalstatus }
                        .isEqualTo(Journalstatus.UTGAR)
                },
            )
        }
    }

    @AfterEach
    fun resetMocks() {
        Mockito.reset(
            journalpostKafkaEventProducerMock,
            journalpostRepositoryMock,
            kodeServiceMock,
            norgConsumerMock,
            oppgaveServiceMock,
            saksbehandlerConsumerMock,
            saksbehandlerOidcTokenManagerMock,
            tokenInformationServiceMock,
        )
    }
}
