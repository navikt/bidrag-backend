package no.nav.bidrag.transport.sak

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer

data class SakHendelse(
    val saksnummer: Saksnummer,
    val hendelsestype: SakKafkaHendelsestype,
    val bidragspliktig: Personident? = null,
    val bidragsmottaker: Personident? = null,
    val barn: List<BarnISak> = emptyList(),
    val forrigeEierfogd: Enhetsnummer? = null,
)

data class BarnISak(
    val ident: Personident? = null,
    val reellMottaker: ReellMottaker? = null,
)

enum class SakKafkaHendelsestype {
    ENDRING,
    OPPRETTELSE,
}
