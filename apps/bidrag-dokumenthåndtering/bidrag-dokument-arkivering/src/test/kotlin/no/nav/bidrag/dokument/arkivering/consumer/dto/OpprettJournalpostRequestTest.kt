package no.nav.bidrag.dokument.arkivering.consumer.dto

import no.nav.bidrag.dokument.arkivering.BidragDokumentArkivering
import no.nav.bidrag.dokument.arkivering.BidragDokumentArkiveringLocal
import no.nav.bidrag.dokument.arkivering.config.BidragDokumentArkiveringConfig
import no.nav.bidrag.dokument.arkivering.dto.Behandlingstema
import no.nav.bidrag.dokument.arkivering.dto.BrukerIdType
import no.nav.bidrag.dokument.arkivering.dto.Fagomraade
import no.nav.bidrag.dokument.arkivering.dto.Fagsaksystem
import no.nav.bidrag.dokument.arkivering.dto.JournalpostType
import no.nav.bidrag.dokument.arkivering.dto.OpprettJournalpostRequest
import no.nav.bidrag.dokument.arkivering.dto.Sakstype
import no.nav.bidrag.dokument.arkivering.dto.Tema
import no.nav.bidrag.dokument.arkivering.testutil.TestdataUtil.mockJournalpostResponse
import no.nav.bidrag.transport.dokument.KodeDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.ActiveProfiles

@DisplayName("OpprettJournalpostRequest")
@ExtendWith(MockitoExtension::class)
@ActiveProfiles(BidragDokumentArkivering.PROFILE_TEST)
@SpringBootTest(
    classes = [BidragDokumentArkiveringLocal::class],
    webEnvironment = WebEnvironment.RANDOM_PORT,
)
@EnableMockOAuth2Server
class OpprettJournalpostRequestTest {
    @DisplayName(
        "Skal mappe fra Bidrag- til Joark-format uten feil dersom alle påkrevde felt har lovlige verdier",
    )
    @Test
    fun skaMappeFraBidragTilJoarkUtenFeilHvisAllePaakrevdeFeltHarLovligeVerdier() {
        val jpResponse = mockJournalpostResponse()
        val jp = jpResponse.journalpost
        val fysiskDokument = "Søknad om penger".toByteArray()
        val request = OpprettJournalpostRequest(jpResponse, fysiskDokument)
        val (_, _, tittel, brevkode, dokumentvarianter) = request.dokumenter[0]
        val (filtype, variantformat, fysiskDokument1, filnavn) = dokumentvarianter!![0]
        val journalpostId = "31712692"
        Assertions.assertAll(
            Executable { Assertions.assertNotNull(request) },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        request.journalpostType!!.name,
                    ).withFailMessage(
                        "Feil journalposttype, forventet <%s>, men fikk <%s>",
                        JournalpostType.UTGAAENDE.name,
                        request.journalpostType!!.name,
                    ).isEqualTo(JournalpostType.UTGAAENDE.name)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        request.avsenderMottaker!!.id,
                    ).withFailMessage(
                        "Feil avsender-/ mottaker-id, forventet <%s>, men fikk <%s>",
                        jp!!.gjelderAktor!!.ident,
                        request.avsenderMottaker!!.id,
                    ).isEqualTo(jp.gjelderAktor!!.ident)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        request.avsenderMottaker!!.idType,
                    ).withFailMessage(
                        "Feil avsender-/ mottaker-idtype, forventet <%s>, men fikk <%s>",
                        "FNR",
                        request.avsenderMottaker!!.idType,
                    ).isEqualTo("FNR")
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        request.avsenderMottaker!!.navn,
                    ).withFailMessage(
                        "Feil avsender-/ mottakernavn, forventet <%s>, men fikk <%s>",
                        null,
                        request.avsenderMottaker!!.navn,
                    ).isEqualTo(null)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        request.bruker!!.idType,
                    ).withFailMessage(
                        "Feil brukeridtype, forventet <%s>, men fikk <%s>",
                        BrukerIdType.FNR,
                        request.bruker!!.idType,
                    ).isEqualTo(BrukerIdType.FNR)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        request.bruker!!.id,
                    ).withFailMessage(
                        "Feil brukerid, forventet <%s>, men fikk <%s>",
                        jp!!.gjelderAktor!!.ident,
                        request.bruker!!.id,
                    ).isEqualTo(jp.gjelderAktor!!.ident)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(request.tema)
                    .withFailMessage(
                        "Feil tema, forventet <%s>, men fikk <%s>",
                        Tema.BID,
                        request.tema,
                    ).isEqualTo(Tema.BID)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(request.behandlingstema)
                    .withFailMessage(
                        "Feil behandlingstema, forventet <%s>, men fikk <%s>",
                        Behandlingstema.BIDRAG_EKSKLUSIV_FARSKAP.kode,
                        request.behandlingstema,
                    ).isEqualTo(Behandlingstema.BIDRAG_EKSKLUSIV_FARSKAP.kode)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(request.tittel)
                    .withFailMessage(
                        "Feil tittel, forventet <%s>, men fikk <%s>",
                        jp!!.innhold,
                        request.tittel,
                    ).isEqualTo(jp.innhold)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(request.journalfoerendeEnhet)
                    .withFailMessage(
                        "Feil journalførende enhet, forventet <%s>, men fikk <%s>",
                        jp!!.journalforendeEnhet,
                        request.journalfoerendeEnhet,
                    ).isEqualTo(jp.journalforendeEnhet)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(request.eksternReferanseId)
                    .withFailMessage(
                        "Feil ekstern referanseid, forventet <%s>, men fikk <%s>",
                        "BID_$journalpostId",
                        request.eksternReferanseId,
                    ).isEqualTo("BID_$journalpostId")
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        request.sak!!.sakstype,
                    ).withFailMessage(
                        "Feil sakstype, forventet <%s>, men fikk <%s>",
                        Sakstype.FAGSAK,
                        request.sak!!.sakstype,
                    ).isEqualTo(Sakstype.FAGSAK)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        request.sak!!.fagsakId,
                    ).withFailMessage(
                        "Feil fagsakid, forventet <%s>, men fikk <%s>",
                        jpResponse.sakstilknytninger[0],
                        request.sak!!.fagsakId,
                    ).isEqualTo(jpResponse.sakstilknytninger[0])
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        request.sak!!.fagsaksystem,
                    ).withFailMessage(
                        "Feil fagsaksystem, forventet <%s>, men fikk <%s>",
                        Fagsaksystem.BISYS,
                        request.sak!!.fagsaksystem,
                    ).isEqualTo(Fagsaksystem.BISYS)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(request.dokumenter.size)
                    .withFailMessage(
                        "Feil antall dokumenter, forventet 1, men fikk <%s>",
                        request.dokumenter.size,
                    ).isEqualTo(1)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(tittel)
                    .withFailMessage(
                        "Feil tittel, forventet <%s>, men fikk <%s>",
                        jp!!.innhold,
                        tittel,
                    ).isEqualTo(jp.innhold)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        brevkode,
                    ).withFailMessage(
                        "Feil brevkode, forventet <%s>, men fikk <%s>",
                        jp!!.brevkode!!.kode,
                        brevkode,
                    ).isEqualTo(jp.brevkode!!.kode)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        filtype,
                    ).withFailMessage(
                        "Feil filtype, forventet <%s>, men fikk <%s>",
                        BidragDokumentArkiveringConfig.DOKUMENT_FILTYPE_PDFA,
                        filtype,
                    ).isEqualTo(BidragDokumentArkiveringConfig.DOKUMENT_FILTYPE_PDFA)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        variantformat,
                    ).withFailMessage(
                        "Feil variantformat, forventet <%s>, men fikk <%s>",
                        BidragDokumentArkiveringConfig.DOKUMENT_VARIANT_FORMAT_ARKIV,
                        variantformat,
                    ).isEqualTo(BidragDokumentArkiveringConfig.DOKUMENT_VARIANT_FORMAT_ARKIV)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        fysiskDokument1,
                    ).withFailMessage(
                        "Feil fysisk dokument, forventet <%s>, men fikk <%s>",
                        fysiskDokument.toString(),
                        fysiskDokument1.toString(),
                    ).isEqualTo(fysiskDokument)
            },
            Executable {
                org.assertj.core.api.Assertions
                    .assertThat(
                        filnavn,
                    ).withFailMessage(
                        "Feil filnavn, forventet <%s>, men fikk <%s>",
                        jp!!.journalpostId!!.replace("-", "_") + ".pdf",
                        filnavn,
                    ).isEqualTo(jp.journalpostId!!.replace("-", "_") + ".pdf")
            },
        )
    }

    @DisplayName(
        "Skal mappe til behandlingstema bidrag INKLUSIV farskap hvis innhold er null og fagområde er FARSKAP",
    )
    @Test
    fun skaMappeTilBehandlingtemaBidragInklusivFarskapHvisInnholdErNullOgFagomraadeErFar() {
        val jpResponse = mockJournalpostResponse(tittel = null, fagomraade = Fagomraade.FARSKAP)
        val fysiskDokument = "Søknad om penger".toByteArray()
        val (_, _, _, _, _, behandlingstema) = OpprettJournalpostRequest(jpResponse, fysiskDokument)
        org.assertj.core.api.Assertions
            .assertThat(behandlingstema)
            .isEqualTo(Behandlingstema.BIDRAG_INKLUSIV_FARSKAP.kode)
    }

    @DisplayName(
        "Skal mappe til behandlingstema bidrag EKSKLUSIV farskap hvis innhold er null og fagområde er BIDRAG",
    )
    @Test
    fun skaMappeTilBehandlingtemaBidragEksklusivFarskapHvisInnholdErNullOgFagomraadeErBid() {
        val jpResponse = mockJournalpostResponse(tittel = null, fagomraade = Fagomraade.BIDRAG)
        val fysiskDokument = "Søknad om penger".toByteArray()
        val (_, _, _, _, _, behandlingstema) = OpprettJournalpostRequest(jpResponse, fysiskDokument)
        org.assertj.core.api.Assertions
            .assertThat(behandlingstema)
            .isEqualTo(Behandlingstema.BIDRAG_EKSKLUSIV_FARSKAP.kode)
    }

    @DisplayName("Skal mappe til behandlingstema til OPPFOSTRINGSBIDRAG")
    @Test
    fun skalMappeTilBehandlingstemaOppfostringsbidrag() {
        val jpResponse =
            mockJournalpostResponse(
                tittel = null,
                fagomraade = Fagomraade.BIDRAG,
                brevkode = KodeDto("BI01S48", null, true),
            )
        val fysiskDokument = "Søknad om penger".toByteArray()
        val (_, _, _, _, _, behandlingstema) = OpprettJournalpostRequest(jpResponse, fysiskDokument)
        org.assertj.core.api.Assertions
            .assertThat(behandlingstema)
            .isEqualTo(Behandlingstema.OPPFOSTRINGSBIDRAG.kode)
    }
}
