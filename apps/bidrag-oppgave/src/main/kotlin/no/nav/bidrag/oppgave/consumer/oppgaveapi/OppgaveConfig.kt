package no.nav.bidrag.oppgave.consumer.oppgaveapi

import no.nav.bidrag.texas.NaisTokenClientRequestInterceptor
import no.nav.bidrag.texas.NaisTokenService
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(OppgaveProperties::class)
class OppgaveConfig {

    @Bean
    internal fun oppgaveClient(properties: OppgaveProperties, tokenService: NaisTokenService): OppgaveClient {
        val restClient = RestClient.builder()
            .baseUrl(properties.url)
//            .requestInterceptor(CallIdClientRequestInterceptor("X-Correlation-ID"))
            .requestInterceptor(NaisTokenClientRequestInterceptor(tokenService, properties.audience))
            .build()

        return OppgaveClient(restClient)
    }
}

@ConfigurationProperties(prefix = "app.oppgave")
internal data class OppgaveProperties(
    val url: String,
    /** api://<cluster>.<namespace>.<other-api-app-name>/.default The intended audience (target API or recipient) of the new token. */
    val audience: String,
)
