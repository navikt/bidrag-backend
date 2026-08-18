package no.nav.bidrag.aktoerregister.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.aktoerregister.exception.AktørNotFoundException
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.transport.samhandler.SamhandlerDto
import no.nav.bidrag.transport.samhandler.SamhandlerSøk
import no.nav.bidrag.transport.samhandler.SamhandlersøkeresultatDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private val LOGGER = KotlinLogging.logger {}

@Service
class SamhandlerConsumer(
    @param:Value($$"${BIDRAG_SAMHANDLER_URL}") val url: URI,
    @param:Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-aktoerregister-samhandler") {

    companion object {
        private const val SAMHANDLER_PATH = "/samhandler"
        private const val SAMHANDLER_SØK_PATH = "/samhandlersok"
    }

    fun hentSamhandler(aktørIdent: Ident): SamhandlerDto? {
        try {
            val response: SamhandlerDto? = postForEntity(leggTilPathPåUri(url, SAMHANDLER_PATH), aktørIdent)
            LOGGER.info { "Hentet samhandler med id: ${aktørIdent.verdi} fra bidrag-samhandler." }
            return response
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                throw AktørNotFoundException("Aktør ikke funnet i bidrag-samhandler")
            }
            throw e
        } catch (e: Exception) {
            LOGGER.error(e) { "Noe gikk galt i kallet mot bidrag-samhandler for ident: ${aktørIdent.verdi}. Svaret fra bidrag-samhandler var: ${e.message}" }
            throw e
        }
    }

    fun samhandlerSøk(samhandlerSøk: SamhandlerSøk): SamhandlersøkeresultatDto? {
        try {
            return postForEntity(leggTilPathPåUri(url, SAMHANDLER_SØK_PATH), samhandlerSøk)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                throw AktørNotFoundException("Samhandler ikke funnet i bidrag-samhandler")
            }
            throw e
        } catch (e: Exception) {
            LOGGER.error(e) { "Noe gikk galt i søket på samhandler mot bidrag-samhandler for ident: $samhandlerSøk. Svaret fra bidrag-samhandler var: ${e.message}" }
            throw e
        }
    }

    private fun leggTilPathPåUri(url: URI, path: String) = UriComponentsBuilder.fromUri(url)
        .path(path).build().toUri()
}
