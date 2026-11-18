package no.nav.bidrag.sak.mapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.sak.domain.Rolle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.net.SocketTimeoutException

class HentNyesteIdentTest {
    private val ident = genererFødselsnummer()
    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.bindTo(restTemplate).build()
    private val consumer = IdentConsumer("http://bidrag-person", restTemplate)

    @BeforeEach
    fun setup() {
        mockkObject(AppContext)
        every { AppContext.getBean(IdentConsumer::class.java) } returns consumer
    }

    @AfterEach
    fun cleanup() {
        unmockkObject(AppContext)
    }

    @ParameterizedTest
    @EnumSource(HttpStatus::class, names = ["NOT_FOUND", "NO_CONTENT"])
    fun `beholder innsendt ident ved manglende treff`(status: HttpStatus) {
        server.expect(requestTo("http://bidrag-person/personidenter")).andRespond(withStatus(status))

        identForBarn(ident) shouldBe Personident(ident)

        server.verify()
    }

    @ParameterizedTest
    @EnumSource(HttpStatus::class, names = ["BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"])
    fun `skal kaste andre HTTP-feil videre`(status: HttpStatus) {
        server.expect(requestTo("http://bidrag-person/personidenter")).andRespond(withStatus(status))

        shouldThrow<HttpStatusCodeException> { identForBarn(ident) }.statusCode shouldBe status

        server.verify()
    }

    @Test
    fun `skal kaste timeout videre`() {
        server.expect(requestTo("http://bidrag-person/personidenter"))
            .andRespond(withException(SocketTimeoutException("Timeout")))

        shouldThrow<RuntimeException> { identForBarn(ident) }.cause.shouldBeInstanceOf<ResourceAccessException>()

        server.verify()
    }

    private fun identForBarn(fnr: String) = listOf(Rolle(fødselsnummer = fnr, rolleType = Rolletype.BARN)).toBarnISak().single().ident
}
