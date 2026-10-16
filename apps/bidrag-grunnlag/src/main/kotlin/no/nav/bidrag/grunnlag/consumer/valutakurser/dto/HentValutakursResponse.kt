package no.nav.bidrag.grunnlag.consumer.valutakurser.dto

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagKilde
import java.math.BigDecimal
import java.time.LocalDateTime

data class HentValutakursResponse(val hentetValutakursListe: List<HentetValutakursResultat>)

sealed interface HentetValutakursResultat {

    data class FeiledValutakurs(
        val periode: ÅrMånedsperiode,
        val basisvaluta: Valutakode,
        val kvoteringsvaluta: Valutakode,
    ) : HentetValutakursResultat

    data class HentetValutakurs(
        val periode: ÅrMånedsperiode,
        val valutakursSnitt: BigDecimal,
        val multiplikator: Int,
        val basisvaluta: Valutakode,
        val kvoteringsvaluta: Valutakode,
        val hentetTidspunkt: LocalDateTime,
        val kilde: ValutakursgrunnlagKilde,
    ) : HentetValutakursResultat
}
