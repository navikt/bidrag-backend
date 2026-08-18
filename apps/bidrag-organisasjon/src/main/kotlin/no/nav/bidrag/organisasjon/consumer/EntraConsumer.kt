package no.nav.bidrag.organisasjon.consumer

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.organisasjon.CacheConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

// Typer fra https://entraproxy.intern.dev.nav.no/swagger-ui/index.html#/
data class EntraAnsatt(
    val navIdent: String,
    val visningNavn: String,
    val fornavn: String,
    val etternavn: String,
)

data class EntraEnhet(
    val enhetnummer: String,
    val navn: String,
)

data class EntraTilgang(
    val rolle: String,
)

@Component
class EntraConsumer(
    @Value($$"${ENTRA_PROXY_URL}")
    private val entraProxyUrl: URI,
    @Qualifier("azure")
    private val restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "tilgangskontroll") {
    private val entraBaseUri get() = UriComponentsBuilder.fromUri(entraProxyUrl).pathSegment("api", "v1")

    @Cacheable(CacheConfig.ENTRA_PERSON_ENHETER)
    fun hentPersonEnheter(saksbehandlerIdent: String): List<EntraEnhet> = getForNonNullEntity(entraBaseUri.pathSegment("enhet").pathSegment("ansatt").pathSegment(saksbehandlerIdent).build().toUri())

    @Cacheable(CacheConfig.ENTRA_BRUKERE_FOR_ENHET)
    fun hentBrukereForEnhet(enhet: String): List<EntraAnsatt> = getForNonNullEntity(entraBaseUri.pathSegment("enhet").pathSegment(enhet).build().toUri())

    @Cacheable(CacheConfig.ENTRA_PERSON_INFORMASJON)
    fun hentPersonInformasjon(navident: String): EntraAnsatt? = try {
        getForEntity(entraBaseUri.pathSegment("ansatt").pathSegment(navident).build().toUri())
    } catch (e: Exception) {
        secureLogger.warn(e) { "Det skjedde en feil ved henting av saksbehandlerinfo for $navident" }
        null
    }

    @Cacheable(CacheConfig.ENTRA_TEMA_SAKSBEHANDLERE)
    fun hentSaksbehandlereSOmHarTilgangTilTema(tema: String): List<EntraAnsatt> = getForNonNullEntity<List<EntraAnsatt>>(entraBaseUri.pathSegment("tema").pathSegment(tema).build().toUri())
}
