package no.nav.bidrag.transport.organisasjon

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.bidrag.domene.enums.diverse.Tema
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.vedtak.Behandlingstype
import no.nav.bidrag.domene.ident.Personident

data class HentEnhetRequest(
    val ident: Personident,
    val biidenter: Set<Personident> = setOf(),
    val tema: String = Tema.TEMA_BIDRAG.verdi,
    @Deprecated("Bruk heller arbeidsfordeling")
    val behandlingstema: String? = null,
    val arbeidsfordeling: Arbeidsfordeling? = null,
    val sakskategori: Sakskategori? = null,
    val behandlingstype: Behandlingstype? = null,
) {
    @get:JsonIgnore
    val alleIdenter get() = biidenter + ident
}
