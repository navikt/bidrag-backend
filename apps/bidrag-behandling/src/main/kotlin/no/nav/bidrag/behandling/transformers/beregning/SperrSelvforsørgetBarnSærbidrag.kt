package no.nav.bidrag.behandling.transformers.beregning

import no.nav.bidrag.behandling.config.UnleashFeatures
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.transport.behandling.beregning.særbidrag.BeregnetSærbidragResultat
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragspliktigesAndel
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse

const val FEILMELDING_SELVFORSØRGET_BARN_SÆRBIDRAG =
    "Beregning av særbidrag når barnet er selvforsørget (barnets inntekt er høyere enn 100 ganger forskuddssatsen) støttes ikke ennå."

/**
 * Midlertidig sperre: Løsningen for særbidrag når barnet er selvforsørget (§ 5) er ikke i henhold til lovverket. Inntil det er
 * avklart feiler beregningen når BPs andel er beregnet med selvforsørget barn, slik at det ikke kan fattes vedtak. Dette gjelder også
 * når resultatet er [no.nav.bidrag.domene.enums.beregning.Resultatkode.SÆRBIDRAG_IKKE_FULL_BIDRAGSEVNE]. Sperren styres av
 * Unleash-toggle [UnleashFeatures.SPERR_SELVFORSØRGET_BARN_SÆRBIDRAG].
 */
fun sperrSærbidragNårBarnetErSelvforsørgetIfUnleashToggleIsOn(resultat: BeregnetSærbidragResultat) {
    if (!UnleashFeatures.SPERR_SELVFORSØRGET_BARN_SÆRBIDRAG.isEnabled) return

    val barnetErSelvforsørget =
        resultat.grunnlagListe
            .toList()
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningBidragspliktigesAndel>(Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL)
            .any { it.innhold.barnetErSelvforsørget }
    if (barnetErSelvforsørget) {
        throw IllegalArgumentException(FEILMELDING_SELVFORSØRGET_BARN_SÆRBIDRAG)
    }
}
