package no.nav.bidrag.commons.security.service

import com.nimbusds.oauth2.sdk.GrantType
import no.nav.bidrag.commons.security.model.TokenException
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.slf4j.LoggerFactory

open class AzureTokenService(
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val clientConfigurationWellknownProperties: ClientConfigurationWellknownProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
) : TokenService("Azure") {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        const val AZURE_CLAIM_OID = "oid"
        const val AZURE_CLAIM_SUB = "sub"
    }

    override fun isEnabled() = true

    override fun fetchToken(
        clientRegistrationId: String,
        token: JwtToken?,
    ): String = getAccessToken(clientRegistrationId, token).access_token!!

    private fun getAccessToken(
        clientRegistrationId: String,
        token: JwtToken?,
    ): OAuth2AccessTokenResponse {
        if (token != null && isOnBehalfOfFlowToken(token)) {
            logger.debug("AZURE: Creating on-behalf-of token")
            return oAuth2AccessTokenService.getAccessToken(
                createClientPropertiesWithGrantType(clientRegistrationId, GrantType.JWT_BEARER),
            )
        }
        logger.debug("AZURE: Creating client credentials token")
        return oAuth2AccessTokenService.getAccessToken(
            createClientPropertiesWithGrantType(clientRegistrationId, GrantType.CLIENT_CREDENTIALS),
        )
    }

    private fun isOnBehalfOfFlowToken(token: JwtToken): Boolean {
        val jwtTokenClaims: JwtTokenClaims = token.jwtTokenClaims
        return jwtTokenClaims.getStringClaim(AZURE_CLAIM_SUB) != jwtTokenClaims.getStringClaim(AZURE_CLAIM_OID)
    }

    private fun createClientPropertiesWithGrantType(
        clientRegistrationId: String,
        grantType: GrantType?,
    ): ClientProperties {
        val registration =
            clientConfigurationProperties.registration[clientRegistrationId]
                ?: throw TokenException("Missing registration for client $clientRegistrationId")
        val registrationWellknown = clientConfigurationWellknownProperties.registration[clientRegistrationId]
        return ClientProperties(
            registration.tokenEndpointUrl,
            registrationWellknown?.wellKnownUrl,
            grantType ?: registration.grantType,
            registration.scope,
            registration.authentication,
            registration.resourceUrl,
            registration.tokenExchange,
        )
    }
}
