package no.nav.bidrag.samhandler

import com.github.tomakehurst.wiremock.WireMockServer
import com.nimbusds.jose.JOSEObjectType
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.samhandler.config.SamhandlerTestConfig
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.util.UriComponentsBuilder
import org.testcontainers.postgresql.PostgreSQLContainer
import org.wiremock.spring.EnableWireMock

@ActiveProfiles("test")
@SpringBootTest(classes = [BidragSamhandlerLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
@EnableWireMock
@AutoConfigureTestRestTemplate
@Import(SamhandlerTestConfig::class)
class SpringTestRunner {
    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    protected lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

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

    fun rootUri(): String = LOCALHOST + port

    fun rootUriComponentsBuilder(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(rootUri())

    protected fun getPort(): String = port.toString()

    val httpHeaderTestRestTemplate
        get() =
            HttpHeaderTestRestTemplate(testRestTemplate).apply {
                add(HttpHeaders.AUTHORIZATION) { generateTestToken() }
            }

    fun generateTestToken(): String {
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
        return "Bearer " + token.serialize()
    }

    companion object {
        private const val LOCALHOST = "http://localhost:"

        private var postgreSqlDb =
            PostgreSQLContainer("postgres:latest").apply {
                withDatabaseName("bidrag-samhandler")
                withUsername("cloudsqliamuser")
                withPassword("admin")
                start()
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgreSqlDb::getJdbcUrl)
            registry.add("spring.datasource.username", postgreSqlDb::getUsername)
            registry.add("spring.datasource.password", postgreSqlDb::getPassword)
        }
    }
}
