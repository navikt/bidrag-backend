package no.nav.bidrag.dokument.arkivering.testutil

import jakarta.activation.DataHandler
import jakarta.activation.FileDataSource
import no.nav.bidrag.dokument.arkivering.dto.DokumentInfo
import no.nav.bidrag.dokument.arkivering.dto.Dokumenttype
import no.nav.bidrag.dokument.arkivering.dto.Fagomraade
import no.nav.bidrag.dokument.arkivering.dto.OpprettJournalpostResponse
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.dokument.AktorDto
import no.nav.bidrag.transport.dokument.DokumentDto
import no.nav.bidrag.transport.dokument.IdentType
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.bidrag.transport.dokument.JournalpostStatus
import no.nav.bidrag.transport.dokument.KodeDto
import org.apache.commons.io.IOUtils
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.Optional

object TestdataUtil {
    const val BIDRAG_JOURNALPOSTSTATUS_MOTTAKSREGISTRERT = "M"

    @JvmStatic
    fun mockJournalpostResponse(
        joarkJournalpostId: String? = null,
        journalstatus: JournalpostStatus = JournalpostStatus.KLAR_FOR_DISTRIBUSJON,
        tittel: String? = "Søknad om mer penger",
        fagomraade: Fagomraade = Fagomraade.BIDRAG,
        brevkode: KodeDto = KodeDto("BI01A08", "Vedtaksbrev", true),
        tilknyttedeSaker: List<String> = listOf("1900006"),
    ): JournalpostResponse {
        val journalpostId = "BID-31712692"
        val dokumentdato = LocalDate.now().minusDays(2)
        val avsendernavn = "Yuko Moto"
        val dokumenter =
            listOf(
                DokumentDto(
                    journalpostId = "3171269236",
                    dokumentType = Dokumenttype.UTGAAENDE.getKode(),
                    dokumentreferanse = tittel,
                ),
            )
        val ident = genererFødselsnummer()
        val enhet = "4802"
        val saksbehandler = "X123456"
        val journalfoertDato = dokumentdato.plusDays(1)
        return JournalpostResponse(
            JournalpostDto(
                avsenderNavn = avsendernavn,
                dokumenter = dokumenter,
                dokumentDato = dokumentdato,
                fagomrade = fagomraade.kode,
                gjelderAktor = AktorDto(ident, IdentType.FNR),
                innhold = tittel,
                journalforendeEnhet = enhet,
                joarkJournalpostId = joarkJournalpostId,
                journalfortAv = saksbehandler,
                journalfortDato = journalfoertDato,
                journalpostId = journalpostId,
                mottattDato = journalfoertDato,
                dokumentType = dokumenter[0].dokumentType,
                journalstatus = journalstatus.kode,
                status = journalstatus,
                brevkode = brevkode,
            ),
            tilknyttedeSaker,
        )
    }

    fun mockOpprettJournalpostResponse(
        dokumentId: String?,
        journalstatus: String = "OD",
        journalpostId: String?,
        melding: String?,
    ): OpprettJournalpostResponse = OpprettJournalpostResponse(
        dokumenter = listOf(DokumentInfo(dokumentId)),
        journalpostId = journalpostId,
        journalstatus = journalstatus,
        journalpostferdigstilt = false,
        melding = melding,
    )

    @JvmStatic
    @Throws(IOException::class)
    fun mockDokumentbehandlingConsumerRetur(): Optional<ByteArray> = Optional.of(IOUtils.toByteArray(mockDataHandler().inputStream))

    @Throws(IOException::class)
    private fun mockDataHandler(): DataHandler {
        val dataSource = FileDataSource(lesSoeknadOmMerPenger())
        return DataHandler(dataSource)
    }

    @Throws(IOException::class)
    private fun lesSoeknadOmMerPenger(): File {
        val resource: Resource = ClassPathResource("testdata/DokumentFraMidlertidigBrevlager.txt")
        val input = resource.inputStream
        return resource.file
    }
}
