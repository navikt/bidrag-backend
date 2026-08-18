package no.nav.bidrag.dokument.security

import com.nimbusds.jose.JOSEObjectType
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.dokument.TEST_PROFILE
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders

@Configuration
@Profile(TEST_PROFILE)
class HttpHeaderTestRestTemplateConfiguration {
    @Autowired
    private val mockOAuth2Server: MockOAuth2Server? = null

    @Bean
    fun securedTestRestTemplate(testRestTemplate: TestRestTemplate): HttpHeaderTestRestTemplate {
        val httpHeaderTestRestTemplate = HttpHeaderTestRestTemplate(testRestTemplate)
        httpHeaderTestRestTemplate.add(HttpHeaders.AUTHORIZATION) { this.generateTestToken() }

        return httpHeaderTestRestTemplate
    }

    private fun generateTestToken(): String {
        val iss = mockOAuth2Server!!.issuerUrl("aad")
        val newIssuer = iss.newBuilder().host("localhost").build()

        //    var token = mockOAuth2Server.issueToken("aad", "aud-localhost", "aud-localhost");
        val token =
            mockOAuth2Server.issueToken(
                "aad",
                "aud-localhost",
                DefaultOAuth2TokenCallback(
                    "aad",
                    "aud-localhost",
                    JOSEObjectType.JWT.type,
                    mutableListOf("aud-localhost"),
                    mapOf("iss" to newIssuer.toString()),
                    3600,
                ),
            )
        return "Bearer " + token.serialize()
    }
}
