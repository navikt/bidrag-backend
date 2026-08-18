package no.nav.bidrag.oppgave.config

import com.nimbusds.jose.jwk.RSAKey
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.JwtBearerOAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.JwtBearerGrantRequest
import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter
import org.springframework.security.oauth2.client.endpoint.RestClientJwtBearerTokenResponseClient
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

@Configuration
class OboClientConfig(
    @Value("\${AZURE_APP_JWK}") azureAppJwk: String,
) {
    private val rsaKey: RSAKey = RSAKey.parse(azureAppJwk)

    @Bean
    fun authorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService,
    ): OAuth2AuthorizedClientManager {
        val nimbusConverter =
            NimbusJwtClientAuthenticationParametersConverter<JwtBearerGrantRequest> { rsaKey }
        val clientAuthConverter =
            Converter<JwtBearerGrantRequest, MultiValueMap<String, String>> { request ->
                nimbusConverter.convert(request)!!
            }

        val onBehalfOfConverter =
            Converter<JwtBearerGrantRequest, MultiValueMap<String, String>> {
                LinkedMultiValueMap<String, String>().apply {
                    add("requested_token_use", "on_behalf_of")
                }
            }

        val tokenResponseClient =
            RestClientJwtBearerTokenResponseClient().apply {
                addParametersConverter(clientAuthConverter)
                addParametersConverter(onBehalfOfConverter)
            }

        val jwtBearerProvider =
            JwtBearerOAuth2AuthorizedClientProvider().apply {
                setAccessTokenResponseClient(tokenResponseClient)
            }

        return AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientService,
        ).apply {
            setAuthorizedClientProvider(jwtBearerProvider)
        }
    }
}
