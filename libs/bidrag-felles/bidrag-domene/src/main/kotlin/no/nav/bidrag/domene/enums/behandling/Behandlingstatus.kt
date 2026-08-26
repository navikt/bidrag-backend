package no.nav.bidrag.domene.enums.behandling

import io.swagger.v3.oas.annotations.media.Schema

// Enum verdier av tabellen T_KODE_SOKN_STAT i bisys
@Schema(enumAsRef = true)
enum class Behandlingstatus(
    val bisysKode: String,
    val kreverOppgave: Boolean,
    val lukketStatus: Boolean,
) {
    // Lukket status
    DØMT_AVSLUTTET("DM", false, true),
    ERKJENT_AVSLUTTET("ER", false, true),
    AVVIST("AV", false, true),
    MIDLERTIDLIG_VEDTAK("MV", true, true),
    VEDTAK_FATTET_ETTER_MIDLERTIDLIG_VEDTAK("EV", false, true),
    UNNTAS_KLAGE("UK", false, true),
    G4("G4", false, true),
    SENDT_UTLANDET_LUKKET("SUL", false, true),

    // Under behandling status
    ERKLÆRING_SENDT_REKOMMANDERT("FS", true, false),
    FAR_UKJENT("FU", false, false),
    HJEMSENDT_TIL_TK("HT", true, false),
    INGEN_HENDELSE("IH", true, false),
    OVERFØRT_KLAGE_OG_ANKE("OF", true, false),
    OVERFØRT_RTV("OR", true, false),
    FOLKEHELSEINSTITUTTET("RI", true, false),
    SENDT_NAV_FARSKAPSENHET("SA", true, false),
    STEVNING_SENDT_TINGRETTEN("ST", true, false),
    SENDT_UTLANDET("SU", true, false),
    SENDT_UTENRIKSSTASJON("SUS", true, false),

    // Brukes av ny løsning
    FEILREGISTRERT("FR", false, true),
    TRUKKET("TR", false, true),
    UNDER_BEHANDLING("UB", true, false),
    VEDTAK_FATTET("VF", false, true),
    ;

    companion object {
        fun fraKode(kode: String): Behandlingstatus? =
            try {
                enumValues<Behandlingstatus>().find { res -> res.bisysKode == kode } ?: Behandlingstatus.valueOf(kode)
            } catch (e: Exception) {
                null
            }
    }
}
