package no.nav.bidrag.sak

import com.github.tomakehurst.wiremock.WireMockServer
import com.nimbusds.jose.JOSEObjectType
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.sak.config.DbContainerInitializer
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.util.UriComponentsBuilder

@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class], classes = [BidragSakLocal::class])
@SpringBootTest(classes = [BidragSakLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(BidragSakProfiles.TEST, "mock-oauth")
@EnableMockOAuth2Server
@AutoConfigureTestRestTemplate
class SpringTestRunner {
    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    protected lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @MockkBean
    protected lateinit var identConsumer: IdentConsumer

    @BeforeEach
    fun mockSetup() {
        every { identConsumer.hentAlleIdenter(any()) }.answers { listOf(firstArg()) }
        every { identConsumer.hentPersonInformasjon(any()) }.answers { null }
    }

    @AfterEach
    fun reset() {
        resetWiremockServers()
    }

    private fun resetWiremockServers() {
        applicationContext
            .getBeansOfType(WireMockServer::class.java)
            .values
            .forEach(WireMockServer::resetRequests)
    }

    protected fun getPort(): String = port.toString()

    protected fun localhost(uri: String): String = UriComponentsBuilder.fromUriString(LOCALHOST + getPort()).pathSegment(uri).toUriString()

    protected fun lokalTestToken(): String {
        val iss = mockOAuth2Server.issuerUrl("aad")
        val newIssuer = iss.newBuilder().host("localhost").build()
        return mockOAuth2Server
            .issueToken(
                "aad",
                "aud-localhost",
                DefaultOAuth2TokenCallback(
                    issuerId = "aad",
                    subject = "aud-localhost",
                    typeHeader = JOSEObjectType.JWT.type,
                    audience = listOf("aud-localhost"),
                    claims = mapOf("iss" to newIssuer.toString(), "NAVident" to "AB12345"),
                    3600,
                ),
            ).serialize()
    }

    companion object {
        private const val LOCALHOST = "http://localhost:"
    }
}
