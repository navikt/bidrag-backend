package no.nav.bidrag.vedtak.util

import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettStønadsendringRequestDto
import no.nav.bidrag.vedtak.persistence.entity.Stønadsendring

@JvmName("ignorerAutomatiskOpphørAvOppfostringsbidragDto")
fun List<OpprettStønadsendringRequestDto>.ignorerAutomatiskOpphørAvOppfostringsbidrag(): List<OpprettStønadsendringRequestDto> {
    return filter {
        if (it.type == Stønadstype.OPPFOSTRINGSBIDRAG) {
            // Hvis det er automatisk opphør av oppfostringsbidrag så betyr det at det er fattet vedtak om bidrag samtidig
            // Da skal stønadsendringen ignorerer i videre sammenligning av vedtak
            val førstePeriode = it.periodeListe.firstOrNull() ?: return@filter true
            førstePeriode.resultatkode != "AutomatiskOpphør"
        } else {
            true
        }
    }
}

@JvmName("ignorerAutomatiskOpphørAvOppfostringsbidragEntity")
fun List<Stønadsendring>.ignorerAutomatiskOpphørAvOppfostringsbidrag(): List<Stønadsendring> = filter {
    if (it.type == Stønadstype.OPPFOSTRINGSBIDRAG.name) {
        // Hvis eneste stønadsendrng er oppfostringsbidrag så skal det inkluderes
        // Hvis det finnes stønadsendring som ikke er oppfostringsbidrag (Bidrag/Bidrag 18 år) så betyr det at det er automatisk opphør av bidrag
        // Da skal den stønadsendringen ignoreres
        this.none { it.type != Stønadstype.OPPFOSTRINGSBIDRAG.name }
    } else {
        true
    }
}
