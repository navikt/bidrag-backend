package no.nav.bidrag.grunnlag.consumer

import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.grunnlag.ISSUER
import no.nav.bidrag.transport.tilgang.Brukertilganger
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class TilgangskontrollConsumer(
    @param:Qualifier("azure") private val restTemplate: RestTemplate,
    @param:Value("\${BIDRAG_TILGANGSKONTROLL_URL}") url: URI,
) {
    private val brukertilgangerUri = UriComponentsBuilder.fromUri(url)
        .pathSegment("v2", "api", "tilgang", "brukertilganger")
        .build().toUri()

    fun krevAdministrasjonstilgang() {
        val kontekst = SpringTokenValidationContextHolder().getTokenValidationContext()
        if (!kontekst.hasValidToken() || !kontekst.hasTokenFor(ISSUER) || TokenUtils.erApplikasjonsbruker()) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Krever administrasjonstilgang for innlogget bruker")
        }
        val claims = kontekst.getClaims(ISSUER)
        if (claims.getStringClaim("NAVident").isNullOrBlank() ||
            claims.getStringClaim("preferred_username").isNullOrBlank() ||
            claims.getStringClaim("sub").isNullOrBlank() ||
            claims.getStringClaim("oid").isNullOrBlank() ||
            claims.getStringClaim("sub") == claims.getStringClaim("oid")
        ) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Krever administrasjonstilgang for innlogget bruker")
        }

        val tilganger = try {
            restTemplate.postForObject(brukertilgangerUri, null, Brukertilganger::class.java)
        } catch (_: RestClientException) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kunne ikke hente brukertilganger")
        } ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kunne ikke hente brukertilganger")

        if (!tilganger.administrasjonTilgang) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Krever administrasjonstilgang")
        }
    }
}
