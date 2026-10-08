package no.nav.bidrag.commons.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.CodeSignature
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException

class SjekkForNyIdentAspectTest {
    private val identConsumer = mockk<IdentConsumer>()
    private val aspect = SjekkForNyIdentAspect(identConsumer)
    private val ident = genererFødselsnummer()

    @ParameterizedTest
    @EnumSource(Plassering::class)
    fun `skal bytte til gjeldende ident`(plassering: Plassering) {
        val nyIdent = genererFødselsnummer()
        every { identConsumer.sjekkIdent(ident) } returns nyIdent

        for (feilHvisIkkeFunnet in listOf(false, true)) {
            kall(plassering, feilHvisIkkeFunnet) shouldBe nyIdent
            kall(plassering, feilHvisIkkeFunnet, Personident(ident)) shouldBe Personident(nyIdent)
            kall(plassering, feilHvisIkkeFunnet, Ident(ident)) shouldBe Ident(nyIdent)
        }
    }

    @ParameterizedTest
    @EnumSource(Plassering::class)
    fun `skal beholde innsendt ident når bidrag-person ikke finner personen`(plassering: Plassering) {
        every { identConsumer.sjekkIdent(ident) } returns null

        kall(plassering) shouldBe ident
        kall(plassering, verdi = Personident(ident)) shouldBe Personident(ident)
        kall(plassering, verdi = Ident(ident)) shouldBe Ident(ident)
    }

    @ParameterizedTest
    @EnumSource(Plassering::class)
    fun `skal kaste 404 ved manglende treff når flagget er på`(plassering: Plassering) {
        every { identConsumer.sjekkIdent(ident) } returns null

        for (verdi in listOf(ident, Personident(ident), Ident(ident))) {
            val joinPoint = joinPoint(plassering, true, verdi)

            shouldThrow<HttpClientErrorException> {
                kall(plassering, true, joinPoint)
            }.statusCode shouldBe HttpStatus.NOT_FOUND

            verify(exactly = 0) { joinPoint.proceed(any<Array<Any?>>()) }
        }
    }

    @ParameterizedTest
    @EnumSource(Plassering::class)
    fun `skal kaste feil videre når bidrag-person feiler`(plassering: Plassering) {
        every { identConsumer.sjekkIdent(ident) } throws
            HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "", HttpHeaders.EMPTY, ByteArray(0), null)

        for (feilHvisIkkeFunnet in listOf(false, true)) {
            shouldThrow<HttpServerErrorException> { kall(plassering, feilHvisIkkeFunnet) }
        }
    }

    @ParameterizedTest
    @EnumSource(Plassering::class)
    fun `skal beholde innsendt ident ved alle oppslagsfeil når ignorerFeil er på`(plassering: Plassering) {
        for (feil in listOf(
            HttpClientErrorException(HttpStatus.FORBIDDEN),
            HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR),
            ResourceAccessException("Timeout"),
        )) {
            every { identConsumer.sjekkIdent(ident) } throws feil

            for (verdi in listOf(ident, Personident(ident), Ident(ident))) {
                kall(plassering, true, verdi, ignorerFeil = true) shouldBe verdi
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Plassering::class)
    fun `ignorerFeil skal overstyre feilHvisIkkeFunnet ved manglende treff`(plassering: Plassering) {
        every { identConsumer.sjekkIdent(ident) } returns null

        kall(plassering, true, ignorerFeil = true) shouldBe ident
    }

    @ParameterizedTest
    @EnumSource(Plassering::class)
    fun `ignorerFeil skal ikke svelge feil fra annotert funksjon`(plassering: Plassering) {
        every { identConsumer.sjekkIdent(ident) } returns ident
        val joinPoint = joinPoint(plassering, false, ident, ignorerFeil = true)
        val feil = IllegalStateException("Feil fra funksjon")
        every { joinPoint.proceed(any<Array<Any?>>()) } throws feil

        shouldThrow<IllegalStateException> {
            kall(plassering, false, joinPoint, ignorerFeil = true)
        } shouldBe feil
    }

    private fun kall(
        plassering: Plassering,
        feilHvisIkkeFunnet: Boolean = false,
        verdi: Any = ident,
        ignorerFeil: Boolean = false,
    ): Any? = kall(plassering, feilHvisIkkeFunnet, joinPoint(plassering, feilHvisIkkeFunnet, verdi, ignorerFeil), ignorerFeil)

    private fun kall(
        plassering: Plassering,
        feilHvisIkkeFunnet: Boolean,
        joinPoint: ProceedingJoinPoint,
        ignorerFeil: Boolean = false,
    ): Any? = when (plassering) {
        Plassering.FUNKSJON -> aspect.prosseserNyIdent(joinPoint, SjekkForNyIdent("ident", feilHvisIkkeFunnet = feilHvisIkkeFunnet, ignorerFeil = ignorerFeil))
        Plassering.PARAMETER -> aspect.prosseserNyIdent(joinPoint)
    }

    private fun joinPoint(
        plassering: Plassering,
        feilHvisIkkeFunnet: Boolean,
        verdi: Any,
        ignorerFeil: Boolean = false,
    ): ProceedingJoinPoint = mockk {
        every { args } returns arrayOf(verdi)
        every { signature } returns when (plassering) {
            Plassering.FUNKSJON -> mockk<CodeSignature> {
                every { parameterNames } returns arrayOf("ident")
                every { toShortString() } returns "ParameterAnnotasjoner.oppslag(..)"
            }

            Plassering.PARAMETER -> mockk<MethodSignature> {
                every { toShortString() } returns "ParameterAnnotasjoner.oppslag(..)"
                every { method } returns ParameterAnnotasjoner::class.java.getDeclaredMethod(
                    when {
                        ignorerFeil && feilHvisIkkeFunnet -> "ignorerFeilOgManglendeTreff"
                        ignorerFeil -> "ignorerFeil"
                        feilHvisIkkeFunnet -> "strengtOppslag"
                        else -> "standardOppslag"
                    },
                    String::class.java,
                )
            }
        }
        every { proceed(any()) } answers { firstArg<Array<Any?>>()[0] }
    }

    enum class Plassering { FUNKSJON, PARAMETER }


    @Suppress("unused")
    class ParameterAnnotasjoner {
        fun standardOppslag(@SjekkForNyIdent ident: String) = ident

        fun strengtOppslag(@SjekkForNyIdent(feilHvisIkkeFunnet = true) ident: String) = ident

        fun ignorerFeil(@SjekkForNyIdent(ignorerFeil = true) ident: String) = ident

        fun ignorerFeilOgManglendeTreff(@SjekkForNyIdent(feilHvisIkkeFunnet = true, ignorerFeil = true) ident: String) = ident
    }
}
