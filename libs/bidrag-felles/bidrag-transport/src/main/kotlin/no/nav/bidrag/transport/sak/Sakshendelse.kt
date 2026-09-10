package no.nav.bidrag.transport.sak

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.sak.Saksnummer
import java.time.Instant

data class SakHendelse(
    val hendelseTidspunkt: Instant?, // todo: fjern nullable når vi har tømt Kafka-køen
    val saksnummer: Saksnummer,
    val hendelsestype: SakKafkaHendelsestype,
    val bidragspliktig: Personident? = null,
    val bidragsmottaker: Personident? = null,
    val barn: List<BarnISak> = emptyList(),
)

data class BarnISak(
    val ident: Personident? = null,
    val reellMottaker: ReellMottaker? = null,
)

enum class SakKafkaHendelsestype {
    ENDRING,
    OPPRETTELSE,
}
