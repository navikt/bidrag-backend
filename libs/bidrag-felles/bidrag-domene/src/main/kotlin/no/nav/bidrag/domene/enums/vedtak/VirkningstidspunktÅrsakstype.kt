@file:Suppress("unused")

package no.nav.bidrag.domene.enums.vedtak

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true, name = "Årsakstype")
enum class VirkningstidspunktÅrsakstype(
    vararg val legacyKode: String?,
    val stønadstype: Stønadstype? = null,
) {
    FRA_BARNETS_FØDSEL("A"),
    FRA_SAMLIVSBRUDD("B"),
    FRA_BARNETS_FLYTTEMÅNED("C"),
    FRA_MÅNEDEN_ETTER_FYLTE_18_ÅR("D", stønadstype = Stønadstype.BIDRAG18AAR),
    FRA_KRAVFREMSETTELSE("D"),
    TRE_MÅNEDER_TILBAKE("E", stønadstype = Stønadstype.FORSKUDD),
    FRA_SØKNADSTIDSPUNKT("E", "H", stønadstype = Stønadstype.BIDRAG),
    TRE_ÅRS_REGELEN("F"),
    FRA_OPPHOLDSTILLATELSE("G"),
    AUTOMATISK_JUSTERING("J"),
    FRA_SAMME_MÅNED_SOM_INNTEKTEN_BLE_REDUSERT("K", stønadstype = Stønadstype.FORSKUDD),
    FRA_MÅNED_ETTER_ENDRET_SØKNAD("K", stønadstype = Stønadstype.BIDRAG),
    FORHØYELSE_TILBAKE_I_TID("L", stønadstype = Stønadstype.BIDRAG),
    FRA_MÅNED_ETTER_INNTEKTEN_ØKTE("L", stønadstype = Stønadstype.FORSKUDD),
    SØKNADSTIDSPUNKT_ENDRING("M", stønadstype = Stønadstype.FORSKUDD),
    NEDSETTELSE_TILBAKE_I_TID("M", stønadstype = Stønadstype.BIDRAG),
    ENDRING_3_MÅNEDER_TILBAKE("N", stønadstype = Stønadstype.FORSKUDD),
    AVSLAG_FORHØYELSE_TILBAKE("N", stønadstype = Stønadstype.BIDRAG),
    ENDRING_3_ÅRS_REGELEN("O", stønadstype = Stønadstype.FORSKUDD),
    AVSLAG_NEDSETTELSE_TILBAKE("O", stønadstype = Stønadstype.BIDRAG),
    TIDLIGERE_FEILAKTIG_AVSLAG("P"),
    REVURDERING_MÅNEDEN_ETTER("Q"),
    ANNET("S", "U"),
    OMREGNING("R"),
    PRIVAT_AVTALE,
    FRA_MÅNEDEN_ETTER_I_PÅVENTE_AV_BIDRAGSSAK,
    FRA_MÅNEDEN_ETTER_PRIVAT_AVTALE,
    FRA_ENDRINGSTIDSPUNKT,
    BIDRAGSPLIKTIG_HAR_IKKE_BIDRATT_TIL_FORSØRGELSE,
    MÅNED_ETTER_BETALT_FORFALT_BIDRAG,
    ;

    companion object {
        fun fraLegacyKode(
            legacyKode: String,
            stønadstype: Stønadstype? = null,
        ): VirkningstidspunktÅrsakstype? =
            try {
                enumValues<VirkningstidspunktÅrsakstype>().find {
                    it.legacyKode.contains(legacyKode) &&
                        (stønadstype == null || it.stønadstype == stønadstype)
                } ?: enumValues<VirkningstidspunktÅrsakstype>().find {
                    it.legacyKode.contains(legacyKode)
                }
                    ?: VirkningstidspunktÅrsakstype.valueOf(
                        legacyKode,
                    )
            } catch (e: Exception) {
                null
            }
    }
}
