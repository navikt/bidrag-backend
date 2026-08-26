package no.nav.bidrag.commons.service.consumers

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.dokument.forsendelse.ForsendelseConflictResponse
import no.nav.bidrag.transport.dokument.forsendelse.OpprettForsendelseForespørsel
import no.nav.bidrag.transport.dokument.forsendelse.OpprettForsendelseRespons
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class FellesForsendelseConsumerImpl(
    @Value("\${BIDRAG_DOKUMENT_FORSENDELSE_URL:\${FORSENDELSE_URL:\${BIDRAG_FORSENDELSE_URL}}}") private val url: URI,
    @Qualifier("azure") restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-dokument-forsendelse"),
    FellesForsendelseConsumer {
    private fun createUri(path: String = "") = URI.create("$url/$path")

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    override fun opprettForsendelse(opprettForsendelseForespørsel: OpprettForsendelseForespørsel): Long {
        try {
            val forsendelseResponse =
                postForNonNullEntity<OpprettForsendelseRespons>(
                    createUri("api/forsendelse"),
                    opprettForsendelseForespørsel,
                )
            return forsendelseResponse.forsendelseId!!
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                secureLogger.debug { "Forsendelse med referanse ${opprettForsendelseForespørsel.unikReferanse} finnes allerede. " }
                val resultat = e.getResponseBodyAs(ForsendelseConflictResponse::class.java)!!
                return resultat.forsendelseId
            } else {
                secureLogger.error(e) { "Feil ved oppretting av forsendelse med referanse ${opprettForsendelseForespørsel.unikReferanse}" }
                throw e
            }
        }
    }
}
