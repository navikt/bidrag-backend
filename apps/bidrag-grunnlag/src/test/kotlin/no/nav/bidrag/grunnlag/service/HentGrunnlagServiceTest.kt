package no.nav.bidrag.grunnlag.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.grunnlag.consumer.bidragperson.BidragPersonConsumer
import no.nav.bidrag.grunnlag.exception.RestResponse
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientException
import java.lang.reflect.InvocationTargetException

class HentGrunnlagServiceTest {
    private val ident = genererFødselsnummer()
    private val consumer = mockk<BidragPersonConsumer>()
    private val service = HentGrunnlagService(
        inntektskomponentenService = mockk(),
        sigrunConsumer = mockk(),
        familieBaSakConsumer = mockk(),
        pensjonConsumer = mockk(),
        familieKsSakConsumer = mockk(),
        bidragPersonConsumer = consumer,
        familieEfSakConsumer = mockk(),
        arbeidsforholdConsumer = mockk(),
        enhetsregisterConsumer = mockk(),
        tilleggsstønadConsumer = mockk(),
    )
    private val hentIdenter = HentGrunnlagService::class.java.getDeclaredMethod("hentIdenterFraConsumer", String::class.java)
        .apply { isAccessible = true }

    @ParameterizedTest
    @EnumSource(HttpStatus::class, names = ["NOT_FOUND", "NO_CONTENT"])
    fun `skal beholde innsendt ident ved manglende treff`(status: HttpStatus) {
        every { consumer.hentPersonidenter(any(), true) } returns
            RestResponse.Failure("Person ikke funnet", status, RestClientException("Person ikke funnet"))

        hentIdenter.invoke(service, ident) shouldBe listOf(HistoriskIdent(ident, false))
    }

    @ParameterizedTest
    @EnumSource(HttpStatus::class, names = ["BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"])
    fun `skal kaste andre feil videre`(status: HttpStatus) {
        val feil = RestClientException("Kall til tjenesten feilet")
        every { consumer.hentPersonidenter(any(), true) } returns RestResponse.Failure("Kall til tjenesten feilet", status, feil)

        shouldThrow<InvocationTargetException> { hentIdenter.invoke(service, ident) }.cause shouldBe feil
    }
}
