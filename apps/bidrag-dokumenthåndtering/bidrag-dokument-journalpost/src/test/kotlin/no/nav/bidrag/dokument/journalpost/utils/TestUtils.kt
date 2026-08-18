package no.nav.bidrag.dokument.journalpost.utils

import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.dokument.journalpost.dto.OppgaveData
import no.nav.bidrag.dokument.journalpost.entity.Journalpost
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.util.Objects
import java.util.Optional

internal fun prefixId(journalpost: Journalpost) = prefixId(journalpost.journalpostId)

internal fun prefixId(journalpostId: Int) = "BID-$journalpostId"

internal fun <T : Any> initHttpEntity(
    body: T?,
    vararg customHeaders: CustomHeader,
): HttpEntity<T> {
    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_JSON

    for (header in customHeaders) {
        headers.add(header.headerName, header.headerValue)
    }

    return HttpEntity(body, headers)
}

internal data class CustomHeader(
    val headerName: String,
    val headerValue: String,
)

internal fun hentMuligAdvarsel(headers: HttpHeaders) = Optional
    .ofNullable(headers[HttpHeaders.WARNING])
    .filter(Objects::nonNull)
    .map { list -> list.iterator().next() }

internal fun hentMuligAdvarseliUtenExceptionPrefix(headers: HttpHeaders) = hentMuligAdvarsel(headers)
    .map { streng: String? -> hentStrengUtenPrefixMedKolon(streng) }

private fun hentStrengUtenPrefixMedKolon(streng: String?): String? {
    if (streng == null || !streng.contains(":")) {
        return streng
    }

    return streng.substring(streng.indexOf(':') + 1).trim { it <= ' ' }
}

internal fun timestampCorrelationIdForThread(localCorrelationId: String) {
    val correlationId = CorrelationId.fetchCorrelationIdForThread()

    if (correlationId == null || correlationId.contains(localCorrelationId).not()) {
        CorrelationId.generateTimestamped(localCorrelationId)
    }
}

internal fun initOppgaveDataForSak(saksnummer: String) = OppgaveData(saksreferanse = saksnummer)
