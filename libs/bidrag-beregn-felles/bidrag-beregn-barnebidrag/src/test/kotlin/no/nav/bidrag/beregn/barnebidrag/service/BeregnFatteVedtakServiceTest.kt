package no.nav.bidrag.beregn.barnebidrag.service

import com.fasterxml.jackson.databind.node.POJONode
import io.kotest.matchers.shouldBe
import no.nav.bidrag.beregn.barnebidrag.service.beregning.BeregnFatteVedtakService
import no.nav.bidrag.beregn.core.service.BeregnService.BeregnGrunnlagJustert
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningAndelAvBidragsevne
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningFatteVedtak
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidragV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class BeregnFatteVedtakServiceTest {

    private val periode = ÅrMånedsperiode(YearMonth.of(2026, 1), null)
    private val sjekkEvneMotPeriode = YearMonth.of(2026, 10)
    private val søknadsbarn = "Person_Søknadsbarn_01"
    private val revurderingsbarn = "Person_Søknadsbarn_02"

    @Test
    fun `skal ikke fatte vedtak for revurderingsbarn når søknadsbarnet har avslag for delt bosted i siste periode`() {
        val resultat = fatteVedtakForRevurderingsbarn(
            sluttberegningSøknadsbarn = SluttberegningBarnebidragV2(
                periode = periode,
                beregnetBeløp = null,
                resultatBeløp = null,
                bidragIkkeBeregnetDeltBosted = true,
            ),
        )

        resultat.skalFatteVedtak shouldBe false
    }

    @Test
    fun `skal ikke fatte vedtak for revurderingsbarn når søknadsbarnet er selvforsørget i siste periode`() {
        val resultat = fatteVedtakForRevurderingsbarn(
            sluttberegningSøknadsbarn = SluttberegningBarnebidragV2(
                periode = periode,
                beregnetBeløp = null,
                resultatBeløp = null,
                barnetErSelvforsørget = true,
            ),
        )

        resultat.skalFatteVedtak shouldBe false
    }

    @Test
    fun `skal ikke fatte vedtak for revurderingsbarn når forpleiningen dekker U for søknadsbarnet i siste periode`() {
        val resultat = fatteVedtakForRevurderingsbarn(
            sluttberegningSøknadsbarn = SluttberegningBarnebidragV2(
                periode = periode,
                beregnetBeløp = null,
                resultatBeløp = null,
                forpleiningDekkerUnderholdskostnaden = true,
            ),
        )

        resultat.skalFatteVedtak shouldBe false
    }

    @Test
    fun `skal fatte vedtak for revurderingsbarn når søknadsbarnet har bidrag og BP mangler evne i siste periode`() {
        val resultat = fatteVedtakForRevurderingsbarn(
            sluttberegningSøknadsbarn = SluttberegningBarnebidragV2(
                periode = periode,
                beregnetBeløp = BigDecimal(1000),
                resultatBeløp = BigDecimal(1000),
            ),
        )

        resultat.skalFatteVedtak shouldBe true
    }

    private fun fatteVedtakForRevurderingsbarn(sluttberegningSøknadsbarn: SluttberegningBarnebidragV2) = BeregnFatteVedtakService
        .delberegningFatteVedtak(
            beregnGrunnlagGjeldendeBarn = grunnlagRevurderingsbarn(),
            beregnGrunnlagAlleBarnListe = listOf(grunnlagSøknadsbarn(sluttberegningSøknadsbarn), grunnlagRevurderingsbarn()),
            // Søknadsbarnet er markert uten full evne, slik at sjekken på avslag i sluttberegningen er det som avgjør
            grunnlagOpprinneligBeregningListe = listOf(
                grunnlag(
                    type = Grunnlagstype.DELBEREGNING_ANDEL_AV_BIDRAGSEVNE,
                    gjelderBarn = søknadsbarn,
                    innhold = DelberegningAndelAvBidragsevne(
                        periode = periode,
                        andelAvSumBidragTilFordelingFaktor = BigDecimal.ONE,
                        andelAvEvneBeløp = BigDecimal.ZERO,
                        bidragEtterFordeling = BigDecimal.ZERO,
                        harBPFullEvne = false,
                    ),
                ),
            ),
            sjekkEvneMotPeriode = sjekkEvneMotPeriode,
        )
        .first { it.type == Grunnlagstype.DELBEREGNING_FATTE_VEDTAK }
        .innholdTilObjekt<DelberegningFatteVedtak>()
        .fatteVedtakResultat

    private fun grunnlagSøknadsbarn(sluttberegning: SluttberegningBarnebidragV2) = BeregnGrunnlagJustert(
        beregnGrunnlag = BeregnGrunnlag(
            periode = periode,
            søknadsbarnReferanse = søknadsbarn,
            grunnlagListe = listOf(grunnlag(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG, søknadsbarn, sluttberegning)),
        ),
        åpenSluttperiode = true,
        erDelAvOpprinneligBehandling = true,
        virkningFraPeriode = periode.fom,
    )

    private fun grunnlagRevurderingsbarn() = BeregnGrunnlagJustert(
        beregnGrunnlag = BeregnGrunnlag(
            periode = periode,
            søknadsbarnReferanse = revurderingsbarn,
            grunnlagListe = listOf(
                grunnlag(
                    type = Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
                    gjelderBarn = revurderingsbarn,
                    innhold = SluttberegningBarnebidragV2(periode = periode, beregnetBeløp = BigDecimal(2000), resultatBeløp = BigDecimal(2000)),
                ),
            ),
        ),
        åpenSluttperiode = true,
        erDelAvOpprinneligBehandling = false,
        virkningFraPeriode = periode.fom,
    )

    private fun grunnlag(type: Grunnlagstype, gjelderBarn: String, innhold: Any) = GrunnlagDto(
        referanse = "${type}_$gjelderBarn",
        type = type,
        innhold = POJONode(innhold),
        gjelderBarnReferanse = gjelderBarn,
    )
}
