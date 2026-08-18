package no.nav.bidrag.bbm.api

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.bidrag.bbm.CommonTestRunner
import no.nav.bidrag.bbm.utils.PERSONIDENT_BARN_1
import no.nav.bidrag.bbm.utils.PERSONIDENT_BARN_2
import no.nav.bidrag.bbm.utils.SAKSNUMMER_1
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningRequestDto
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.time.LocalDate

class BBMControllerValideringTest : CommonTestRunner() {
    @Test
    fun `Skal feile med BAD_REQUEST hvis ugyldig saksnummer`() {
        val httpEntity =
            HttpEntity(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = "123213123213123",
                            personidentBarn = Personident(PERSONIDENT_BARN_2),
                            søknadsid = "1",
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            )

        assertSoftly(
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning",
                httpEntity,
                String::class.java,
            ),
        ) {
            statusCode shouldBe HttpStatus.BAD_REQUEST
            body shouldContain "Saksnummer må bestå av 7 tegn"
        }
    }

    @Test
    fun `Skal feile med BAD_REQUEST ugyldig personident barn`() {
        val httpEntity =
            HttpEntity(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_1,
                            personidentBarn = Personident("123213213213213213"),
                            datoSøknad = LocalDate.parse("2024-01-01"),
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            )

        assertSoftly(
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning",
                httpEntity,
                String::class.java,
            ),
        ) {
            statusCode shouldBe HttpStatus.BAD_REQUEST
            body shouldContain "Personident må inneholde 11 tegn"
        }
    }

    @Test
    fun `Skal feile med BAD_REQUEST hvis datosøknad og søknadsid mangler`() {
        val httpEntity =
            HttpEntity(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_1,
                            personidentBarn = Personident(PERSONIDENT_BARN_1),
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_1,
                            personidentBarn = Personident(PERSONIDENT_BARN_1),
                            søknadsid = "1",
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            )

        assertSoftly(
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning",
                httpEntity,
                String::class.java,
            ),
        ) {
            statusCode shouldBe HttpStatus.BAD_REQUEST
            body shouldContain "Enten datoSøknad eller søknadsid må settes"
        }
    }

    @Test
    fun `Skal feile med BAD_REQUEST hvis søknad ikke finnes`() {
        val httpEntity =
            HttpEntity(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_1,
                            personidentBarn = Personident(PERSONIDENT_BARN_1),
                            stønadstype = Stønadstype.BIDRAG,
                            søknadsid = "123213213",
                        ),
                    ),
                ),
            )

        assertSoftly(
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning",
                httpEntity,
                String::class.java,
            ),
        ) {
            statusCode shouldBe HttpStatus.NOT_FOUND
            headers[HttpHeaders.WARNING]?.first() shouldContain "Fant ikke søknad med id 123213213"
        }
    }
}
