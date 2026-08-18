package no.nav.bidrag.dokument.arkivering.consumer.rest.bidrag

import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.dokument.arkivering.consumer.BidragDokumentConsumer
import no.nav.bidrag.dokument.arkivering.testutil.TestdataUtil.mockJournalpostResponse
import no.nav.bidrag.transport.dokument.JournalpostResponse
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@DisplayName("BidragDokumentConsumer")
@ExtendWith(MockitoExtension::class)
class BidragDokumentConsumerTest {
    @Mock
    private lateinit var restTemplateMock: HttpHeaderRestTemplate

    @Mock
    private lateinit var securityTokenService: SecurityTokenService
    private var bidragDokumentConsumer: BidragDokumentConsumer? = null

    @BeforeEach
    fun setUp() {
        bidragDokumentConsumer = BidragDokumentConsumer(restTemplateMock, "", securityTokenService)
    }

    @Test
    @DisplayName("Skal hente journalpost dersom journalpostid eksisterer")
    fun skalHenteJournalpostDersomJournalpostIdEksisterer() {
        // given
        val journalpostResponse = mockJournalpostResponse()
        val path =
            String.format(
                BidragDokumentConsumer.HENTE_JOURNALPOST_PATH,
                "BID-" + journalpostResponse.journalpost!!.journalpostId,
            )
        Mockito
            .`when`(
                restTemplateMock!!.exchange(
                    path,
                    HttpMethod.GET,
                    null,
                    JournalpostResponse::class.java,
                ),
            ).thenReturn(ResponseEntity(journalpostResponse, HttpStatus.OK))

        // when
        val respons =
            bidragDokumentConsumer!!.hentBidragJournalpost(
                journalpostResponse.journalpost!!.journalpostId!!,
            )
        assert(respons.fetchBody().isPresent)
        val (journalpost) = respons.fetchBody().get()
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(respons.is2xxSuccessful()) },
            Executable {
                Assertions.assertThat(
                    journalpost!!.journalpostId == journalpostResponse.journalpost!!.journalpostId,
                )
            },
            Executable {
                Assertions.assertThat(
                    journalpost!!.dokumenter.size == journalpostResponse.journalpost!!.dokumenter.size,
                )
            },
        )
    }
}
