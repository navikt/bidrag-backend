package no.nav.bidrag.commons.web.interceptor

import com.nimbusds.oauth2.sdk.GrantType
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.security.service.ClientConfigurationWellknownProperties
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.security.utils.TokenUtsteder
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.net.URI

abstract class AzureTokenClientInterceptor(
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val clientConfigurationWellknownProperties: ClientConfigurationWellknownProperties,
) : ClientHttpRequestInterceptor {
    protected fun genererAccessToken(
        request: HttpRequest,
        oAuth2GrantType: GrantType? = null,
    ): String {
        val clientProperties =
            clientPropertiesFor(
                request.uri,
                clientConfigurationProperties,
                clientConfigurationWellknownProperties,
                oAuth2GrantType,
            )
        return oAuth2AccessTokenService.getAccessToken(clientProperties).access_token!!
    }

    /**
     * Finds client property for grantType:
     *  - Returns first client property, if there is only one
     *  - Returns client property for client_credentials or jwt_bearer
     */
    private fun clientPropertiesFor(
        uri: URI,
        clientConfigurationProperties: ClientConfigurationProperties,
        clientConfigurationWellknownProperties: ClientConfigurationWellknownProperties,
        oAuth2GrantType: GrantType? = null,
    ): ClientProperties {
        val clientProperties = filterClientProperties(clientConfigurationProperties, uri)
        val clientWellknownProperties = filterClientPropertiesWellKnown(clientConfigurationWellknownProperties, uri)

        if (TokenUtils.erTokenUtstedtAv(TokenUtsteder.TOKENX)) {
            return ClientProperties(
                clientWellknownProperties.tokenXUrl,
                clientWellknownProperties.wellKnownUrl,
                oAuth2GrantType ?: clientCredentialOrJwtBearer(),
                clientProperties.scope,
                clientWellknownProperties.authenticationTokenX!!,
                clientProperties.resourceUrl,
                ClientProperties.TokenExchangeProperties(
                    clientProperties.scope
                        .first()
                        .replace("api://", "")
                        .replace("/.default", "")
                        .replace(".", ":"),
                    "",
                ),
            )
        }
        return ClientProperties(
            clientProperties.tokenEndpointUrl,
            clientWellknownProperties.wellKnownUrl,
            oAuth2GrantType ?: clientCredentialOrJwtBearer(),
            clientProperties.scope,
            clientProperties.authentication,
            clientProperties.resourceUrl,
            null,
        )
    }

    private fun filterClientPropertiesWellKnown(
        clientConfigurationProperties: ClientConfigurationWellknownProperties,
        uri: URI,
    ) = clientConfigurationProperties
        .registration
        .values
        .firstOrNull { uri.toString().startsWith(it.resourceUrl.toString()) }
        ?: error("could not find oauth2 client config for uri=$uri")

    private fun filterClientProperties(
        clientConfigurationProperties: ClientConfigurationProperties,
        uri: URI,
    ) = clientConfigurationProperties
        .registration
        .values
        .firstOrNull { uri.toString().startsWith(it.resourceUrl.toString()) }
        ?: error("could not find oauth2 client config for uri=$uri")

    private fun clientCredentialOrJwtBearer() =
        if (erSystembruker()) {
            GrantType.CLIENT_CREDENTIALS
        } else if (TokenUtils.erTokenUtstedtAv(TokenUtsteder.TOKENX)) {
            GrantType.TOKEN_EXCHANGE
        } else {
            GrantType.JWT_BEARER
        }

    private fun erSystembruker(): Boolean {
        return try {
            if (SikkerhetsKontekst.erIApplikasjonKontekst()) return true
            if (SpringTokenValidationContextHolder()
                    .getTokenValidationContext()
                    .hasTokenFor("tokenx")
            ) {
                return false
            }
            val preferredUsername =
                SpringTokenValidationContextHolder()
                    .getTokenValidationContext()
                    .getClaims("aad")
                    .getStringClaim("preferred_username")
            return preferredUsername == null
        } catch (e: Throwable) {
            // Ingen request context. Skjer ved kall som har opphav i kjørende applikasjon. Ping etc.
            true
        }
    }
}

@Component
class BearerTokenClientInterceptor(
    oAuth2AccessTokenService: OAuth2AccessTokenService,
    clientConfigurationProperties: ClientConfigurationProperties,
    clientConfigurationWellknownProperties: ClientConfigurationWellknownProperties,
) : AzureTokenClientInterceptor(oAuth2AccessTokenService, clientConfigurationProperties, clientConfigurationWellknownProperties) {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        request.headers.setBearerAuth(genererAccessToken(request))
        return execution.execute(request, body)
    }
}

@Component
class ServiceUserAuthTokenInterceptor(
    oAuth2AccessTokenService: OAuth2AccessTokenService,
    clientConfigurationProperties: ClientConfigurationProperties,
    clientConfigurationWellknownProperties: ClientConfigurationWellknownProperties,
) : AzureTokenClientInterceptor(oAuth2AccessTokenService, clientConfigurationProperties, clientConfigurationWellknownProperties) {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        request.headers.setBearerAuth(genererAccessToken(request, GrantType.CLIENT_CREDENTIALS))
        return execution.execute(request, body)
    }
}
