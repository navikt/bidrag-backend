package no.nav.bidrag.oppgave.consumer

import no.nav.bidrag.oppgave.consumer.oppgaveapi.OppgaveClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.client.RestClient
import java.util.UUID

@Configuration
class OppgaveApiClientConfig {

    @Bean
    fun oppgaveClient(
        authorizedClientManager: OAuth2AuthorizedClientManager,
        @Value("\${oppgave-api.base-url}") baseUrl: String,
    ): OppgaveClient {
        val restClient =
            RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor { request, body, execution ->
                    val authentication = SecurityContextHolder.getContext().authentication
                        ?: error("Ingen autentisert bruker – kan ikke gjøre OBO-veksling mot oppgave-api")

                    val authorizeRequest =
                        OAuth2AuthorizeRequest
                            .withClientRegistrationId("oppgave-api-obo")
                            .principal(authentication)
                            .build()

                    val authorizedClient = authorizedClientManager.authorize(authorizeRequest)
                        ?: error("Klarte ikke å hente OBO-token for oppgave-api")

                    request.headers.setBearerAuth(authorizedClient.accessToken.tokenValue)
                    request.headers.set("X-Correlation-ID", UUID.randomUUID().toString())
                    execution.execute(request, body)
                }
                .build()

        return OppgaveClient(restClient)
    }
}
