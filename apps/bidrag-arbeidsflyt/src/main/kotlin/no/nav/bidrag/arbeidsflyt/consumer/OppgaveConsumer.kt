package no.nav.bidrag.arbeidsflyt.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.dto.DefaultOpprettOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.MappeDto
import no.nav.bidrag.arbeidsflyt.dto.MapperResponse
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.dto.PatchOppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.EndreOppgaveFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.OpprettOppgaveFeiletFunksjoneltException
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

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

    fun hentÅpneOppgaverForSak(saksnummer: String): List<OppgaveData> {
        val request = OppgaveSokRequest().leggTilSaksreferanse(saksnummer).utenFagomrade()
        val oppgaver = mutableListOf<OppgaveData>()
        do {
            val response = getForNonNullEntity<OppgaveSokResponse>(baseUri.queryParams(request.medOffset(oppgaver.size).tilMultiValueMap()).build().toUri())
            check(response.oppgaver.isNotEmpty() || oppgaver.size >= response.antallTreffTotalt) {
                "Oppgavesøket returnerte en tom side før alle oppgavene var hentet"
            }
            oppgaver.addAll(response.oppgaver)
        } while (oppgaver.size < response.antallTreffTotalt)
        return oppgaver
    }

    fun hentMappe(mappeId: String): MappeDto = getForNonNullEntity(
        UriComponentsBuilder.fromUri(url).path("/api/v1/mapper/").pathSegment(mappeId).build().toUri(),
    )

    fun hentMapper(enhet: String, tema: String): List<MappeDto> {
        val mapper = mutableListOf<MappeDto>()
        do {
            val response = getForNonNullEntity<MapperResponse>(
                UriComponentsBuilder.fromUri(url)
                    .path("/api/v1/mapper")
                    .queryParam("enhetsnr", enhet)
                    .queryParam("tema", tema)
                    .queryParam("limit", 100)
                    .queryParam("offset", mapper.size)
                    .build().toUri(),
            )
            check(response.mapper.isNotEmpty() || mapper.size >= response.antallTreffTotalt) {
                "Mappesøket returnerte en tom side før alle mappene var hentet"
            }
            mapper.addAll(response.mapper)
        } while (mapper.size < response.antallTreffTotalt)
        return mapper
    }

    fun hentMappeNavn(mappeId: String?): String? {
        if (mappeId.isNullOrEmpty()) return null
        return try {
            hentMappe(mappeId).navn ?: mappeId
        } catch (e: Exception) {
            LOGGER.warn(e) { "Kunne ikke hente mappe med id $mappeId" }
            mappeId
        }
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
            secureLogger.info { "Endrer oppgave ${patchOppgaveRequest.id} med $patchOppgaveRequest" }
            patchForEntity<OppgaveData>(
                baseUri.pathSegment(patchOppgaveRequest.id.toString()).build().toUri(),
                patchOppgaveRequest,
            )
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
