package no.nav.bidrag.organisasjon.consumer

import no.nav.bidrag.commons.web.CorrelationIdFilter
import org.springframework.http.HttpHeaders

fun initRequestHeaders(): HttpHeaders {
    val headers = HttpHeaders()
    headers.add(CorrelationIdFilter.CORRELATION_ID_HEADER, CorrelationIdFilter.fetchCorrelationIdForThread())
    return headers
}
