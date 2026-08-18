package no.nav.bidrag.statistikk

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.bidrag.statistikk.BidragStatistikkTest.Companion.TEST_PROFILE
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = [BidragStatistikkTest::class])
@ActiveProfiles(TEST_PROFILE)
@DisplayName("BidragStatistikk")
@EnableMockOAuth2Server
@EnableWireMock(ConfigureWireMock(name = "my-service", port = 0))
class BidragStatistikkApplicationTest {
    @InjectWireMock("my-service")
    var wireMock: WireMockServer? = null

    @Test
    fun `skal laste spring-context`() {
    }
}
