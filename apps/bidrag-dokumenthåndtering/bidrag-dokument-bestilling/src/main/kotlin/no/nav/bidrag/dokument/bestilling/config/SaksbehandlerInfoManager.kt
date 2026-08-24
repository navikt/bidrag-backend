package no.nav.bidrag.dokument.bestilling.config

import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.dokument.bestilling.consumer.BidragOrganisasjonConsumer
import no.nav.bidrag.dokument.bestilling.model.Saksbehandler
import org.springframework.stereotype.Service

@Service
class SaksbehandlerInfoManager(
    private val bidragOrganisasjonConsumer: BidragOrganisasjonConsumer,
) {
    fun hentSaksbehandlerBrukerId(): String? =
        try {
            TokenUtils.hentSaksbehandlerIdent()
        } catch (e: Exception) {
            null
        }

    fun hentSaksbehandler(ident: String? = null): Saksbehandler? {
        return try {
            val saksbehandlerIdent = ident ?: hentSaksbehandlerBrukerId() ?: return null
            val saksbehandlerNavn = bidragOrganisasjonConsumer.hentSaksbehandlerInfo(saksbehandlerIdent)?.navn
            Saksbehandler(saksbehandlerIdent, saksbehandlerNavn)
        } catch (e: Exception) {
            null
        }
    }

    fun erSystembruker(): Boolean =
        try {
            TokenUtils.erApplikasjonsbruker()
        } catch (e: Exception) {
            false
        }
}
