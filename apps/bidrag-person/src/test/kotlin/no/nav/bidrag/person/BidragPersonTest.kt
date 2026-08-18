package no.nav.bidrag.person

import com.nimbusds.jose.JOSEObjectType
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.http.HttpHeaders

const val ISSUER = "aad"

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@PropertySource("classpath:url.properties", "classpath:secret.properties")
@EnableJwtTokenValidation(ignore = ["springfox.documentation.swagger.web.ApiResourceController"])
@ComponentScan(
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            value = [BidragPerson::class],
        ),
    ],
)
class BidragPersonTest {
    @Configuration
    @Profile(OIDC_TOKEN_TEST)
    class SecuredTestRestTemplateConfiguration {
        @Autowired
        lateinit var mockOAuth2Server: MockOAuth2Server

        @Bean
        fun httpHeaderTestRestTemplate(): TestRestTemplate = TestRestTemplate(
            RestTemplateBuilder()
                .additionalInterceptors({ request, body, execution ->
                    request.headers.add(HttpHeaders.AUTHORIZATION, generateTestToken())
                    execution.execute(request, body)
                }),
        )

        private fun generateTestToken(): String {
            val iss = mockOAuth2Server.issuerUrl(ISSUER)
            val newIssuer = iss.newBuilder().host("localhost").build()
            val token =
                mockOAuth2Server.issueToken(
                    ISSUER,
                    "aud-localhost",
                    DefaultOAuth2TokenCallback(
                        ISSUER,
                        "aud-localhost",
                        JOSEObjectType.JWT.type,
                        listOf("aud-localhost"),
                        mapOf("iss" to newIssuer.toString()),
                        3600,
                    ),
                )
            return "Bearer " + token.serialize()
        }
    }

    companion object {
        const val OIDC_TOKEN_TEST = "oidc-token-test" // see application.yaml
        const val LOCAL = "local" // Enable endpoint testing with Swagger locally, see application.yaml

        @JvmStatic
        fun main(args: Array<String>) {
            val app = SpringApplication(BidragPersonTest::class.java)
            app.setAdditionalProfiles(LOCAL)
            app.run(*args)
        }
    }
}
