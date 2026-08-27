package no.nav.bidrag.commons.security.service

import com.nimbusds.oauth2.sdk.GrantType
import no.nav.bidrag.commons.security.model.TokenException
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.slf4j.LoggerFactory

open class TokenXTokenService(
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val clientConfigurationWellknownProperties: ClientConfigurationWellknownProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
) : TokenService("Azure") {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun isEnabled() = true

    override fun fetchToken(clientRegistrationId: String): String = getAccessToken(clientRegistrationId).access_token!!

    private fun getAccessToken(clientRegistrationId: String): OAuth2AccessTokenResponse {
        logger.debug("TokenX: Creating token for clientRegistrationId $clientRegistrationId")
        return oAuth2AccessTokenService.getAccessToken(createClientPropertiesWithGrantType(clientRegistrationId))
    }

    private fun createClientPropertiesWithGrantType(clientRegistrationId: String): ClientProperties {
        val registration =
            clientConfigurationProperties.registration["${clientRegistrationId}_tokenx"]
                ?: throw TokenException("Missing registration for client $clientRegistrationId")
        val tokenExchange =
            ClientProperties.TokenExchangeProperties(
                registration.tokenExchange!!.audience.replace(".", ":"),
                "",
            )
        val registrationWellknown = clientConfigurationWellknownProperties.registration["${clientRegistrationId}_tokenx"]

        return ClientProperties(
            registration.tokenEndpointUrl,
            registrationWellknown?.wellKnownUrl,
            GrantType.TOKEN_EXCHANGE,
            registration.scope,
            registration.authentication,
            registration.resourceUrl,
            tokenExchange,
        )
    }
}
