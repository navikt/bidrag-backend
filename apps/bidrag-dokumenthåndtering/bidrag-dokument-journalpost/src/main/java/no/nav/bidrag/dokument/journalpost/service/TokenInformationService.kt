package no.nav.bidrag.dokument.journalpost.service

import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager
import no.nav.bidrag.dokument.journalpost.consumer.SaksbehandlerConsumer
import no.nav.bidrag.dokument.journalpost.dto.Saksbehandler
import no.nav.bidrag.dokument.journalpost.model.SYSTEM_SAKSBEHANDLER
import no.nav.bidrag.dokument.journalpost.model.SYSTEM_SAKSBEHANDLER_NAVN
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class TokenInformationService(
    private val saksbehandlerConsumer: SaksbehandlerConsumer,
    private val saksbehandlerOidcTokenManager: SaksbehandlerOidcTokenManager,
) {
    companion object {
        const val SAKSBEHANDLER_NAVN_UKJENT = "Ukjent navn"
    }

    fun hentSaksbehandlersBrukerid(): String? {
        if (saksbehandlerOidcTokenManager.erSystemBruker()) {
            return SYSTEM_SAKSBEHANDLER
        }
        return saksbehandlerOidcTokenManager.hentSaksbehandler()
    }

    fun hentSaksbehandler(): Optional<Saksbehandler> = hentSaksbehandler(null)

    fun hentSaksbehandlersNavn(): String? = hentSaksbehandlersNavn(null)

    fun hentSaksbehandler(saksbehandlerId: String?): Optional<Saksbehandler> = saksbehandlerConsumer.hentSaksbehandler(saksbehandlerId ?: saksbehandlerOidcTokenManager.hentSaksbehandler())

    fun hentSaksbehandlersNavn(saksbehandlerIdent: String?): String? {
        if (saksbehandlerIdent.isNullOrEmpty() && saksbehandlerOidcTokenManager.erSystemBruker()) {
            return SYSTEM_SAKSBEHANDLER_NAVN
        }
        val saksbehandler = hentSaksbehandler(saksbehandlerIdent)
        return if (saksbehandler.isPresent) saksbehandler.get().navn else SAKSBEHANDLER_NAVN_UKJENT
    }
}
