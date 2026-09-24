package no.nav.bidrag.arbeidsflyt.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.dto.DefaultOpprettOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.dto.PatchOppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.EndreOppgaveFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.OpprettOppgaveFeiletFunksjoneltException
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import no.nav.bidrag.commons.util.secureLogger

private const val OPPGAVE_CONTEXT = "/api/v1/oppgaver/"
private val LOGGER = KotlinLogging.logger { }

@Service
class OppgaveConsumer(
    @Value($$"${OPPGAVE_URL}") val url: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
    @Value($$"${retry.enabled:true}") val shouldRetry: Boolean,
) : AbstractRestClient(restTemplate, "oppgave") {

    private val baseUri
        get() =
            UriComponentsBuilder
                .fromUri(url)
                .path(OPPGAVE_CONTEXT)

    fun søkOppgaver(oppgaveSokRequest: OppgaveSokRequest): OppgaveSokResponse {
        val response = getForEntity<OppgaveSokResponse>(baseUri.queryParams(oppgaveSokRequest.tilMultiValueMap()).build().toUri())
        secureLogger.debug { "Response søk oppgave - ${initStringOf(response)}" }
        return response ?: OppgaveSokResponse(0)
    }

    fun hentOppgave(oppgaveId: Long): OppgaveData = try {
        getForNonNullEntity<OppgaveData>(
            baseUri.pathSegment(oppgaveId.toString()).build().toUri(),
        )
    } catch (e: HttpStatusCodeException) {
        if (e.statusCode == HttpStatus.NOT_FOUND) {
            throw EndreOppgaveFeiletFunksjoneltException("Fant ikke oppgave med id $oppgaveId. Feilet med feilmelding ${e.message}", e)
        }
        throw e
    }

    private fun initStringOf(oppgaveSokResponse: OppgaveSokResponse?): String {
        if (oppgaveSokResponse != null) {
            return "OppgaveSokResponse(antallTreff=${oppgaveSokResponse.antallTreffTotalt},oppgaver=[${oppgaveSokResponse.oppgaver}]"
        }

        return "no body, antall treff = 0"
    }

    fun endreOppgave(
        patchOppgaveRequest: PatchOppgaveRequest,
        endretAvEnhetsnummer: String? = null,
    ) {
        patchOppgaveRequest.endretAvEnhetsnr = endretAvEnhetsnummer

        try {
            val responseEntity =
                patchForEntity<OppgaveData>(
                    baseUri.pathSegment(patchOppgaveRequest.id.toString()).build().toUri(),
                    patchOppgaveRequest,
                )
            secureLogger.info{ "Endret oppgave ${patchOppgaveRequest.id}, fikk respons $responseEntity" }
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.BAD_REQUEST) {
                throw EndreOppgaveFeiletFunksjoneltException(
                    "Kunne ikke endre oppgave med id ${patchOppgaveRequest.id}. Feilet med feilmelding ${e.message}",
                    e,
                )
            }
            throw e
        }
    }

    fun opprettOppgave(opprettOppgaveRequest: DefaultOpprettOppgaveRequest): OppgaveData {
        try {
            val responseEntity =
                postForNonNullEntity<OppgaveData>(
                    baseUri.build().toUri(),
                    opprettOppgaveRequest,
                )
            LOGGER.info { "Opprettet oppgave ${responseEntity.id} med type ${opprettOppgaveRequest.oppgavetype} og journalpostId ${opprettOppgaveRequest.journalpostId}" }
            return responseEntity
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.BAD_REQUEST) {
                throw OpprettOppgaveFeiletFunksjoneltException(
                    "Kunne ikke opprette oppgave for journalpost ${opprettOppgaveRequest.journalpostId}. Feilet med feilmelding ${e.message}",
                    e,
                )
            }

            throw e
        }
    }
}
