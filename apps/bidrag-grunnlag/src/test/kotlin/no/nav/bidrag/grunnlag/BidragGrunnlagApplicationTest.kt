package no.nav.bidrag.grunnlag

import no.nav.bidrag.grunnlag.BidragGrunnlagTest.Companion.TEST_PROFILE
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = [BidragGrunnlagTest::class])
@ActiveProfiles(TEST_PROFILE)
@DisplayName("BidragGrunnlag")
@ComponentScan(
    basePackages = ["no.nav.bidrag.commons.util"],
    excludeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = [BidragGrunnlag::class, BidragGrunnlagTest::class])],
)
@EnableWireMock(
    ConfigureWireMock(name = "my-service", port = 0),
)
@EnableMockOAuth2Server
class BidragGrunnlagApplicationTest {

    @Test
    fun `skal laste spring-context`() {
    }
}
