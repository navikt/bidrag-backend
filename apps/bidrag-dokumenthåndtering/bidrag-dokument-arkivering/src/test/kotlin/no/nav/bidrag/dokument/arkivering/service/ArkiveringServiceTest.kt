package no.nav.bidrag.dokument.arkivering.service

import no.nav.bidrag.commons.web.HttpResponse.Companion.from
import no.nav.bidrag.dokument.arkivering.BidragDokumentArkivering
import no.nav.bidrag.dokument.arkivering.BidragDokumentArkiveringLocal
import no.nav.bidrag.dokument.arkivering.config.BidragDokumentArkiveringConfig.SaksbehandlerOidcTokenManager
import no.nav.bidrag.dokument.arkivering.consumer.BidragDokumentConsumer
import no.nav.bidrag.dokument.arkivering.consumer.JournalpostApiConsumer
import no.nav.bidrag.dokument.arkivering.dto.ArkiverDecision
import no.nav.bidrag.dokument.arkivering.dto.ArkivereJournalpostResponse
import no.nav.bidrag.dokument.arkivering.dto.DokumentInfo
import no.nav.bidrag.dokument.arkivering.dto.JournalStatus
import no.nav.bidrag.dokument.arkivering.exceptions.HentingAvDokumentFeiletException
import no.nav.bidrag.dokument.arkivering.exceptions.JournalpostHarFlereEnnEnSakException
import no.nav.bidrag.dokument.arkivering.exceptions.JournalpostHarIkkeGyldigStatusException
import no.nav.bidrag.dokument.arkivering.exceptions.JournalpostIkkeFunnetException
import no.nav.bidrag.dokument.arkivering.testutil.TestdataUtil.mockDokumentbehandlingConsumerRetur
import no.nav.bidrag.dokument.arkivering.testutil.TestdataUtil.mockJournalpostResponse
import no.nav.bidrag.transport.dokument.JournalpostStatus
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.io.IOException
import java.util.List

@DisplayName("ArkiveringService")
@SpringBootTest(classes = [BidragDokumentArkiveringLocal::class])
@ActiveProfiles(BidragDokumentArkivering.PROFILE_TEST)
@EnableMockOAuth2Server
class ArkiveringServiceTest {
    @MockitoBean
    private val bidragDokumentConsumer: BidragDokumentConsumer? = null

    @MockitoBean
    private val saksbehandlerOidcTokenManager: SaksbehandlerOidcTokenManager? = null

    @MockitoBean
    private val journalpostApiConsumer: JournalpostApiConsumer? = null

    @Autowired
    private val arkiveringService: ArkiveringService? = null

    @Test
    @DisplayName("Skal arkivere journalpost dersom dokument returneres fra midlertidig brevlager")
    @Throws(
        IOException::class,
    )
    fun skalArkivereJournalpostHvisDokumentReturneresFraBrevlager() {
        // given
        val jpIdBidrag = "18902067"
        val jpIdJoark = "467018752"
        val joarkDokumentId = "123456789"
        val journalstatus = JournalStatus.UGAAENDE_MED_DOKUMENTVARIANTER.kode
        val melding = "Testing 1-2"
        val journalpostResponse = mockJournalpostResponse()
        val fysiskDokument = mockDokumentbehandlingConsumerRetur()
        val saksbehandler = "X123456"
        Mockito
            .`when`(
                bidragDokumentConsumer!!.hentDokument(
                    journalpostResponse.journalpost!!.journalpostId!!,
                ),
            ).thenReturn(fysiskDokument.get())
        Mockito
            .`when`(
                bidragDokumentConsumer.hentBidragJournalpost(jpIdBidrag),
            ).thenReturn(from(HttpStatus.OK, journalpostResponse))
        Mockito
            .`when`(saksbehandlerOidcTokenManager!!.hentSaksbehandler())
            .thenReturn(saksbehandler)
        Mockito
            .`when`(bidragDokumentConsumer.kanArkivereJournalpost(jpIdBidrag))
            .thenReturn(ArkiverDecision(true, null))
        Mockito
            .`when`(
                journalpostApiConsumer!!.arkivereJournalpost(journalpostResponse, fysiskDokument.get()),
            ).thenReturn(
                from(
                    HttpStatus.OK,
                    ArkivereJournalpostResponse
                        .Builder()
                        .jpIdBidrag(jpIdBidrag)
                        .jpIdJoark(jpIdJoark)
                        .journalpostFerdigstilt(false)
                        .melding(melding)
                        .dokumentInfo(List.of(DokumentInfo(joarkDokumentId)))
                        .journalstatus(journalstatus)
                        .build(),
                ),
            )

        // when
        val arkivereJournalpostResponse = arkiveringService!!.arkivereJournalpost(jpIdBidrag)!!
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions
                    .assertThat(
                        arkivereJournalpostResponse.jpIdBidrag,
                    ).withFailMessage(
                        "Mottok feil jpIdBidrag: forventet <%s>, fikk <%s>",
                        jpIdBidrag,
                        arkivereJournalpostResponse.jpIdBidrag,
                    ).isEqualTo(jpIdBidrag)
            },
            Executable {
                Assertions
                    .assertThat(
                        arkivereJournalpostResponse.jpIdJoark,
                    ).withFailMessage(
                        "Mottok feil jpIdJoark: forventet <%s>, fikk <%s>",
                        jpIdJoark,
                        arkivereJournalpostResponse.jpIdJoark,
                    ).isEqualTo(jpIdJoark)
            },
            Executable {
                Assertions
                    .assertThat(
                        arkivereJournalpostResponse.journalstatus,
                    ).withFailMessage(
                        "Mottok feil journalstatus: forventet <%s>, fikk <%s>",
                        journalstatus,
                        arkivereJournalpostResponse.jpIdJoark,
                    ).isEqualTo(journalstatus)
            },
            Executable {
                Assertions
                    .assertThat(
                        arkivereJournalpostResponse.melding,
                    ).withFailMessage(
                        "Mottok melding: forventet <%s>, fikk <%s>",
                        melding,
                        arkivereJournalpostResponse.melding,
                    ).isEqualTo(melding)
            },
            Executable {
                Assertions
                    .assertThat(
                        arkivereJournalpostResponse.journalpostFerdigstilt,
                    ).withFailMessage(
                        "Mottok feil journalpostFerdigstilt: forventet <%s>, fikk <%s>",
                        false,
                        arkivereJournalpostResponse.journalpostFerdigstilt,
                    ).isEqualTo(false)
            },
            Executable {
                Assertions
                    .assertThat(
                        arkivereJournalpostResponse.journalpostFerdigstilt,
                    ).withFailMessage(
                        "Mottok feil jpIdJoark: forventet <%s>, fikk <%s>",
                        false,
                        arkivereJournalpostResponse.journalpostFerdigstilt,
                    ).isEqualTo(false)
            },
            Executable {
                Assertions
                    .assertThat(
                        arkivereJournalpostResponse.dokumentInfo!!.size,
                    ).withFailMessage(
                        "Mottok feil antall dokumenter: forventet 1, fikk <%s>",
                        arkivereJournalpostResponse.dokumentInfo!!.size,
                    ).isEqualTo(1)
            },
            Executable {
                Assertions
                    .assertThat(
                        arkivereJournalpostResponse.dokumentInfo!![0].dokumentInfoId,
                    ).withFailMessage(
                        "Mottok feil dokumentInfoId: forventet <%s>, fikk <%s>",
                        joarkDokumentId,
                        arkivereJournalpostResponse.dokumentInfo!![0].dokumentInfoId,
                    ).isEqualTo(joarkDokumentId)
            },
        )
    }

    @Test
    @DisplayName("Skal ikke arkivere journalpost dersom journalstatus ikke er reservert")
    fun skalIkkeArkivereJournalpostHvisJournalstatusIkkeErReservert() {
        val jpIdBidrag = "18902067"
        val journalpostResponse =
            mockJournalpostResponse(
                journalstatus = JournalpostStatus.MOTTAKSREGISTRERT,
            )
        Mockito
            .`when`(
                bidragDokumentConsumer!!.hentBidragJournalpost(jpIdBidrag),
            ).thenReturn(from(HttpStatus.OK, journalpostResponse))
        org.junit.jupiter.api.Assertions.assertThrows(
            JournalpostHarIkkeGyldigStatusException::class.java,
        ) { arkiveringService!!.arkivereJournalpost(jpIdBidrag) }
    }

    @Test
    @DisplayName("Skal ikke arkivere journalpost dersom journalpost har flere enn en sak")
    fun skalIkkeArkivereJournalpostHvisJournalpostHarFlereEnnEnSak() {
        val jpIdBidrag = "18902067"
        val journalpostResponse =
            mockJournalpostResponse(tilknyttedeSaker = listOf("213123", "3213123"))
        Mockito
            .`when`(
                bidragDokumentConsumer!!.hentBidragJournalpost(jpIdBidrag),
            ).thenReturn(from(HttpStatus.OK, journalpostResponse))
        Mockito
            .`when`(bidragDokumentConsumer.kanArkivereJournalpost(jpIdBidrag))
            .thenReturn(ArkiverDecision(true, null))
        org.junit.jupiter.api.Assertions.assertThrows(
            JournalpostHarFlereEnnEnSakException::class.java,
        ) { arkiveringService!!.arkivereJournalpost(jpIdBidrag) }
    }

    @Test
    @DisplayName("Skal ikke arkivere manglende journalpost")
    fun skalIkkeArkivereManglendeJournalpost() {
        val jpIdInn = "1"
        Mockito
            .`when`(
                bidragDokumentConsumer!!.hentBidragJournalpost(jpIdInn),
            ).thenReturn(from(HttpStatus.NOT_FOUND))
        Mockito
            .`when`(bidragDokumentConsumer.kanArkivereJournalpost(jpIdInn))
            .thenReturn(ArkiverDecision(true, null))
        org.junit.jupiter.api.Assertions.assertThrows(
            JournalpostIkkeFunnetException::class.java,
        ) { arkiveringService!!.arkivereJournalpost(jpIdInn) }
    }

    @Test
    @DisplayName("Skal ikke lagre journalpost dersom henting av fysisk dokument feiler")
    @Throws(
        IOException::class,
    )
    fun skalIkkeLagreJournalpostDersomHentingAvFysiskDokumentFeiler() {
        val jpIdBidrag = "18902067"
        val saksbehandler = "X123456"
        val journalpostResponse = mockJournalpostResponse()
        Mockito
            .`when`(
                bidragDokumentConsumer!!.hentDokument(
                    journalpostResponse.journalpost!!.journalpostId!!,
                ),
            ).thenThrow(HentingAvDokumentFeiletException(""))
        Mockito
            .`when`(
                bidragDokumentConsumer.hentBidragJournalpost(jpIdBidrag),
            ).thenReturn(from(HttpStatus.OK, journalpostResponse))
        Mockito
            .`when`(bidragDokumentConsumer.kanArkivereJournalpost(jpIdBidrag))
            .thenReturn(ArkiverDecision(true, null))
        Mockito
            .`when`(saksbehandlerOidcTokenManager!!.hentSaksbehandler())
            .thenReturn(saksbehandler)
        org.junit.jupiter.api.Assertions.assertThrows(
            HentingAvDokumentFeiletException::class.java,
        ) { arkiveringService!!.arkivereJournalpost(jpIdBidrag) }
    }
}
