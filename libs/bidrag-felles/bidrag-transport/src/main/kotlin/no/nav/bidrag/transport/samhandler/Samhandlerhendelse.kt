package no.nav.bidrag.transport.samhandler

data class Samhandlerhendelse(
    val samhandlerId: String,
    val hendelsestype: SamhandlerKafkaHendelsestype,
    val sporingId: String,
)

enum class SamhandlerKafkaHendelsestype {
    OPPRETTET,
    OPPDATERT,
    OPPHØRT,
}
