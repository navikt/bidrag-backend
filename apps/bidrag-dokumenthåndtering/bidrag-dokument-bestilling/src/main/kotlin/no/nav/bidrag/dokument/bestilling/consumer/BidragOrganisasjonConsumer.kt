package no.nav.bidrag.dokument.bestilling.consumer

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.dokument.bestilling.config.CacheConfig.Companion.ENHETINFO_CACHE
import no.nav.bidrag.dokument.bestilling.config.CacheConfig.Companion.ENHETKONTAKTINFO_CACHE
import no.nav.bidrag.dokument.bestilling.config.CacheConfig.Companion.SAKSBEHANDLERINFO_CACHE
import no.nav.bidrag.dokument.bestilling.consumer.dto.EnhetInfo
import no.nav.bidrag.dokument.bestilling.consumer.dto.EnhetKontaktInfoDto
import no.nav.bidrag.dokument.bestilling.consumer.dto.SaksbehandlerInfoResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BidragOrganisasjonConsumer(
    @Value($$"${BIDRAG_ORGANISASJON_URL}") val url: URI,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-organisasjon") {
    private fun createUri(path: String?) = UriComponentsBuilder
        .fromUri(url)
        .path(path ?: "")
        .build()
        .toUri()

    @Cacheable(SAKSBEHANDLERINFO_CACHE)
    fun hentSaksbehandlerInfo(saksbehandlerIdent: String): SaksbehandlerInfoResponse? = getForEntity(createUri("/saksbehandler/info/$saksbehandlerIdent"))

    @Cacheable(ENHETINFO_CACHE)
    fun hentEnhetInfo(enhetId: String): EnhetInfo? = getForEntity(createUri("/enhet/info/$enhetId"))

    @Cacheable(ENHETKONTAKTINFO_CACHE)
    fun hentEnhetKontaktinfo(
        enhetId: String,
        spraak: String,
    ): EnhetKontaktInfoDto? = getForEntity(createUri("/enhet/kontaktinfo/$enhetId/$spraak"))
}
