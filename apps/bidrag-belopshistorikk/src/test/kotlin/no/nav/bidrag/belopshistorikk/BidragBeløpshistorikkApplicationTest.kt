package no.nav.bidrag.belopshistorikk

import no.nav.bidrag.belopshistorikk.BidragBeløpshistorikkTest.Companion.TEST_PROFILE
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = [BidragBeløpshistorikkTest::class])
@ActiveProfiles(TEST_PROFILE)
@DisplayName("BidragBeløpshistorikk")
@EnableWireMock(ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
class BidragBeløpshistorikkApplicationTest {
    @Test
    fun `skal laste spring-context`() {
    }
}
