package no.nav.bidrag.aktoerregister.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.aktoerregister.exception.AktørNotFoundException
import no.nav.bidrag.commons.util.secureLogger
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
            return response
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                throw AktørNotFoundException("Aktør ikke funnet i bidrag-samhandler")
            }
            LOGGER.error { "Kall mot bidrag-samhandler feilet. operasjon=hentSamhandler, httpStatus=${e.statusCode.value()}" }
            secureLogger.error(e) { "Kall mot bidrag-samhandler feilet for aktørIdent=${aktørIdent.verdi}" }
            throw e
        } catch (e: Exception) {
            LOGGER.error { "Kall mot bidrag-samhandler feilet. operasjon=hentSamhandler" }
            secureLogger.error(e) { "Kall mot bidrag-samhandler feilet for aktørIdent=${aktørIdent.verdi}" }
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
            LOGGER.error { "Samhandlersøk mot bidrag-samhandler feilet. operasjon=samhandlerSøk, httpStatus=${e.statusCode.value()}" }
            secureLogger.error(e) { "Samhandlersøk mot bidrag-samhandler feilet. søk=$samhandlerSøk" }
            throw e
        } catch (e: Exception) {
            LOGGER.error { "Samhandlersøk mot bidrag-samhandler feilet. operasjon=samhandlerSøk" }
            secureLogger.error(e) { "Samhandlersøk mot bidrag-samhandler feilet. søk=$samhandlerSøk" }
            throw e
        }
    }

    private fun leggTilPathPåUri(url: URI, path: String) = UriComponentsBuilder.fromUri(url)
        .path(path).build().toUri()
}
