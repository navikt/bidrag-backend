package no.nav.bidrag.person.consumer

import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.person.dto.KrrPersonKontaktinformasjonRequest
import no.nav.bidrag.person.dto.KrrPersonKontaktinformasjonRespons
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

@Component
class KrrConsumer(@param:Qualifier("krr") private val krrRestTemplate: RestTemplate) {

    /* Denne metoden er med vilje ikke cached.
   Den brukes av bidrag-aktoerregister for å hente kontonummer på personer
   og må alltid returnere siste data. */
    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    fun hentPersonSpraak(personident: Personident): String? {
        val headers = HttpHeaders().apply {
            set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        }

        val respons: ResponseEntity<KrrPersonKontaktinformasjonRespons> =
            krrRestTemplate.exchange(
                KRR_PERSON_ENDEPUNKT,
                HttpMethod.POST,
                HttpEntity(KrrPersonKontaktinformasjonRequest(listOf(personident.verdi)), headers),
            )

        return if (respons.statusCode == HttpStatus.OK) {
            respons.body?.personer?.getOrDefault(personident.verdi, null)?.spraak
        } else {
            null
        }
    }

    companion object {
        const val KRR_PERSON_ENDEPUNKT = "/rest/v1/personer"
    }
}
