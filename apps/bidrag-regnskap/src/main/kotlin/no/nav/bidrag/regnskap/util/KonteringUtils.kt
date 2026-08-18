package no.nav.bidrag.regnskap.util

import no.nav.bidrag.domene.enums.regnskap.Søknadstype
import no.nav.bidrag.domene.enums.regnskap.Type
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import java.time.YearMonth

object KonteringUtils {

    fun vurderSøknadType(hendelse: Hendelse, indexPeriode: Int): String = if (hendelse.vedtakType == Vedtakstype.INDEKSREGULERING && indexPeriode == 0) {
        Søknadstype.IR.name
    } else if (hendelse.type == Engangsbeløptype.GEBYR_MOTTAKER.name) {
        Søknadstype.FABM.name
    } else if (hendelse.type == Engangsbeløptype.GEBYR_SKYLDNER.name) {
        Søknadstype.FABP.name
    } else {
        Søknadstype.EN.name
    }

    fun vurderSøknadType(vedtakType: String, stønadType: String, indexPeriode: Int, påløpskjøring: Boolean = false): String = if (vedtakType == Vedtakstype.INDEKSREGULERING.name && indexPeriode == 0) {
        Søknadstype.IR.name
    } else if (stønadType == Engangsbeløptype.GEBYR_MOTTAKER.name) {
        Søknadstype.FABM.name
    } else if (stønadType == Engangsbeløptype.GEBYR_SKYLDNER.name) {
        Søknadstype.FABP.name
    } else if (påløpskjøring) {
        Søknadstype.MP.name
    } else {
        Søknadstype.EN.name
    }

    fun vurderType(oppdragsperioder: List<Oppdragsperiode>, periode: YearMonth): String {
        if (manglerKonteringForPeriode(oppdragsperioder, periode)) {
            return Type.NY.name
        }
        return Type.ENDRING.name
    }

    private fun manglerKonteringForPeriode(oppdragsperioder: List<Oppdragsperiode>, periode: YearMonth): Boolean {
        val periodeString = periode.toString()

        return oppdragsperioder.none { oppdragsperiode ->
            val harKonteringForPeriode = oppdragsperiode.konteringer.any { kontering ->
                kontering.overføringsperiode == periodeString
            }
            harKonteringForPeriode
        }
    }
}
