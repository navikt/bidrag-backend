package no.nav.bidrag.beregn.barnebidrag.service.orkestrering

import no.nav.bidrag.beregn.barnebidrag.unleash.BarnebidragUnleashFeatures
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.sjablon.SjablonTallNavn
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultatV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.SjablonSjablontallPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidragV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnGrunnlagSomErReferertAv
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnSluttberegningIReferanser
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import java.math.BigDecimal

internal fun feilmeldingSelvforsørgetBarn(maksInntektBBMultiplikator: BigDecimal?) = "Beregning av barnebidrag når barnet er selvforsørget (barnets inntekt er minst " +
    "${maksInntektBBMultiplikator?.stripTrailingZeros()?.toPlainString() ?: SjablonTallNavn.MAKS_INNTEKT_BB_MULTIPLIKATOR.navn} " +
    "ganger forskuddssatsen) støttes ikke ennå."

/**
 * Midlertidig sperre: Det er ikke avklart om barnebidraget skal opphøre når barnet er selvforsørget (§ 5). Inntil det er avklart
 * feiler beregningen når en resultatperiode for et søknadsbarn er beregnet som selvforsørget, slik at det ikke kan fattes vedtak.
 * Avslag for full forpleining (§ 11 c) og delt bosted (§ 8) berøres ikke. Sperren styres av Unleash-bryteren
 * [BarnebidragUnleashFeatures.SPERR_SELVFORSØRGET_BARN].
 */
internal fun sperrBeregningNårBarnetErSelvforsørgetIfUnleashToggleIsOn(beregningResultatListe: List<BeregnetBarnebidragResultatV2>) {
    if (!BarnebidragUnleashFeatures.SPERR_SELVFORSØRGET_BARN.isEnabled) return

    beregningResultatListe.forEach { resultat ->
        val grunnlagListe = resultat.beregnetBarnebidragResultat.grunnlagListe
        resultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe.forEach { periode ->
            val sluttberegning = grunnlagListe.finnSluttberegningIReferanser(periode.grunnlagsreferanseListe) ?: return@forEach
            if (sluttberegning.innholdTilObjekt<SluttberegningBarnebidragV2>().barnetErSelvforsørget) {
                val maksInntektBBMultiplikator = grunnlagListe
                    .finnGrunnlagSomErReferertAv(Grunnlagstype.SJABLON_SJABLONTALL, sluttberegning)
                    .map { it.innholdTilObjekt<SjablonSjablontallPeriode>() }
                    .find { it.sjablon == SjablonTallNavn.MAKS_INNTEKT_BB_MULTIPLIKATOR }
                    ?.verdi
                throw IllegalArgumentException(feilmeldingSelvforsørgetBarn(maksInntektBBMultiplikator))
            }
        }
    }
}
