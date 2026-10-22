package no.nav.bidrag.behandling.transformers.vedtak

import no.nav.bidrag.behandling.service.hentNyesteIdentIgnorerForbidden
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.gjelderRevurderingsbarn

val VedtakHendelse.erOpphørEllerInnkreving get() = listOf(Vedtakstype.OPPHØR, Vedtakstype.INNKREVING).contains(this.type)
val VedtakHendelse.stønadstype get() = this.stønadsendringListe?.firstOrNull()?.type
val VedtakHendelse.engangsbeløptype get() = this.engangsbeløpListe?.firstOrNull()?.type
val VedtakDto.innkrevingstype get() =
    if (stønadsendringListe.isNotEmpty()) {
        val stønadsendringerIkkeRevurdering =
            this.stønadsendringListe
                .filter { !gjelderRevurderingsbarn(it) }
        if (stønadsendringerIkkeRevurdering.isEmpty()) {
            val alleErRevurdering = this.stønadsendringListe.all { gjelderRevurderingsbarn(it) }
            if (alleErRevurdering) Innkrevingstype.UTEN_INNKREVING else Innkrevingstype.MED_INNKREVING
        } else {
            val finnesMedInnkreving =
                stønadsendringerIkkeRevurdering.any { it.innkreving == Innkrevingstype.MED_INNKREVING }
            if (finnesMedInnkreving) Innkrevingstype.MED_INNKREVING else Innkrevingstype.UTEN_INNKREVING
        }
    } else {
        this.engangsbeløpListe.firstOrNull()?.innkreving ?: Innkrevingstype.MED_INNKREVING
    }

fun VedtakDto.finnStønadsendringNyesteIdent(stønad: Stønadsid) = stønadsendringListe.find {
    hentNyesteIdentIgnorerForbidden(it.kravhaver.verdi) == hentNyesteIdentIgnorerForbidden(stønad.kravhaver.verdi) &&
        hentNyesteIdentIgnorerForbidden(it.skyldner.verdi) == hentNyesteIdentIgnorerForbidden(stønad.skyldner.verdi) &&
        it.type == stønad.type
}
// val VedtakDto.mottatDato get() = hent
