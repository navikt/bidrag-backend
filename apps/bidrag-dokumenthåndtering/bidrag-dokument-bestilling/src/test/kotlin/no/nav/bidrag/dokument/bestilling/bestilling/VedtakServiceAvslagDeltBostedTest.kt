package no.nav.bidrag.dokument.bestilling.bestilling

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.dokument.bestilling.tjenester.SjablongService
import no.nav.bidrag.dokument.bestilling.tjenester.VedtakService
import no.nav.bidrag.dokument.bestilling.utils.BARN2
import no.nav.bidrag.dokument.bestilling.utils.lagVedtaksdata
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.rolle.Rolletype
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Vedtaket er beregnet med testfilen barnebidragV2_avslag_flere_barn_delt_bosted.json i bidrag-beregn-barnebidrag.
 * BPs andel av U er under 50 % ved delt bosted, så beregningen gir avslag/opphør uten beløp i alle periodene.
 */
class VedtakServiceAvslagDeltBostedTest {
    private val barnIdent = BARN2.ident.verdi

    private val sjablongService =
        mockk<SjablongService> {
            every { hentInntektGrenseForPeriode(any()) } returns BigDecimal(100000)
            every { hentMaksInntektForPeriode(any()) } returns BigDecimal(1000000)
        }
    private val vedtakService = VedtakService(mockk(), sjablongService, mockk(), mockk())

    @Test
    fun `skal ta med grunnlaget for beregningen i perioder med avslag eller opphør ved delt bosted`() {
        val vedtak = lagVedtaksdata("vedtak/vedtak_response-bidrag-avslag-delt-bosted.json")

        val perioder = vedtakService.hentStønadsendringerForBarn(barnIdent, vedtak).single().vedtakPerioder

        perioder shouldHaveSize 2
        assertSoftly {
            perioder.forEach { periode ->
                periode.beløp.compareTo(BigDecimal.ZERO) shouldBe 0
                periode.bidragsevne.shouldBeNull()

                val andelUnderhold = periode.andelUnderhold.shouldNotBeNull()
                andelUnderhold.andelFaktor.shouldNotBeNull().compareTo(BigDecimal(40)) shouldBe 0
                (andelUnderhold.beløpUnderholdskostnad.shouldNotBeNull() > BigDecimal.ZERO) shouldBe true
                (andelUnderhold.inntektBP.shouldNotBeNull() > BigDecimal.ZERO) shouldBe true
                (andelUnderhold.inntektBM.shouldNotBeNull() > BigDecimal.ZERO) shouldBe true

                periode.underhold.shouldNotBeNull().delberegning.underholdskostnad shouldBe andelUnderhold.beløpUnderholdskostnad
                periode.samvær.shouldNotBeNull().samværsklasse shouldBe Samværsklasse.DELT_BOSTED

                periode.inntekter.shouldNotBeEmpty()
                periode.inntekter.filter { it.rolle == Rolletype.BIDRAGSPLIKTIG && it.periodeTotalinntekt == true }.shouldNotBeEmpty()
                periode.inntekter.filter { it.rolle == Rolletype.BIDRAGSMOTTAKER && it.periodeTotalinntekt == true }.shouldNotBeEmpty()
            }
        }
    }
}
