package no.nav.bidrag.behandling.service

import com.github.tomakehurst.wiremock.WireMockServer
import io.mockk.unmockkObject
import no.nav.bidrag.behandling.BehandlingAppTest
import no.nav.bidrag.commons.service.AppContext
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock

@ActiveProfiles("test")
@SpringBootTest(
    classes = [BehandlingAppTest::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@EnableWireMock(ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
@Import(AppContext::class)
abstract class CommonTestRunner {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

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
            .forEach(WireMockServer::resetAll)
    }
}
