package no.nav.bidrag.dokument.bestilling

import com.nimbusds.jose.JOSEObjectType
import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.dokument.bestilling.utils.SAKSBEHANDLER_IDENT
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders

@Configuration
@Profile("test")
class TestRestTemplateConfiguration {
    @Autowired
    lateinit var mockOAuth2Server: MockOAuth2Server

    @Value($$"${AZURE_APP_CLIENT_ID}")
    private lateinit var clientId: String

    @Bean
    fun httpHeaderTestRestTemplate(): TestRestTemplate = TestRestTemplate(
        RestTemplateBuilder()
            .additionalInterceptors({ request, body, execution ->
                request.headers.add(HttpHeaders.AUTHORIZATION, generateBearerToken())
                execution.execute(request, body)
            })
            .defaultMessageConverters()
            .additionalMessageConverters(
                CustomJacksonHttpMessageConverter(commonObjectmapper),
            ),
    )

    fun generateBearerToken(): String {
        val iss = mockOAuth2Server.issuerUrl("aad")
        val newIssuer = iss.newBuilder().host("localhost").build()
        val token =
            mockOAuth2Server.issueToken(
                "aad",
                clientId,
                DefaultOAuth2TokenCallback(
                    "aad",
                    SAKSBEHANDLER_IDENT,
                    JOSEObjectType.JWT.type,
                    listOf(clientId),
                    mapOf("iss" to newIssuer.toString(), "azp_name" to "bidrag-dokument-bestilling-test"),
                    3600,
                ),
            )
        return "Bearer " + token.serialize()
    }
}
