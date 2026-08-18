package no.nav.bidrag.organisasjon

import com.nimbusds.jose.JOSEObjectType
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders

@Configuration
@Profile(WEB_ENV_TEST)
class TestRestTemplateConfiguration {
    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    fun <T : Any> initHttpEntity(body: T? = null): HttpEntity<T> {
        val headers = HttpHeaders()
        headers.add(EnhetFilter.X_ENHET_HEADER, "1001")
        headers.add(CorrelationId.CORRELATION_ID_HEADER, "Correlateion_xxx_bidrag_sak")
        headers.setBearerAuth(generateBearerToken())
        return HttpEntity(body, headers)
    }

    private fun generateBearerToken(): String {
        val iss = mockOAuth2Server.issuerUrl("aad")
        val newIssuer = iss.newBuilder().host("localhost").build()
        val token =
            mockOAuth2Server.issueToken(
                "aad",
                "aud-localhost",
                DefaultOAuth2TokenCallback(
                    "aad",
                    "aud-localhost",
                    JOSEObjectType.JWT.type,
                    listOf("aud-localhost"),
                    mapOf("iss" to newIssuer.toString()),
                    3600,
                ),
            )
        return token.serialize()
    }
}
