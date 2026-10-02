package no.nav.bidrag.sak.util

import no.nav.bidrag.commons.util.IdentConsumer

fun IdentConsumer.sammePerson(første: String?, andre: String?): Boolean {
    if (første == null || andre == null) return false
    return første == andre || hentAlleIdenter(første).contains(andre)
}
