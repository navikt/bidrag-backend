package no.nav.bidrag.aktoerregister.batch.samhandler

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.aktoerregister.batch.AktørBatchProcessorResult
import no.nav.bidrag.aktoerregister.batch.AktørStatus
import no.nav.bidrag.aktoerregister.exception.AktørNotFoundException
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.aktoerregister.service.AktørService
import no.nav.bidrag.domene.ident.Ident
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class SamhandlerBatchProcessor(
    private val aktørService: AktørService,
) : ItemProcessor<Aktør, AktørBatchProcessorResult> {

    override fun process(aktør: Aktør): AktørBatchProcessorResult? = try {
        aktørService.hentAktørFraSamhandler(Ident(aktør.aktørIdent))
            .takeIf { it != aktør }
            ?.let {
                AktørBatchProcessorResult(aktør, it, AktørStatus.UPDATED)
            }
    } catch (e: AktørNotFoundException) {
        LOGGER.warn(e) { "Samhandler: ${aktør.id} finnes ikke. Feilmelding: ${e.message}" }
        null
    } catch (e: Exception) {
        LOGGER.error(e) { "Samhandler: ${aktør.id} feilet i SamhandlerBatchProcessor. Feilmelding: ${e.message}" }
        null
    }
}
