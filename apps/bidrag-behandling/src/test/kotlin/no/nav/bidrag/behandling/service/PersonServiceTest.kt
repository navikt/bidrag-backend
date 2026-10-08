package no.nav.bidrag.behandling.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.bidrag.behandling.consumer.BidragPersonConsumer
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.ExpectedCount.twice
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.net.SocketTimeoutException
import java.net.URI

class PersonServiceTest {
    private val ident = genererFødselsnummer()
    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.bindTo(restTemplate).build()
    private val consumer = BidragPersonConsumer(URI("http://bidrag-person"), restTemplate)
    private val oppslag = listOf(::hentPerson, PersonService(consumer)::hentPerson)

    @BeforeEach
    fun setup() {
        mockkObject(AppContext)
        every { AppContext.getBean(BidragPersonConsumer::class.java) } returns consumer
    }

    @AfterEach
    fun cleanup() {
        unmockkObject(AppContext)
    }

    @ParameterizedTest
    @EnumSource(HttpStatus::class, names = ["NOT_FOUND", "NO_CONTENT"])
    fun `skal returnere null ved manglende treff`(status: HttpStatus) {
        server.expect(twice(), requestTo("http://bidrag-person/informasjon")).andRespond(withStatus(status))

        oppslag.forEach { it(ident).shouldBeNull() }

        server.verify()
    }

    @ParameterizedTest
    @EnumSource(HttpStatus::class, names = ["BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"])
    fun `skal kaste andre HTTP-feil videre`(status: HttpStatus) {
        server.expect(twice(), requestTo("http://bidrag-person/informasjon")).andRespond(withStatus(status))

        oppslag.forEach { kall ->
            shouldThrow<HttpStatusCodeException> { kall(ident) }.statusCode shouldBe status
        }

        server.verify()
    }

    @Test
    fun `skal kaste timeout videre`() {
        server.expect(twice(), requestTo("http://bidrag-person/informasjon"))
            .andRespond(withException(SocketTimeoutException("Timeout")))

        oppslag.forEach { kall ->
            shouldThrow<RuntimeException> { kall(ident) }.cause.shouldBeInstanceOf<ResourceAccessException>()
        }

        server.verify()
    }
}
