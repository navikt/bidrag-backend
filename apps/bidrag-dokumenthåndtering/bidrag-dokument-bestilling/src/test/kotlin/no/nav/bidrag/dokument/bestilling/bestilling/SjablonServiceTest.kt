@file:Suppress("ktlint:standard:property-naming")

package no.nav.bidrag.dokument.bestilling.bestilling

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.dokument.bestilling.consumer.SjablonConsumer
import no.nav.bidrag.dokument.bestilling.consumer.dto.SjablongerDto
import no.nav.bidrag.dokument.bestilling.model.MAX_DATE
import no.nav.bidrag.dokument.bestilling.model.typeRef
import no.nav.bidrag.dokument.bestilling.tjenester.SjablongService
import no.nav.bidrag.dokument.bestilling.utils.readFile
import no.nav.bidrag.domene.enums.person.Sivilstandskode
import no.nav.bidrag.transport.dokumentmaler.BeløpFraTil
import no.nav.bidrag.transport.dokumentmaler.ForskuddInntektgrensePeriode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class SjablonServiceTest {
    @MockK
    lateinit var sjablonConsumer: SjablonConsumer

    @InjectMockKs
    lateinit var sjablongService: SjablongService

    @BeforeEach
    fun initMocks() {
        val sjablonResponse =
            ObjectMapper()
                .findAndRegisterModules()
                .readValue(readFile("api/sjablon_all.json"), typeRef<SjablongerDto>())
        every { sjablonConsumer.hentSjablonger() } returns sjablonResponse
    }

    @Test
    fun `skal hente inntekgrense forskudd for periode`() {
        sjablongService.hentInntektGrenseForPeriode(LocalDate.parse("2023-06-30")) shouldBe BigDecimal(52800)
        sjablongService.hentInntektGrenseForPeriode(LocalDate.parse("2022-06-30")) shouldBe BigDecimal(51300)
    }

    @Test
    fun `skal hente maks innteksgrense forskudd for periode`() {
        sjablongService.hentMaksInntektForPeriode(LocalDate.parse("2023-06-30")) shouldBe BigDecimal(563200)
        sjablongService.hentMaksInntektForPeriode(LocalDate.parse("2022-06-30")) shouldBe BigDecimal(547200)
    }

    @Test
    fun `skal hente forskudd innteksgrenser for periode 2022 - 2023`() {
        val fraDato = LocalDate.parse("2023-01-02")
        val inntektgrenser =
            sjablongService.hentForskuddInntektgrensePerioder(fraDato)
        assertSoftly {
            inntektgrenser shouldHaveSize 8 * 3
            inntektgrenser.filter { it.fomDato == fraDato } shouldHaveSize 8
            inntektgrenser.filter { it.tomDato == MAX_DATE } shouldHaveSize 8
            val ensligGrenser = inntektgrenser.filter { it.forsorgerType == Sivilstandskode.ENSLIG }
            ensligGrenser shouldHaveSize 4 * 3
            ensligGrenser[0].antallBarn shouldBe 1
            ensligGrenser[1].antallBarn shouldBe 2
            ensligGrenser[2].antallBarn shouldBe 3
            ensligGrenser[3].antallBarn shouldBe 4

            // Valider beregning av inntektgrenser for 2023 basert på reele sjablonverdier
            ensligGrenser
                .hentForAntallBarn(1)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(312701), BigDecimal(488300)))
            ensligGrenser
                .hentForAntallBarn(2)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(312701), BigDecimal(557200)))
            ensligGrenser
                .hentForAntallBarn(3)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(312701), BigDecimal(563200)))
            ensligGrenser
                .hentForAntallBarn(4)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(312701), BigDecimal(563200)))

            ensligGrenser
                .hentForAntallBarn(1)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(488301), BigDecimal(563200)))
            ensligGrenser
                .hentForAntallBarn(2)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(557201), BigDecimal(563200)))
            ensligGrenser
                .hentForAntallBarn(3)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(563200), BigDecimal(563200)))
            ensligGrenser
                .hentForAntallBarn(3)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(563200), BigDecimal(563200)))

            val giftSamboerGrenser =
                inntektgrenser.filter { it.forsorgerType == Sivilstandskode.GIFT_SAMBOER }
            giftSamboerGrenser shouldHaveSize 4 * 3
            giftSamboerGrenser[0].antallBarn shouldBe 1
            giftSamboerGrenser[1].antallBarn shouldBe 2
            giftSamboerGrenser[2].antallBarn shouldBe 3
            giftSamboerGrenser[3].antallBarn shouldBe 4

            giftSamboerGrenser
                .hentForAntallBarn(1)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(312701), BigDecimal(375700)))
            giftSamboerGrenser
                .hentForAntallBarn(2)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(312701), BigDecimal(444600)))
            giftSamboerGrenser
                .hentForAntallBarn(3)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(312701), BigDecimal(513500)))
            giftSamboerGrenser
                .hentForAntallBarn(4)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(312701), BigDecimal(563200)))

            giftSamboerGrenser
                .hentForAntallBarn(1)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(375701), BigDecimal(563200)))
            giftSamboerGrenser
                .hentForAntallBarn(2)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(444601), BigDecimal(563200)))
            giftSamboerGrenser
                .hentForAntallBarn(3)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(513501), BigDecimal(563200)))
            giftSamboerGrenser
                .hentForAntallBarn(4)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(563200), BigDecimal(563200)))
        }
    }

    @Test
    fun `skal hente forskudd innteksgrenser for periode 2020 - 2021`() {
        val fraDato = LocalDate.parse("2020-07-01")
        val tomDato = LocalDate.parse("2021-06-30")
        val inntektgrenser =
            sjablongService.hentForskuddInntektgrensePerioder(fraDato, tomDato)
        assertSoftly {
            inntektgrenser shouldHaveSize 8
            inntektgrenser.filter { it.fomDato == fraDato } shouldHaveSize 8
            inntektgrenser.filter { it.tomDato == tomDato } shouldHaveSize 8
            val ensligGrenser = inntektgrenser.filter { it.forsorgerType == Sivilstandskode.ENSLIG }
            ensligGrenser shouldHaveSize 4
            ensligGrenser[0].antallBarn shouldBe 1
            ensligGrenser[1].antallBarn shouldBe 2
            ensligGrenser[2].antallBarn shouldBe 3
            ensligGrenser[3].antallBarn shouldBe 4

            // Valider beregning av inntektgrenser for 2023 basert på reele sjablonverdier
            ensligGrenser
                .hentForAntallBarn(1)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(297501), BigDecimal(468500)))
            ensligGrenser
                .hentForAntallBarn(2)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(297501), BigDecimal(534400)))
            ensligGrenser
                .hentForAntallBarn(3)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(297501), BigDecimal(534400)))
            ensligGrenser
                .hentForAntallBarn(4)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(297501), BigDecimal(534400)))

            ensligGrenser
                .hentForAntallBarn(1)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(468501), BigDecimal(534400)))
            ensligGrenser
                .hentForAntallBarn(2)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(534400), BigDecimal(534400)))
            ensligGrenser
                .hentForAntallBarn(3)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(534400), BigDecimal(534400)))
            ensligGrenser
                .hentForAntallBarn(3)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(534400), BigDecimal(534400)))

            val giftSamboerGrenser =
                inntektgrenser.filter { it.forsorgerType == Sivilstandskode.GIFT_SAMBOER }
            giftSamboerGrenser shouldHaveSize 4
            giftSamboerGrenser[0].antallBarn shouldBe 1
            giftSamboerGrenser[1].antallBarn shouldBe 2
            giftSamboerGrenser[2].antallBarn shouldBe 3
            giftSamboerGrenser[3].antallBarn shouldBe 4

            giftSamboerGrenser
                .hentForAntallBarn(1)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(297501), BigDecimal(360800)))
            giftSamboerGrenser
                .hentForAntallBarn(2)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(297501), BigDecimal(429900)))
            giftSamboerGrenser
                .hentForAntallBarn(3)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(297501), BigDecimal(499000)))
            giftSamboerGrenser
                .hentForAntallBarn(4)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(297501), BigDecimal(534400)))

            giftSamboerGrenser
                .hentForAntallBarn(1)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(360801), BigDecimal(534400)))
            giftSamboerGrenser
                .hentForAntallBarn(2)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(429901), BigDecimal(534400)))
            giftSamboerGrenser
                .hentForAntallBarn(3)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(499001), BigDecimal(534400)))
            giftSamboerGrenser
                .hentForAntallBarn(4)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(534400), BigDecimal(534400)))
        }
    }

    @Test
    fun `skal hente forskudd innteksgrenser for periode 2021-2023`() {
        val fraDato = LocalDate.parse("2020-07-01")
        val inntektgrenser =
            sjablongService.hentForskuddInntektgrensePerioder(fraDato)
        assertSoftly {
            inntektgrenser shouldHaveSize 8 * 5
            inntektgrenser.filter { it.tomDato == MAX_DATE } shouldHaveSize 8
            val inntekgrenser2020_2021 = inntektgrenser.hentForPeriodeHvorDatoErInkludert(LocalDate.parse("2021-01-02"))
            inntekgrenser2020_2021 shouldHaveSize 8
            inntekgrenser2020_2021[0].fomDato shouldBe fraDato
            inntekgrenser2020_2021[0].tomDato!! shouldBe LocalDate.parse("2021-06-30")

            val inntekgrenser2021_2022 = inntektgrenser.hentForPeriodeHvorDatoErInkludert(LocalDate.parse("2021-07-01"))
            inntekgrenser2021_2022 shouldHaveSize 8
            inntekgrenser2021_2022[0].fomDato shouldBe LocalDate.parse("2021-07-01")
            inntekgrenser2021_2022[0].tomDato!! shouldBe LocalDate.parse("2022-06-30")

            val inntekgrenser2023 = inntektgrenser.hentForPeriodeHvorDatoErInkludert(LocalDate.parse("2022-07-01"))
            inntekgrenser2023 shouldHaveSize 8
            inntekgrenser2023[0].fomDato shouldBe LocalDate.parse("2022-07-01")
            inntekgrenser2023[0].tomDato!! shouldBe LocalDate.parse("2023-06-30")

            val ensligGrenser = inntektgrenser.filter { it.forsorgerType == Sivilstandskode.ENSLIG }
            ensligGrenser shouldHaveSize 4 * 5

            // Valider beregning av inntektgrenser for 2022 basert på reele sjablonverdier
            val ensligPeriode2022 = ensligGrenser.hentForPeriodeHvorDatoErInkludert(LocalDate.parse("2022-04-01"))
            ensligPeriode2022
                .hentForAntallBarn(1)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(305001), BigDecimal(476600)))
            ensligPeriode2022
                .hentForAntallBarn(2)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(305001), BigDecimal(545000)))
            ensligPeriode2022
                .hentForAntallBarn(3)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(305001), BigDecimal(547200)))
            ensligPeriode2022
                .hentForAntallBarn(4)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(305001), BigDecimal(547200)))

            ensligPeriode2022
                .hentForAntallBarn(1)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(476601), BigDecimal(547200)))
            ensligPeriode2022
                .hentForAntallBarn(2)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(545001), BigDecimal(547200)))
            ensligPeriode2022
                .hentForAntallBarn(3)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(547200), BigDecimal(547200)))
            ensligPeriode2022
                .hentForAntallBarn(3)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(547200), BigDecimal(547200)))

            val giftSamboerGrenser =
                inntektgrenser.filter { it.forsorgerType == Sivilstandskode.GIFT_SAMBOER }
            giftSamboerGrenser shouldHaveSize 4 * 5
            val giftSamboerPeriode2022 =
                giftSamboerGrenser.hentForPeriodeHvorDatoErInkludert(LocalDate.parse("2022-04-01"))

            giftSamboerPeriode2022
                .hentForAntallBarn(1)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(305001), BigDecimal(366900)))
            giftSamboerPeriode2022
                .hentForAntallBarn(2)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(305001), BigDecimal(435300)))
            giftSamboerPeriode2022
                .hentForAntallBarn(3)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(305001), BigDecimal(503700)))
            giftSamboerPeriode2022
                .hentForAntallBarn(4)!!
                .validerBelop75Prosent(BeløpFraTil(BigDecimal(305001), BigDecimal(547200)))

            giftSamboerPeriode2022
                .hentForAntallBarn(1)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(366901), BigDecimal(547200)))
            giftSamboerPeriode2022
                .hentForAntallBarn(2)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(435301), BigDecimal(547200)))
            giftSamboerPeriode2022
                .hentForAntallBarn(3)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(503701), BigDecimal(547200)))
            giftSamboerPeriode2022
                .hentForAntallBarn(4)!!
                .validerBelop50Prosent(BeløpFraTil(BigDecimal(547200), BigDecimal(547200)))
        }
    }

    fun List<ForskuddInntektgrensePeriode>.hentForPeriodeHvorDatoErInkludert(dato: LocalDate) = this.filter { it.tomDato != null && it.tomDato!! >= dato && it.fomDato <= dato }

    fun List<ForskuddInntektgrensePeriode>.hentForAntallBarn(antallBarn: Int) = this.find { it.antallBarn == antallBarn }

    fun ForskuddInntektgrensePeriode.validerBelop50Prosent(belopFraTil: BeløpFraTil) {
        this.beløp50Prosent shouldBe belopFraTil
    }

    fun ForskuddInntektgrensePeriode.validerBelop75Prosent(belopFraTil: BeløpFraTil) {
        this.beløp75Prosent shouldBe belopFraTil
    }
}
