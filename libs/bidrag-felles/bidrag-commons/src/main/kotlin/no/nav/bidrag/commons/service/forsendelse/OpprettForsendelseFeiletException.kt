package no.nav.bidrag.commons.service.forsendelse

class OpprettForsendelseFeiletException(
    melding: String,
) : IllegalStateException(melding)

fun opprettForsendelseFeilet(melding: String): Nothing = throw OpprettForsendelseFeiletException(melding)
