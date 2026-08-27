package no.nav.bidrag.commons.service.consumers

import no.nav.bidrag.transport.dokument.forsendelse.OpprettForsendelseForespørsel

interface FellesForsendelseConsumer {
    fun opprettForsendelse(opprettForsendelseForespørsel: OpprettForsendelseForespørsel): Long
}
