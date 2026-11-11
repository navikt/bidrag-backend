package no.nav.bidrag.tilgangskontroll.konsumer

import no.nav.bidrag.cache.annotasjoner.KortCache
import no.nav.bidrag.cache.annotasjoner.LangCache
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.tilgangskontroll.konfigurasjon.Cache
import no.nav.bidrag.tilgangskontroll.model.tilgangsmaskin.RegelType
import no.nav.bidrag.tilgangskontroll.model.tilgangsmaskin.TilgangsmaskinBulkResponse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class TilgangsmaskinConsumer(
    @param:Value("\${TILGANGSMASKIN_URL}") val url: URI,
    @param:Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "tilgangsmaskin") {
    private val tilgangsmaskinUrl get() = UriComponentsBuilder.fromUri(url).path("api/v1")

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    @KortCache(Cache.TILGANGSMASKIN_KOMPLETTEREGLER, perBruker = true) // Bruker @KortCache fordi ResponseEntity ikke kan lagres i Valkey.
    fun evaluerKomplettRegelsettForBruker(personIdent: Personident): Any? = postForEntity<ResponseEntity<Any>>(
        tilgangsmaskinUrl.pathSegment("komplett").build().toUri(),
        personIdent.verdi,
    )

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    @KortCache(Cache.TILGANGSMASKIN_KJERNEREGLER, perBruker = true) // Bruker @KortCache fordi ResponseEntity ikke kan lagres i Valkey.
    fun evaluerKjerneRegelsettForBruker(personIdent: Personident): Any? = postForEntity<ResponseEntity<Any>>(
        tilgangsmaskinUrl.pathSegment("kjerne").build().toUri(),
        personIdent.verdi,
    )

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    @LangCache(Cache.TILGANGSMASKIN_KJERNEREGLER_BULK, perBruker = true)
    fun evaluerKjerneRegelsettForFlereBrukere(personIdent: List<String>): TilgangsmaskinBulkResponse {
        val response =
            postForEntity<TilgangsmaskinBulkResponse>(
                tilgangsmaskinUrl
                    .pathSegment("bulk/obo" + RegelType.KJERNE_REGELTYPE.name)
                    .build()
                    .toUri(),
                personIdent,
            )
        return response ?: throw IllegalStateException("Ingen respons fra tilgangsmaskin for bulk evaluering av kjerne regelsett.")
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    @LangCache(Cache.TILGANGSMASKIN_KOMPLETTEREGLER_BULK, perBruker = true)
    fun evaluerKomplettRegelsettForFlereBrukere(personIdent: List<String>): TilgangsmaskinBulkResponse {
        try {
            val response =
                postForEntity<TilgangsmaskinBulkResponse>(
                    tilgangsmaskinUrl
                        .pathSegment("bulk/obo/" + RegelType.KOMPLETT_REGELTYPE.name)
                        .build()
                        .toUri(),
                    personIdent,
                )
            return response
                ?: throw IllegalStateException("Ingen respons fra tilgangsmaskin for bulk evaluering av komplett regelsett.")
        } catch (e: HttpClientErrorException.BadRequest) {
            secureLogger.error(e) {
                "Feil ved kall mot tilgangsmaskin med input $personIdent: " +
                    "${commonObjectmapper.writeValueAsString(personIdent)} -  ${e.responseBodyAsString}"
            }
            throw e
        }
    }
}
