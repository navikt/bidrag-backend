package no.nav.oppgave

import no.nav.bidrag.oppgaveapiclient.EksternOppgaveId
import no.nav.oppgave.model.FinnOppgaverParams
import no.nav.oppgave.model.OppgaveDto
import no.nav.oppgave.model.OpprettOppgaveRequest
import no.nav.oppgave.model.PatchOppgaveRequest
import no.nav.oppgave.model.SokOppgaverResponse
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

/**
 *  Client for Oppgave API. Hentet fra https://oppgave.intern.dev.nav.no/
 *
 *  Krever at header X-Correlation-ID settes i RestClient
 */

class OppgaveClient(
    private val restClient: RestClient,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun opprettOppgave(request: OpprettOppgaveRequest): OppgaveDto = restClient.post()
        .uri("/api/v1/oppgaver")
        .body(request)
        .retrieve()
        .body<OppgaveDto>()!!

    fun hentOppgave(oppgaveId: EksternOppgaveId): OppgaveDto? = restClient.get()
        .uri("/api/v1/oppgaver/{id}", oppgaveId)
        .retrieve()
        .onStatus({ status -> status.value() == 404 }) { _, _ -> }
        .body<OppgaveDto>()

    fun patchOppgave(oppgaveId: EksternOppgaveId, request: PatchOppgaveRequest): OppgaveDto = restClient.patch()
        .uri("/api/v1/oppgaver/{id}", oppgaveId)
        .body(request)
        .retrieve()
        .body<OppgaveDto>()!!

    fun finnOppgaver(params: FinnOppgaverParams): SokOppgaverResponse = restClient.get()
        .uri { uriBuilder ->
            val builder = uriBuilder.path("/api/v1/oppgaver")
            params.statuskategori?.let { builder.queryParam("statuskategori", it) }
            params.tema?.let { builder.queryParam("tema", *it.toTypedArray()) }
            params.oppgavetype?.let { builder.queryParam("oppgavetype", *it.toTypedArray()) }
            params.tildeltEnhetsnr?.let { builder.queryParam("tildeltEnhetsnr", it) }
            params.tilordnetRessurs?.let { builder.queryParam("tilordnetRessurs", it) }
            params.behandlingstema?.let { builder.queryParam("behandlingstema", it) }
            params.behandlingstype?.let { builder.queryParam("behandlingstype", it) }
            params.aktoerId?.let { builder.queryParam("aktoerId", *it.toTypedArray()) }
            params.journalpostId?.let { builder.queryParam("journalpostId", *it.toTypedArray()) }
            params.saksreferanse?.let { builder.queryParam("saksreferanse", *it.toTypedArray()) }
            params.ferdigstiltFom?.let { builder.queryParam("ferdigstiltFom", it) }
            params.ferdigstiltTom?.let { builder.queryParam("ferdigstiltTom", it) }
            params.orgnr?.let { builder.queryParam("orgnr", *it.toTypedArray()) }
            params.limit?.let { builder.queryParam("limit", it) }
            params.offset?.let { builder.queryParam("offset", it) }
            builder.build()
        }
        .retrieve()
        .body<SokOppgaverResponse>()!!
}
