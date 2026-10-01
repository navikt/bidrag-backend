package no.nav.bidrag.behandling

import com.github.tomakehurst.wiremock.WireMockServer
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.bidrag.behandling.utils.StubUtils
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [BidragBehandlingLocal::class])
@SpringBootTest(classes = [BidragBehandlingLocal::class, StubUtils::class, TestRestTemplateConfiguration::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock(ConfigureWireMock(port = 0))
@ActiveProfiles("test")
@EnableMockOAuth2Server
class SpringTestRunner {
    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    lateinit var stubUtils: StubUtils

    @Autowired
    lateinit var httpHeaderTestRestTemplate: TestRestTemplate

    @BeforeEach
    fun mockkUnleash() {
        // Nullstiller toggle-stubber som andre testklasser har lagt igjen på det globale objektet
        unmockkObject(UnleashFeaturesProvider)
        mockkObject(UnleashFeaturesProvider)
    }

    @BeforeEach
    fun pekAppContextTilGjeldendeKontekst() {
        // AppContext er statisk og peker på sist startede Spring-kontekst. Når en cachet kontekst gjenbrukes
        // må den pekes tilbake, ellers går oppslag via AppContext mot feil kontekst (og feil WireMock-server).
        unmockkObject(AppContext)
        AppContext().setApplicationContext(applicationContext)
    }

    @AfterEach
    fun reset() {
        resetWiremockServers()
    }

    private fun resetWiremockServers() {
        applicationContext
            .getBeansOfType(WireMockServer::class.java)
            .values
            .forEach {
                it.resetAll()
                it
            }
    }

    protected fun getPort(): String = port.toString()
}
