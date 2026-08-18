package no.nav.bidrag.sak.util

import no.nav.bidrag.domene.enums.behandling.SøknadGruppeKombinasjon
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype

fun SøknadGruppeKombinasjon.tilStønadstype(): Stønadstype? = when (this) {
    SøknadGruppeKombinasjon.BIDRAG,
    SøknadGruppeKombinasjon.BIDRAG_INNKREVING,
    SøknadGruppeKombinasjon.BIDRAG_TILLEGGSBIDRAG,
    SøknadGruppeKombinasjon.BIDRAG_TILLEGGSBIDRAG_INNKREVING,
    -> Stønadstype.BIDRAG

    SøknadGruppeKombinasjon.BIDRAG_18_ÅR,
    SøknadGruppeKombinasjon.BIDRAG_18_ÅR_INNKREVING,
    SøknadGruppeKombinasjon.BIDRAG_18_ÅR_TILLEGGSBIDRAG,
    SøknadGruppeKombinasjon.BIDRAG_18_AAR_TILLEGGSBIDRAG_INNKREVING,
    -> Stønadstype.BIDRAG18AAR

    SøknadGruppeKombinasjon.FORSKUDD -> Stønadstype.FORSKUDD

    SøknadGruppeKombinasjon.EKTEFELLEBIDRAG_UTEN_INNKREVING,
    SøknadGruppeKombinasjon.EKTEFELLEBIDRAG_MED_INNKREVING,
    -> Stønadstype.EKTEFELLEBIDRAG

    SøknadGruppeKombinasjon.MOTREGNING -> Stønadstype.MOTREGNING

    SøknadGruppeKombinasjon.OPPFOSTRINGSBIDRAG_INNKREVING -> Stønadstype.OPPFOSTRINGSBIDRAG

    else -> null
}

fun SøknadGruppeKombinasjon.tilEngangsbeløptype(): Engangsbeløptype? = when (this) {
    SøknadGruppeKombinasjon.DIREKTE_OPPGJØR -> Engangsbeløptype.DIREKTE_OPPGJØR

    SøknadGruppeKombinasjon.ETTERGIVELSE -> Engangsbeløptype.ETTERGIVELSE

    SøknadGruppeKombinasjon.GEBYR -> Engangsbeløptype.GEBYR_MOTTAKER

    SøknadGruppeKombinasjon.TILBAKEKREVING,
    SøknadGruppeKombinasjon.TILBAKEKREVING_ETTERGIVELSE,
    -> Engangsbeløptype.TILBAKEKREVING

    SøknadGruppeKombinasjon.SÆRBIDRAG,
    SøknadGruppeKombinasjon.SÆRBIDRAG_INNKREVING,
    -> Engangsbeløptype.SÆRBIDRAG

    else -> null
}
