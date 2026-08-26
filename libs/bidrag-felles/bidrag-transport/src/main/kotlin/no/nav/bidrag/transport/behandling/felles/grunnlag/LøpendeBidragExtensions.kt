package no.nav.bidrag.transport.behandling.felles.grunnlag

import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidrag
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragPeriodeResponse

fun LøpendeBidragPeriodeResponse.filtrerForPeriode(beregningsperiode: ÅrMånedsperiode): List<LøpendeBidrag> =
    // Fjerner perioder som ikke overlapper med beregningsperioden
    bidragListe.mapNotNull { bidrag ->
        val beregningsperiodeTil = beregningsperiode.til
        val periodeListe =
            bidrag.periodeListe
                .filter {
                    it.periode.overlapper(beregningsperiode) &&
                        it.periode.fom != beregningsperiode.til &&
                        it.periode.til != beregningsperiode.fom
                }.map { periode ->
                    // Justerer periode.til til beregningsperiode.til hvis til er null eller etter beregningsperiode.til
                    val periodeTil = periode.periode.til
                    val justerTil = beregningsperiodeTil != null && (periodeTil == null || periodeTil.isAfter(beregningsperiodeTil))

                    // Justerer periode.fom til beregningsperiode.fom hvis fom er før beregningsperiode.fom
                    val justerFom = periode.periode.fom.isBefore(beregningsperiode.fom)

                    if (justerFom || justerTil) {
                        val nyFom = if (justerFom) beregningsperiode.fom else periode.periode.fom
                        val nyTil = if (justerTil) beregningsperiodeTil else periodeTil
                        periode.copy(periode = periode.periode.copy(fom = nyFom, til = nyTil))
                    } else {
                        periode
                    }
                }
        if (periodeListe.isNotEmpty()) {
            LøpendeBidrag(
                sak = bidrag.sak,
                type = bidrag.type,
                kravhaver = bidrag.kravhaver,
                mottaker = bidrag.mottaker,
                periodeListe = periodeListe,
            )
        } else {
            null
        }
    }
