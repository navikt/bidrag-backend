package no.nav.bidrag.grunnlag

import com.fasterxml.jackson.core.JsonProcessingException
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.bidrag.commons.service.organisasjon.SaksbehandlerInfoResponse
import no.nav.bidrag.grunnlag.BidragGrunnlagLocal.Companion.LOCAL_PROFILE
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.junit.Assert
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@EnableAspectJAutoProxy
@EnableJwtTokenValidation(ignore = ["org.springdoc", "org.springframework"])
@ActiveProfiles(LOCAL_PROFILE)
@ComponentScan(basePackages = ["no.nav.bidrag.commons.util"])
class BidragGrunnlagLocal {
    companion object {
        const val LOCAL_PROFILE = "local"
    }
}

fun main(args: Array<String>) {
    val wireMockServer = WireMockServer(
        WireMockConfiguration.wireMockConfig().dynamicPort().dynamicHttpsPort(),
    ) // No-args constructor will start on port 8080, no HTTPS
    wireMockServer.start()

    val profile = if (args.isEmpty()) LOCAL_PROFILE else args[0]
    val app = SpringApplication(BidragGrunnlagLocal::class.java)
    stubHentSaksbehandler()
    app.setAdditionalProfiles(profile)
    app.run(*args)

    wireMockServer.resetAll()
    wireMockServer.stop()
}

fun stubHentSaksbehandler() {
    WireMock.stubFor(
        WireMock.get(WireMock.urlMatching("/organisasjon/saksbehandler/info/(.*)")).willReturn(
            WireMock.aResponse()
                .withHeader(HttpHeaders.CONNECTION, "close")
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .withStatus(HttpStatus.OK.value())
                .withBody(jsonToString(SaksbehandlerInfoResponse("Z99999", "Fornavn Etternavn"))),
        ),
    )
}

private fun jsonToString(data: Any): String = try {
    commonObjectmapper.findAndRegisterModules().writeValueAsString(data)
} catch (e: JsonProcessingException) {
    Assert.fail(e.message)
    ""
}
