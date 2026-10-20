package no.nav.bidrag.commons.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.person.Identgruppe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestOperations
import java.net.URI

class BidragPersonOppslagClientTest {
    private val restOperations = mockk<RestOperations>()
    private val client = BidragPersonOppslagClient("http://bidrag-person", restOperations)

    @ParameterizedTest
    @MethodSource("oppslagOgIkkeFunnet")
    fun `skal returnere null når personen ikke finnes`(
        oppslag: Oppslag,
        ikkeFunnet: Svar,
    ) {
        stub(ikkeFunnet)

        oppslag.kall(client).shouldBeNull()
    }

    @Test
    fun `hentPersonidenter skal returnere null ved tom liste`() {
        stub(Svar("tom liste") { ResponseEntity.ok(emptyList<Any>()) })

        client.hentPersonidenter(IDENT, setOf(Identgruppe.FOLKEREGISTERIDENT), true).shouldBeNull()
    }

    @ParameterizedTest
    @MethodSource("oppslag")
    fun `skal kaste andre feil videre`(oppslag: Oppslag) {
        stub(Svar("500") { throw HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "", HttpHeaders.EMPTY, ByteArray(0), null) })

        shouldThrow<HttpServerErrorException> { oppslag.kall(client) }
    }

    @ParameterizedTest
    @MethodSource("oppslag")
    fun `skal kaste timeout videre`(oppslag: Oppslag) {
        val feil = ResourceAccessException("Timeout")
        stub(Svar("timeout") { throw feil })

        shouldThrow<RuntimeException> { oppslag.kall(client) }.cause shouldBe feil
    }

    private fun stub(svar: Svar) {
        every { restOperations.exchange(any<URI>(), HttpMethod.POST, any(), any<ParameterizedTypeReference<Any>>()) } answers { svar.svar() }
    }

    class Oppslag(
        private val navn: String,
        val kall: (BidragPersonOppslagClient) -> Any?,
    ) {
        override fun toString() = navn
    }

    class Svar(
        private val navn: String,
        val svar: () -> ResponseEntity<Any>,
    ) {
        override fun toString() = navn
    }

    companion object {
        private val IDENT = genererFødselsnummer()

        @JvmStatic
        fun oppslag() = listOf(
            Oppslag("hentPersonidenter") { it.hentPersonidenter(IDENT, emptySet(), true) },
            Oppslag("hentPersonInformasjon") { it.hentPersonInformasjon(Personident(IDENT)) },
        )

        @JvmStatic
        fun oppslagOgIkkeFunnet() = oppslag().flatMap { oppslag ->
            listOf(
                Svar("204") { ResponseEntity.noContent().build() },
                Svar("404 som klientfeil") {
                    throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "", HttpHeaders.EMPTY, ByteArray(0), null)
                },
                Svar("404 som serverfeil") {
                    throw HttpServerErrorException.create(HttpStatus.NOT_FOUND, "", HttpHeaders.EMPTY, ByteArray(0), null)
                },
            ).map { Arguments.of(oppslag, it) }
        }
    }
}
