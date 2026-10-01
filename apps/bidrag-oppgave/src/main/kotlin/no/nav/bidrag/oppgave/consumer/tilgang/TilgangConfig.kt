package no.nav.bidrag.oppgave.consumer.tilgang

import no.nav.bidrag.mdc.CallIdClientRequestInterceptor
import no.nav.bidrag.oppgave.consumer.oppgaveapi.OppgaveClient
import no.nav.bidrag.texas.NaisTokenClientRequestInterceptor
import no.nav.bidrag.texas.NaisTokenService
import no.nav.bidrag.tilgang.TilgangClient
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(TilgangProperties::class)
class TilgangConfig {

    @Bean
    internal fun tilgangClient(properties: TilgangProperties, tokenService: NaisTokenService): TilgangClient {
        val restClient = RestClient.builder()
            .baseUrl(properties.url)
            .requestInterceptor(CallIdClientRequestInterceptor())
            .requestInterceptor(NaisTokenClientRequestInterceptor(tokenService, properties.audience))
            .build()

        return TilgangClient(restClient)
    }
}

@ConfigurationProperties(prefix = "app.tilgangskontroll")
internal data class TilgangProperties(
    val url: String,
    /** api://<cluster>.<namespace>.<other-api-app-name>/.default The intended audience (target API or recipient) of the new token. */
    val audience: String,
)
