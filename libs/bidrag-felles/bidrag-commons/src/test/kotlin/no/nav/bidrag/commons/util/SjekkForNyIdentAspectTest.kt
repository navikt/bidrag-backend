package no.nav.bidrag.commons.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.CodeSignature
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException

class SjekkForNyIdentAspectTest {
    private val identConsumer = mockk<IdentConsumer>()
    private val aspect = SjekkForNyIdentAspect(identConsumer)
    private val ident = genererFødselsnummer()

    @Test
    fun `skal bytte til gjeldende ident`() {
        val nyIdent = genererFødselsnummer()
        every { identConsumer.sjekkIdent(ident) } returns nyIdent

        kall() shouldBe nyIdent
    }

    @Test
    fun `skal beholde innsendt ident når bidrag-person ikke finner personen`() {
        every { identConsumer.sjekkIdent(ident) } returns null

        kall() shouldBe ident
    }

    @Test
    fun `skal kaste feil videre når bidrag-person feiler`() {
        every { identConsumer.sjekkIdent(ident) } throws
            HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "", HttpHeaders.EMPTY, ByteArray(0), null)

        shouldThrow<HttpServerErrorException> { kall() }
    }

    private fun kall(): Any? {
        val joinPoint = mockk<ProceedingJoinPoint> {
            every { args } returns arrayOf(ident)
            every { signature } returns mockk<CodeSignature> { every { parameterNames } returns arrayOf("ident") }
            every { proceed(any()) } answers { firstArg<Array<Any?>>()[0] }
        }
        return aspect.prosseserNyIdent(joinPoint, SjekkForNyIdent("ident"))
    }
}
