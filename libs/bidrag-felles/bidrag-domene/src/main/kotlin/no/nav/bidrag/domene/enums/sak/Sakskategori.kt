@file:Suppress("unused")

package no.nav.bidrag.domene.enums.sak

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.bidrag.domene.enums.vedtak.Behandlingstype

enum class Sakskategori(
    private val beskrivelse: String,
    private val behandlingstypeForvaltning: String?,
    private val behandlingstypeKlage: String,
    private val behandlingstypeSøknad: String,
    val behandlingstype: String,
) {
    @JsonProperty("U")
    @JsonAlias("UTLAND")
    UTLAND("Utland", "ae0106", "ae0108", "ae0110", "ae0106"),

    @JsonProperty("N")
    @JsonAlias("NASJONAL")
    NASJONAL("Nasjonal", null, "ae0058", "ae0003", "ae0118"),

    ;

    override fun toString(): String = beskrivelse

    fun finnBehandlingstypekode(behandlingstype: Behandlingstype): String? =
        when (behandlingstype) {
            Behandlingstype.FORVALTNING -> this.behandlingstypeForvaltning
            Behandlingstype.KLAGE -> this.behandlingstypeKlage
            Behandlingstype.SØKNAD -> this.behandlingstypeSøknad
        }
}
