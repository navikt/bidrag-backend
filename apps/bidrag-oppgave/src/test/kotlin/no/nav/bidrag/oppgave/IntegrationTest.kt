package no.nav.bidrag.oppgave

import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.test.context.ContextConfiguration

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [MockOidcServerInitializer::class])
annotation class IntegrationTest

object MockOidcServer {
    const val AZURE_APP_CLIENT_ID = "bidrag-oppgave"
    private const val ISSUER_ID = "azure"

    val server: MockOAuth2Server = MockOAuth2Server().apply {
        start()
        runCatching { wellKnownUrl(ISSUER_ID).toUrl().readText() }
        runCatching { jwksUrl(ISSUER_ID).toUrl().readText() }
    }
    val jwk: String = RSAKeyGenerator(2048).keyID("test-key").generate().toJSONString()

    fun issueToken(subject: String = "testbruker"): String = server.issueToken(
        ISSUER_ID,
        subject,
        DefaultOAuth2TokenCallback(
            issuerId = ISSUER_ID,
            subject = subject,
            audience = listOf(AZURE_APP_CLIENT_ID),
        ),
    ).serialize()

    fun issuerUrl(): String = server.issuerUrl(ISSUER_ID).toString()

    fun tokenEndpointUrl(): String = server.tokenEndpointUrl(ISSUER_ID).toString()
}

class MockOidcServerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val properties =
            mapOf(
                "AZURE_OPENID_CONFIG_ISSUER" to MockOidcServer.issuerUrl(),
                "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to MockOidcServer.tokenEndpointUrl(),
                "AZURE_APP_CLIENT_ID" to MockOidcServer.AZURE_APP_CLIENT_ID,
                "AZURE_APP_JWK" to MockOidcServer.jwk,
                "OPPGAVE_API_SCOPE" to "api://oppgave/.default",
                "OPPGAVE_API_URL" to "http://localhost",
            )
        applicationContext.environment.propertySources
            .addFirst(MapPropertySource("mockOidcServer", properties))
    }
}
