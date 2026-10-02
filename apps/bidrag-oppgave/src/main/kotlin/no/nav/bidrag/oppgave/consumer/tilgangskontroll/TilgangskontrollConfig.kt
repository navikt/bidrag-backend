package no.nav.bidrag.oppgave.consumer.tilgangskontroll

import no.nav.bidrag.mdc.CallIdClientRequestInterceptor
import no.nav.bidrag.texas.NaisTokenClientRequestInterceptor
import no.nav.bidrag.texas.NaisTokenService
import no.nav.bidrag.tilgang.TilgangskontrollClient
import no.nav.bidrag.tilgang.TilgangskontrollService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(TilgangProperties::class)
class TilgangskontrollConfig {
    private val log = LoggerFactory.getLogger(TilgangskontrollConfig::class.java)

    @Bean
    internal fun tilgangskontrollService(properties: TilgangProperties, tokenService: NaisTokenService): TilgangskontrollService {
        val restClient = RestClient.builder()
            .baseUrl(properties.url)
            .requestInterceptor(CallIdClientRequestInterceptor())
            .requestInterceptor(NaisTokenClientRequestInterceptor(tokenService, properties.audience))
            .build()
        log.info("Bruker tilgangskontoll fra ${properties.url}")
        val tilgangskontrollClient = TilgangskontrollClient(restClient)

        return TilgangskontrollService(
            tilgangskontrollClient = tilgangskontrollClient,
            navIdentSupplier = { SecurityContextHolder.getContext().authentication?.name ?: throw IllegalStateException("Ingen innlogget bruker") },
        )
    }
}

@ConfigurationProperties(prefix = "app.tilgangskontroll")
internal data class TilgangProperties(
    val url: String,
    /** api://<cluster>.<namespace>.<other-api-app-name>/.default The intended audience (target API or recipient) of the new token. */
    val audience: String,
)
