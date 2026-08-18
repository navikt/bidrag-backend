package no.nav.bidrag.tilgangskontroll

import com.nimbusds.jose.JOSEObjectType
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class TokenGenerator(
    val mockOAuth2Server: MockOAuth2Server,
    @Value("\${AZURE_APP_CLIENT_ID}") val clientId: String,
) {
    fun opprettBrukerToken(saksbehandlerIdent: String = "Z993399"): String {
        val iss = mockOAuth2Server.issuerUrl("aad")
        val newIssuer = iss.newBuilder().host("localhost").build()
        val token =
            mockOAuth2Server.issueToken(
                "aad",
                clientId,
                DefaultOAuth2TokenCallback(
                    "aad",
                    saksbehandlerIdent,
                    JOSEObjectType.JWT.type,
                    listOf(clientId),
                    mapOf("iss" to newIssuer.toString()),
                    3600,
                ),
            )
        return "Bearer " + token.serialize()
    }

    @Bean
    fun opprettClientCredentialsToken(): String {
        val iss = mockOAuth2Server.issuerUrl("aad")
        val newIssuer = iss.newBuilder().host("localhost").build()
        val token =
            mockOAuth2Server.issueToken(
                "aad",
                clientId,
                DefaultOAuth2TokenCallback(
                    "aad",
                    "bidrag-nais-app",
                    JOSEObjectType.JWT.type,
                    listOf(clientId),
                    mapOf("roles" to listOf("access_as_application"), "iss" to newIssuer.toString()),
                    3600,
                ),
            )
        return "Bearer " + token.serialize()
    }
}
