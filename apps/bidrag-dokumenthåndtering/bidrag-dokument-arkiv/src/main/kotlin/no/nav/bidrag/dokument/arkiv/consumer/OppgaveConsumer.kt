package no.nav.bidrag.dokument.arkiv.consumer

import no.nav.bidrag.dokument.arkiv.dto.OppgaveData
import no.nav.bidrag.dokument.arkiv.dto.OppgaveRequest
import no.nav.bidrag.dokument.arkiv.dto.OppgaveResponse
import no.nav.bidrag.dokument.arkiv.dto.OppgaveSokResponse
import no.nav.bidrag.dokument.arkiv.dto.OpprettOppgaveRequest
import no.nav.bidrag.dokument.arkiv.model.OppgaveSokParametre
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.client.patchForObject
import org.springframework.web.client.postForEntity

class OppgaveConsumer(restTemplate: RestTemplate?) : AbstractConsumer(restTemplate) {
    fun finnOppgaver(parametre: OppgaveSokParametre): OppgaveSokResponse? {
        val pathMedParametre = parametre.hentParametreForApneOppgaverSortertSynkendeEtterFrist()
        return restTemplate.exchange<OppgaveSokResponse>(
            pathMedParametre,
            HttpMethod.GET,
            null,
        ).body
    }

    fun opprett(opprettOppgaveRequest: OpprettOppgaveRequest): Long? {
        val oppgaveResponse =
            restTemplate.postForEntity<OppgaveResponse>("/", opprettOppgaveRequest)
        return oppgaveResponse.body?.id
    }

    fun patchOppgave(oppgavePatch: OppgaveRequest): OppgaveData? {
        return restTemplate.patchForObject<OppgaveData>(
            "/${oppgavePatch.id}",
            oppgavePatch,
        )
    }

    fun patchOppgaveWithVersionRetry(oppgavePatch: OppgaveRequest): OppgaveData? {
        try {
            return patchOppgave(oppgavePatch)
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                val oppgaveData = hentOppgave(oppgavePatch.id)!!
                oppgavePatch.versjon = oppgaveData.versjon
                return patchOppgave(oppgavePatch)
            }
            throw e
        }
    }

    fun hentOppgave(oppgaveId: Long): OppgaveData? {
        return restTemplate.exchange<OppgaveData>(
            "/$oppgaveId",
            HttpMethod.GET,
            null,
        ).body
    }
}
