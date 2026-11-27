package no.nav.bidrag.behandling.transformers.beregning

import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.dto.v2.validering.BeregningValideringsfeil
import no.nav.bidrag.behandling.dto.v2.validering.SærbidragRimeligÅInnvilgeValideringsfeil
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.transport.behandling.beregning.særbidrag.BeregnetSærbidragResultat
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragspliktigesAndel
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.nio.charset.Charset

/**
 * Barnets inntekt er lik eller høyere enn 100 ganger forskuddssats. Beregningen gir da avslag fordi barnet er selvforsørget,
 * med mindre saksbehandler har vurdert at det er rimelig å innvilge særbidrag. Grunnlaget for vurderingen er bare med i
 * resultatet når vurderingen er brukt i beregningen av BPs andel.
 */
fun BeregnetSærbidragResultat.barnetsInntektErOverGrensenForSelvforsørget(): Boolean {
    val grunnlagsliste = grunnlagListe.toList()
    return grunnlagsliste.any { it.type == Grunnlagstype.SÆRBIDRAG_RIMELIG_Å_INNVILGE } ||
        grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningBidragspliktigesAndel>(Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL)
            .any { it.innhold.barnetErSelvforsørget }
}

private val htmlTagg = Regex("<[^>]*>")

/**
 * Begrunnelsen kommer som HTML fra tekstredigeringen, for eksempel `<p><br/></p>` eller `<p>&nbsp;</p>` når den bare har
 * tomme linjer eller mellomrom. Begrunnelsen mangler når den ikke har synlig tekst.
 */
fun String?.manglerSynligTekst(): Boolean = this == null ||
    replace(htmlTagg, " ")
        .replace("&nbsp;", " ")
        .replace(' ', ' ')
        .isBlank()

/**
 * Saksbehandler må alltid begrunne vurderingen av om det er rimelig å innvilge særbidrag når barnets inntekt er lik eller høyere
 * enn 100 ganger forskuddssats.
 */
fun Behandling.validerBegrunnelseRimeligÅInnvilge(resultat: BeregnetSærbidragResultat) {
    if (!resultat.barnetsInntektErOverGrensenForSelvforsørget() || !utgift?.rimeligÅInnvilgeBegrunnelse.manglerSynligTekst()) return

    val feil = BeregningValideringsfeil(særbidragRimeligÅInnvilge = SærbidragRimeligÅInnvilgeValideringsfeil(manglerBegrunnelse = true))
    secureLogger.warn {
        "Feil ved validering av behandling $id for beregning av særbidrag: " + commonObjectmapper.writeValueAsString(feil)
    }
    throw HttpClientErrorException(
        HttpStatus.BAD_REQUEST,
        "Feil ved validering av behandling for beregning av særbidrag",
        commonObjectmapper.writeValueAsBytes(feil),
        Charset.defaultCharset(),
    )
}
