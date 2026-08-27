package no.nav.bidrag.commons.interceptor

import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import no.nav.bidrag.commons.security.service.ClientConfigurationWellknownProperties
import no.nav.bidrag.commons.security.service.ClientPropertiesWellknown
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import java.net.URI

private const val TOKEN_ENDPOINT = "http://tokenendpoint.com"
private val authentication =
    ClientAuthenticationProperties(
        "clientIdent",
        ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
        "Secrets are us",
        null,
    )

val clientConfigurationWellknownProperties =
    ClientConfigurationWellknownProperties(
        mapOf(
            "1" to
                ClientPropertiesWellknown(
                    URI("http://firstResource.no"),
                    URI(TOKEN_ENDPOINT),
                ),
            "2" to
                ClientPropertiesWellknown(
                    URI("http://jwtResource.no"),
                    URI(TOKEN_ENDPOINT),
                ),
            "3" to
                ClientPropertiesWellknown(
                    URI("http://clientResource.no"),
                    URI(TOKEN_ENDPOINT),
                ),
        ),
    )

val clientConfigurationProperties =
    ClientConfigurationProperties(
        mapOf(
            "1" to
                ClientProperties(
                    URI(TOKEN_ENDPOINT),
                    URI(TOKEN_ENDPOINT),
                    GrantType.CLIENT_CREDENTIALS,
                    listOf("z", "y", "x"),
                    authentication,
                    URI("http://firstResource.no"),
                    null,
                ),
            "2" to
                ClientProperties(
                    URI(TOKEN_ENDPOINT),
                    URI(TOKEN_ENDPOINT),
                    GrantType.JWT_BEARER,
                    listOf("z", "y", "x"),
                    authentication,
                    URI("http://jwtResource.no"),
                    null,
                ),
            "3" to
                ClientProperties(
                    URI(TOKEN_ENDPOINT),
                    URI(TOKEN_ENDPOINT),
                    GrantType.CLIENT_CREDENTIALS,
                    listOf("z", "y", "x"),
                    authentication,
                    URI("http://clientResource.no"),
                    null,
                ),
        ),
    )
