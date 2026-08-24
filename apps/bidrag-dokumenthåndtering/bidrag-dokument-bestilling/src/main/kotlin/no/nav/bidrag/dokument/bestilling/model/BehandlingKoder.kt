package no.nav.bidrag.dokument.bestilling.model

import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype

enum class SoknadType(
    val kode: String,
) {
    ENDRING("EN"),
    ENDRING_MOTTAKER("EN"),
    EGET_TILTAK("ET"),
    SOKNAD("FA"),
    INNKREVINGSGRUNNL("IG"),
    INNKREVING("IK"),
    INDEKSREGULERING("IR"),
    KLAGE_BEGR_SATS("KB"),
    KLAGE("KL"),
    FOLGER_KLAGE("KM"),
    KONVERTERING("KV"),
    OMGJORING_BEGR_SATS("OB"),
    OPPJUST_FORSK("OF"),
    OPPHØR("OH"),
    OMGJORING("OM"),
    PRIVAT_AVTALE("PA"),
    BEGR_REVURD("RB"),
    REVURDERING("RF"),
    KORRIGERING("KR"),
    ;

    companion object {
        fun fromKode(kode: String): SoknadType? = entries.find { it.kode == kode }

        fun fromVedtakType(vedtakType: Vedtakstype): SoknadType =
            when (vedtakType) {
                Vedtakstype.INDEKSREGULERING -> INDEKSREGULERING

                Vedtakstype.FASTSETTELSE -> SOKNAD

                Vedtakstype.ENDRING, Vedtakstype.ENDRING_MOTTAKER -> ENDRING

                Vedtakstype.INNKREVING -> INNKREVINGSGRUNNL

                // Kan være INNKREVINGSGRUNNL, PRIVAT_AVTALE
                Vedtakstype.KLAGE -> KLAGE

                // Kan være KLAGE_BEGR_SATS, KLAGE, FOLGER_KLAGE
                Vedtakstype.REVURDERING -> REVURDERING

                // Kan være REVURDERING, BEGR_REVURD, EGET_TILTAK
                Vedtakstype.ALDERSOPPHØR, Vedtakstype.OPPHØR -> OPPHØR

                Vedtakstype.ALDERSJUSTERING -> OPPJUST_FORSK

                // Kan være EGET_TILTAK, OPPJUST_FORSK
                else -> ENDRING
            }
    }
}

enum class BehandlingType(
    val kode: String,
) {
    AVSKRIVNING("AV"),
    EKTEFELLEBIDRAG("EB"),
    BIDRAG_18_AR("18"),
    BIDRAG18AAR("18"),
    BIDRAG("BI"),
    BIDRAG_TILLEGGSBIDRAG("BT"),
    DIREKTE_OPPGJOR("DO"),
    ETTERGIVELSE("EG"),
    ERSTATNING("ER"),
    FARSKAP("FA"),
    FORSKUDD("FO"),
    GEBYR("GB"),
    INNKREVING("IK"),
    MOTREGNING("MR"),
    REFUSJON_BIDRAG("RB"),
    SAKSOMKOSTNINGER("SO"),
    SARTILSKUDD("ST"),
    SÆRBIDRAG("ST"),
    BIDRAG_18_AR_TILLEGGSBBI("T1"),
    TILLEGGSBIDRAG("TB"),
    TILBAKEKR_ETTERGIVELSE("TE"),
    TILBAKEKREVING("TK"),
    OPPFOSTRINGSBIDRAG("OB"),
    MORSKAP("MO"),
    KUNNSKAP_BIOLOGISK_FAR("FB"),
    BARNEBORTFORING("BF"),
    KONVERTERING("KV"),
    REISEKOSTNADER("RK"),
    ;

    companion object {
        fun fromKode(kode: String): BehandlingType? = BehandlingType.values().find { it.kode == kode }

        fun from(
            stonadType: Stønadstype?,
            engangsbelopType: Engangsbeløptype?,
        ): BehandlingType? =
            when (stonadType) {
                Stønadstype.FORSKUDD -> {
                    FORSKUDD
                }

                Stønadstype.BIDRAG -> {
                    BIDRAG
                }

                // Inneholder BIDRAG, BIDRAG_TILLEGGSBIDRAG, TILLEGGSBIDRAG
                Stønadstype.BIDRAG18AAR -> {
                    BIDRAG_18_AR
                }

                // Inneholder BIDRAG_18_AR_TILLEGGSBBI, BIDRAG_18_AR
                Stønadstype.EKTEFELLEBIDRAG -> {
                    EKTEFELLEBIDRAG
                }

                Stønadstype.MOTREGNING -> {
                    MOTREGNING
                }

                Stønadstype.OPPFOSTRINGSBIDRAG -> {
                    OPPFOSTRINGSBIDRAG
                }

                else -> {
                    when (engangsbelopType) {
                        Engangsbeløptype.SAERTILSKUDD, Engangsbeløptype.SÆRTILSKUDD, Engangsbeløptype.SÆRBIDRAG -> SARTILSKUDD
                        Engangsbeløptype.GEBYR_SKYLDNER, Engangsbeløptype.GEBYR_MOTTAKER -> GEBYR
                        Engangsbeløptype.ETTERGIVELSE -> ETTERGIVELSE
                        Engangsbeløptype.TILBAKEKREVING -> TILBAKEKREVING
                        Engangsbeløptype.ETTERGIVELSE_TILBAKEKREVING -> TILBAKEKR_ETTERGIVELSE
                        else -> null
                    }
                }
            }
    }
}
