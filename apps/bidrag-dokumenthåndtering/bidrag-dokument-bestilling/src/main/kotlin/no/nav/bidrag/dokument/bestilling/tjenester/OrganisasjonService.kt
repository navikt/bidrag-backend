package no.nav.bidrag.dokument.bestilling.tjenester

import no.nav.bidrag.dokument.bestilling.consumer.BidragOrganisasjonConsumer
import no.nav.bidrag.dokument.bestilling.consumer.dto.EnhetInfo
import no.nav.bidrag.dokument.bestilling.consumer.dto.EnhetKontaktInfoDto
import org.springframework.stereotype.Service

@Service
class OrganisasjonService(
    private val bidragOrganisasjonConsumer: BidragOrganisasjonConsumer,
) {
    fun hentEnhetInfo(enhetId: String): EnhetInfo? = bidragOrganisasjonConsumer.hentEnhetInfo(enhetId)

    fun hentEnhetKontaktInfo(
        enhetId: String,
        spraak: String?,
    ): EnhetKontaktInfoDto? = bidragOrganisasjonConsumer.hentEnhetKontaktinfo(enhetId, spraak ?: "NB")
}
