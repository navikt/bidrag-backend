package no.nav.bidrag.domene.enums.behandling

import io.swagger.v3.oas.annotations.media.Schema

// Enum verdier av tabellen T_KODE_SOKN_GR_KOM i bisys
@Schema(enumAsRef = true)
enum class SøknadGruppeKombinasjon(
    val kode: String,
    val beskrivelse: String,
    val behandlingstema: Behandlingstema,
    val innkreving: Boolean,
) {
    AVSKRIVNING_DIREKTE_BETALT("AV", "Avskrivning direkte betalt", Behandlingstema.AVSKRIVNING, false),
    BIDRAG("B", "Bidrag", Behandlingstema.BIDRAG, false),
    BIDRAG_TILLEGGSBIDRAG_INNKREVING("BG", "Bidrag,tilleggsb,innkreving", Behandlingstema.BIDRAG_PLUSS_TILLEGGSBIDRAG, true),
    BIDRAG_INNKREVING("BI", "Bidrag,innkreving", Behandlingstema.BIDRAG, true),
    BIDRAG_TILLEGGSBIDRAG("BT", "Bidrag,tilleggsb", Behandlingstema.BIDRAG_PLUSS_TILLEGGSBIDRAG, false),
    DIREKTE_OPPGJØR("DO", "Direkte oppgjør", Behandlingstema.DIREKTE_OPPGJØR, false),
    EKTEFELLEBIDRAG_UTEN_INNKREVING("EB", "Ektefellebidrag u/innkr", Behandlingstema.EKTEFELLEBIDRAG, false),
    ETTERGIVELSE("EG", "Ettergivelse", Behandlingstema.ETTERGIVELSE, false),
    EKTEFELLEBIDRAG_MED_INNKREVING("EI", "Ektefellebidrag m/innkr", Behandlingstema.EKTEFELLEBIDRAG, true),
    ERSTATNING("ER", "Erstatning", Behandlingstema.ERSTATNING, false),
    FARSKAP("FA", "Farskap", Behandlingstema.FARSSKAP, false),
    KUNNSKAP_OM_BIOLOGISK_FAR("FB", "Kunnskap om biologisk far", Behandlingstema.KUNNSKAP_OM_BIOLOGISK_FAR, false),
    FORSKUDD("FO", "Forskudd", Behandlingstema.FORSKUDD, false),
    GEBYR("GB", "Gebyr", Behandlingstema.GEBYR, false),
    BIDRAG_18_ÅR_TILLEGGSBIDRAG("IG", "18 år, tilleggsb.", Behandlingstema.BIDRAG_18_ÅR_PLUSS_TILLEGGSBIDRAG, false),
    BIDRAG_18_ÅR_INNKREVING("II", "18 år, innkreving", Behandlingstema.BIDRAG_18_ÅR, true),
    INNKREVING("IK", "Innkreving", Behandlingstema.INNKREVING, true),
    SÆRBIDRAG_INNKREVING("IS", "Særtilskudd,innkreving", Behandlingstema.SÆRBIDRAG, true),
    BIDRAG_18_AAR_TILLEGGSBIDRAG_INNKREVING("IT", "18 år, tilleggsb,innkreving", Behandlingstema.BIDRAG_18_ÅR_PLUSS_TILLEGGSBIDRAG, true),
    MORSKAP("MO", "Morskap", Behandlingstema.MORSSKAP, false),
    MOTREGNING("MR", "Motregning", Behandlingstema.MOTREGNING, false),
    OPPFOSTRINGSBIDRAG_INNKREVING("OI", "Oppfostringsbidrag,innkreving", Behandlingstema.OPPFOSTRINGSBIDRAG, true),
    REFUSJON_BIDRAG("RB", "Refusjon bidrag", Behandlingstema.REFUSJON_BIDRAG, false),
    SAKSOMKOSTNINGER("SO", "Saksomkostninger", Behandlingstema.SAKSOMKOSTNINGER, false),
    SÆRBIDRAG("ST", "Særtilskudd", Behandlingstema.SÆRBIDRAG, false),
    TILLEGGSBIDRAG("TB", "Tilleggsbidrag", Behandlingstema.TILLEGGSBIDRAG, false),
    TILBAKEKREVING_ETTERGIVELSE("TE", "Tilbakekr,ettergivelse", Behandlingstema.TILBAKEKREVING_ETTERGIVELSE, false),
    TILLEGGSBIDRAG_INNKREVING("TI", "Tilleggsbidrag,innkreving", Behandlingstema.TILLEGGSBIDRAG, true),
    TILBAKEKREVING("TK", "Tilbakekreving", Behandlingstema.TILBAKEKREVING, false),
    BIDRAG_18_ÅR("18", "18 år", Behandlingstema.BIDRAG_18_ÅR, false),
    REISEKOSTNADER("RK", "Reisekostnader", Behandlingstema.REISEKOSTNADER, false),
    ;

    companion object {
        fun fraKode(kode: String): SøknadGruppeKombinasjon? =
            try {
                enumValues<SøknadGruppeKombinasjon>().find { it.kode == kode }
            } catch (e: Exception) {
                null
            }

        fun fraBehandlingstemaOgInnkreving(
            behandlingstema: Behandlingstema,
            medInnkreving: Boolean,
        ): SøknadGruppeKombinasjon? =
            enumValues<SøknadGruppeKombinasjon>().find {
                it.behandlingstema == behandlingstema && it.innkreving == medInnkreving
            }
    }
}
