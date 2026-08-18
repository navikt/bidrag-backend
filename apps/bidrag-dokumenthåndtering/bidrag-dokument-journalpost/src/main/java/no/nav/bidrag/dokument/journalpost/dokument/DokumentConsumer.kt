package no.nav.bidrag.dokument.journalpost.dokument

import io.micrometer.core.annotation.Timed
import jakarta.activation.DataHandler
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpost.SECURE_LOGGER
import no.nav.bidrag.dokument.journalpost.dto.Dokumentbestilling
import no.nav.bidrag.dokument.journalpost.exception.DokumentErIkkeRTFException
import no.nav.bidrag.dokument.journalpost.exception.DokumentetErIkkePdfException
import no.nav.bidrag.dokument.journalpost.exception.HentingAvDokumentFeiletException
import no.nav.tjenester.brevogarkiv.dokumentbehandling.DokumentbehandlingPortType
import no.nav.tjenester.brevogarkiv.dokumentbehandling.HentDokumentRequest
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.Optional

@Component
class DokumentConsumer(
    private val port: DokumentbehandlingPortType,
) {
    @Retryable(
        maxAttempts = 3,
        backoff = Backoff(delay = 500, maxDelay = 1000, multiplier = 2.0),
        exclude = [DokumentetErIkkePdfException::class],
    )
    @Timed("hentDokumentRTF")
    fun henteDokumentRTF(dokumentbestilling: Dokumentbestilling): Optional<ByteArray> {
        LOGGER.info("Henter RTF dokument med brevreferanse ${dokumentbestilling.brevreferanse} fra midlertidig brevlager")

        val hentDokumentRequest = HentDokumentRequest()
        hentDokumentRequest.brevreferanse = dokumentbestilling.brevreferanse
        hentDokumentRequest.systemId = dokumentbestilling.systemId
        hentDokumentRequest.token = dokumentbestilling.token

        LOGGER.info("SystemId i hentDokumentRequest: ${hentDokumentRequest.systemId}")
        try {
            val dokumentrespons = port.hentDokument(hentDokumentRequest)
            LOGGER.info(
                "Dokument detaljer: contentType: ${dokumentrespons.dokumentData.contentType}, name: ${dokumentrespons.dokumentData.name}",
            )
            val returnertDokument = dokumentrespons.dokumentData
            verifiserErRTFDokument(returnertDokument)
            return tilByteArray(returnertDokument)
        } catch (e: IOException) {
            throw HentingAvDokumentFeiletException(
                "Henting av dokument for brevreferanse ${dokumentbestilling.brevreferanse} feilet. Feilmelding: ${e.message}",
                e,
            )
        } catch (e: RuntimeException) {
            throw HentingAvDokumentFeiletException(
                "Henting av dokument for brevreferanse ${dokumentbestilling.brevreferanse} feilet. Feilmelding: ${e.message}",
                e,
            )
        }
    }

    @Retryable(
        maxAttempts = 3,
        backoff = Backoff(delay = 500, maxDelay = 1000, multiplier = 2.0),
        exclude = [DokumentetErIkkePdfException::class],
    )
    @Timed("hentDokument")
    fun henteDokument(dokumentbestilling: Dokumentbestilling): Optional<ByteArray> {
        LOGGER.info("Henter dokument med brevreferanse ${dokumentbestilling.brevreferanse} fra midlertidig brevlager")
        LOGGER.info("SystemId for dokumentbestilling: ${dokumentbestilling.systemId}")

        val hentDokumentRequest = HentDokumentRequest()
        hentDokumentRequest.brevreferanse = dokumentbestilling.brevreferanse
        hentDokumentRequest.systemId = dokumentbestilling.systemId
        hentDokumentRequest.token = dokumentbestilling.token

        LOGGER.info("SystemId i hentDokumentRequest: ${hentDokumentRequest.systemId}")
        try {
            val dokumentrespons = port.hentDokument(hentDokumentRequest)
            val returnertDokument = dokumentrespons.dokumentData
            verifiserErPDFDokument(returnertDokument)
            return tilByteArray(returnertDokument)
        } catch (e: IOException) {
            throw HentingAvDokumentFeiletException(
                "Henting av dokument for brevreferanse ${dokumentbestilling.brevreferanse} feilet. Feilmelding: ${e.message}",
                e,
            )
        } catch (e: RuntimeException) {
            throw HentingAvDokumentFeiletException(
                "Henting av dokument for brevreferanse ${dokumentbestilling.brevreferanse} feilet. Feilmelding: ${e.message}",
                e,
            )
        }
    }

    @Retryable(
        maxAttempts = 3,
        backoff = Backoff(delay = 500, maxDelay = 1000, multiplier = 2.0),
        exclude = [DokumentetErIkkePdfException::class],
    )
    @Timed("erFerdigstilt")
    fun erFerdigstilt(dokumentbestilling: Dokumentbestilling): Boolean {
        LOGGER.debug("Henter dokument med brevreferanse ${dokumentbestilling.brevreferanse} fra midlertidig brevlager")
        LOGGER.debug("SystemId for dokumentbestilling: ${dokumentbestilling.systemId}")

        val hentDokumentRequest = HentDokumentRequest()
        hentDokumentRequest.brevreferanse = dokumentbestilling.brevreferanse
        hentDokumentRequest.systemId = dokumentbestilling.systemId
        hentDokumentRequest.token = dokumentbestilling.token
        try {
            val dokumentrespons = port.hentDokument(hentDokumentRequest)
            val dokument = dokumentrespons.dokumentData
            return dokument.contentType.equals("application/pdf", ignoreCase = true)
        } catch (e: RuntimeException) {
            throw HentingAvDokumentFeiletException(
                String.format(
                    "Henting av dokument for brevreferanse %s feilet. Feilmelding: %s",
                    dokumentbestilling.brevreferanse,
                    e.message,
                ),
                e,
            )
        }
    }

    private fun verifiserErRTFDokument(dokument: DataHandler) {
        val erPdf = dokument.contentType.equals("text/rtf", ignoreCase = true)
        if (!erPdf) {
            throw DokumentErIkkeRTFException(
                "Dokumentet er ikke et gyldig RTF dokument. Dokumentet har typen ${dokument.contentType}.",
            )
        }
    }

    private fun verifiserErPDFDokument(dokument: DataHandler) {
        val erPdf = dokument.contentType.equals("application/pdf", ignoreCase = true)
        if (!erPdf) {
            throw DokumentetErIkkePdfException(
                "Dokumentet er ikke et gyldig PDF dokument. Dokumentet har typen ${dokument.contentType}.",
            )
        }
    }

    @Throws(IOException::class)
    private fun tilByteArray(dataHandler: DataHandler): Optional<ByteArray> {
        LOGGER.info("Dokument hentet fra midlertidig brevlager, konverterer til byte array")
        return Optional.of(IOUtils.toByteArray(dataHandler.inputStream))
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(DokumentConsumer::class.java)
    }
}
