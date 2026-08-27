package no.nav.bidrag.transport.behandling.felles.grunnlag

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class SluttberegningBarnebidragTest {
    private val sluttberegning =
        SluttberegningBarnebidrag(
            periode = ÅrMånedsperiode(LocalDate.now(), null),
            beregnetBeløp = BigDecimal.ONE,
            resultatBeløp = BigDecimal.ONE,
            uMinusNettoBarnetilleggBM = BigDecimal.ONE,
            bruttoBidragEtterBarnetilleggBM = BigDecimal.ONE,
            nettoBidragEtterBarnetilleggBM = BigDecimal.ONE,
            bruttoBidragJustertForEvneOg25Prosent = BigDecimal.ONE,
            bruttoBidragEtterBegrensetRevurdering = BigDecimal.ONE,
            bruttoBidragEtterBarnetilleggBP = BigDecimal.ONE,
            nettoBidragEtterSamværsfradrag = BigDecimal.ONE,
            bpAndelAvUVedDeltBostedFaktor = BigDecimal.ONE,
            bpAndelAvUVedDeltBostedBeløp = BigDecimal.ONE,
            barnetErSelvforsørget = false,
            bidragJustertForDeltBosted = false,
            bidragJustertForNettoBarnetilleggBP = false,
            bidragJustertForNettoBarnetilleggBM = false,
            bidragJustertNedTilEvne = false,
            bidragJustertNedTil25ProsentAvInntekt = false,
            bidragJustertTilForskuddssats = false,
            ikkeOmsorgForBarnet = false,
        )

    @Test
    fun `skal oversette til riktig bisys resultatkode`() {
        sluttberegning.bisysResultatkode shouldBe "KBB"

        sluttberegning
            .copy(
                barnetErSelvforsørget = true,
                bidragJustertForDeltBosted = true,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = true,
                bidragJustertNedTil25ProsentAvInntekt = true,
                bidragJustertForNettoBarnetilleggBM = true,
            ).bisysResultatkode shouldBe "5SF"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = true,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = true,
                bidragJustertNedTil25ProsentAvInntekt = true,
                bidragJustertForNettoBarnetilleggBM = true,
            ).bisysResultatkode shouldBe "6MB"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = true,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = true,
                bidragJustertForNettoBarnetilleggBM = true,
            ).bisysResultatkode shouldBe "7M"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = true,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = false,
                bidragJustertForNettoBarnetilleggBM = true,
            ).bisysResultatkode shouldBe "8DN"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = true,
                bidragJustertNedTilEvne = true,
                bidragJustertNedTil25ProsentAvInntekt = true,
                bidragJustertForNettoBarnetilleggBM = true,
            ).bisysResultatkode shouldBe "101"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = true,
                bidragJustertNedTil25ProsentAvInntekt = true,
                bidragJustertForNettoBarnetilleggBM = true,
            ).bisysResultatkode shouldBe "6MB"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = true,
                bidragJustertForNettoBarnetilleggBM = true,
            ).bisysResultatkode shouldBe "7M"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = false,
                bidragJustertForNettoBarnetilleggBM = true,
            ).bisysResultatkode shouldBe "102"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = false,
                bidragJustertForNettoBarnetilleggBM = false,
            ).bisysResultatkode shouldBe "KBB"

        sluttberegning
            .copy(
                bidragJustertTilForskuddssats = true,
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = false,
                bidragJustertForNettoBarnetilleggBM = false,
            ).bisysResultatkode shouldBe "RFO"

        sluttberegning
            .copy(
                bidragJustertTilForskuddssats = true,
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = true,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = false,
                bidragJustertForNettoBarnetilleggBM = false,
            ).bisysResultatkode shouldBe "101"

        sluttberegning
            .copy(
                bidragJustertTilForskuddssats = true,
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = true,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = false,
                bidragJustertForNettoBarnetilleggBM = false,
                ikkeOmsorgForBarnet = true,
            ).bisysResultatkode shouldBe "AIO"
    }

    @Test
    fun `skal oversette til riktig visningsnavn`() {
        sluttberegning.resultatVisningsnavn?.intern shouldBe "Kostnadsberegnet bidrag"

        sluttberegning
            .copy(
                barnetErSelvforsørget = true,
                bidragJustertForDeltBosted = true,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = true,
                bidragJustertNedTil25ProsentAvInntekt = true,
                bidragJustertForNettoBarnetilleggBM = true,
            ).resultatVisningsnavn
            ?.intern shouldBe "Barnet er selvforsørget"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = true,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = true,
                bidragJustertNedTil25ProsentAvInntekt = true,
                bidragJustertForNettoBarnetilleggBM = true,
            ).resultatVisningsnavn
            ?.intern shouldBe "Bidrag redusert til evne"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = true,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = true,
                bidragJustertForNettoBarnetilleggBM = true,
            ).resultatVisningsnavn
            ?.intern shouldBe "Bidrag redusert til 25 prosent av inntekt"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = true,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = false,
                bidragJustertForNettoBarnetilleggBM = true,
            ).resultatVisningsnavn
            ?.intern shouldBe "Bidrag justert for delt bosted"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = true,
                bidragJustertNedTilEvne = true,
                bidragJustertNedTil25ProsentAvInntekt = true,
                bidragJustertForNettoBarnetilleggBM = true,
            ).resultatVisningsnavn
            ?.intern shouldBe "Bidrag satt til barnetillegg fra BP"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = true,
                bidragJustertNedTil25ProsentAvInntekt = true,
                bidragJustertForNettoBarnetilleggBM = true,
            ).resultatVisningsnavn
            ?.intern shouldBe "Bidrag redusert til evne"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = true,
                bidragJustertForNettoBarnetilleggBM = true,
            ).resultatVisningsnavn
            ?.intern shouldBe "Bidrag redusert til 25 prosent av inntekt"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = false,
                bidragJustertForNettoBarnetilleggBM = true,
            ).resultatVisningsnavn
            ?.intern shouldBe "Bidrag satt til underholdskostnad minus barnetillegg BM"

        sluttberegning
            .copy(
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = false,
                bidragJustertForNettoBarnetilleggBM = false,
            ).resultatVisningsnavn
            ?.intern shouldBe "Kostnadsberegnet bidrag"

        sluttberegning
            .copy(
                bidragJustertTilForskuddssats = true,
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = false,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = false,
                bidragJustertForNettoBarnetilleggBM = false,
            ).resultatVisningsnavn
            ?.intern shouldBe "Bidrag justert til forskuddssats"

        sluttberegning
            .copy(
                bidragJustertTilForskuddssats = true,
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = true,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = false,
                bidragJustertForNettoBarnetilleggBM = false,
            ).resultatVisningsnavn
            ?.intern shouldBe "Bidrag satt til barnetillegg fra BP"

        sluttberegning
            .copy(
                bidragJustertTilForskuddssats = true,
                barnetErSelvforsørget = false,
                bidragJustertForDeltBosted = false,
                bidragJustertForNettoBarnetilleggBP = true,
                bidragJustertNedTilEvne = false,
                bidragJustertNedTil25ProsentAvInntekt = false,
                bidragJustertForNettoBarnetilleggBM = false,
                ikkeOmsorgForBarnet = true,
            ).resultatVisningsnavn
            ?.intern shouldBe "Ikke omsorg for barnet"
    }
}
