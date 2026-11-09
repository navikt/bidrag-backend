package no.nav.bidrag.beregn.barnebidrag.service.orkestrering

import no.nav.bidrag.beregn.barnebidrag.unleash.BarnebidragUnleashFeatures
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultatV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidragV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnSluttberegningIReferanser
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt

internal const val FEILMELDING_SELVFORSØRGET_BARN =
    "Beregning av barnebidrag når barnet er selvforsørget (barnets inntekt er høyere enn 100 ganger forskuddssatsen) støttes ikke ennå."

/**
 * Midlertidig sperre: Det er ikke avklart om barnebidraget skal opphøre når barnet er selvforsørget (§ 5). Inntil det er avklart
 * feiler beregningen når en resultatperiode for et søknadsbarn er beregnet som selvforsørget, slik at det ikke kan fattes vedtak.
 * Avslag for full forpleining (§ 11 c) og delt bosted (§ 8) berøres ikke. Sperren styres av Unleash-bryteren
 * [BarnebidragUnleashFeatures.SPERR_SELVFORSØRGET_BARN].
 */
internal fun sperrBeregningNårBarnetErSelvforsørget(beregningResultatListe: List<BeregnetBarnebidragResultatV2>) {
    if (!BarnebidragUnleashFeatures.SPERR_SELVFORSØRGET_BARN.isEnabled) return

    val barnetErSelvforsørget = beregningResultatListe.any { resultat ->
        val grunnlagListe = resultat.beregnetBarnebidragResultat.grunnlagListe
        resultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe.any { periode ->
            grunnlagListe.finnSluttberegningIReferanser(periode.grunnlagsreferanseListe)
                ?.innholdTilObjekt<SluttberegningBarnebidragV2>()
                ?.barnetErSelvforsørget == true
        }
    }
    if (barnetErSelvforsørget) {
        throw IllegalArgumentException(FEILMELDING_SELVFORSØRGET_BARN)
    }
}
