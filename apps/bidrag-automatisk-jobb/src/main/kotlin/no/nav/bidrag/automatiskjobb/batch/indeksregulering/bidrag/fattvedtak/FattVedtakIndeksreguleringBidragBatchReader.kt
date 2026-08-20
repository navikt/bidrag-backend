package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.fattvedtak

import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.batch.core.listener.StepExecutionListener
import org.springframework.batch.core.step.StepExecution
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.data.domain.PageRequest
import java.time.Year

class FattVedtakIndeksreguleringBidragBatchReader(
    private val indeksreguleringRepository: IndeksreguleringRepository,
    private val pageSize: Int,
) : ItemReader<Indeksregulering>,
    StepExecutionListener {
    private var år: Int = Year.now().value
    private var sisteId: Int = 0
    private var side: Iterator<Indeksregulering> = emptyList<Indeksregulering>().iterator()

    override fun beforeStep(stepExecution: StepExecution) {
        år = stepExecution.jobParameters.getString("aar")?.toInt() ?: Year.now().value
        sisteId = 0
        side = emptyList<Indeksregulering>().iterator()
    }

    override fun read(): Indeksregulering? {
        if (!side.hasNext()) {
            side = hentNesteSide().iterator()
        }
        return if (side.hasNext()) side.next() else null
    }

    // Keyset-paginering (id > sisteId) i stedet for offset-paginering. Prosessoren endrer status bort
    // fra BEHANDLET for hver rad som behandles, og med offset-paginering ville treffmengden krympe
    // mellom hver side slik at halvparten av radene ble hoppet over.
    private fun hentNesteSide(): List<Indeksregulering> = indeksreguleringRepository
        .findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
            Status.BEHANDLET,
            Behandlingstype.FATTET_FORSLAG,
            år,
            listOf(Stønadstype.BIDRAG, Stønadstype.OPPFOSTRINGSBIDRAG, Stønadstype.BIDRAG18AAR),
            sisteId,
            PageRequest.of(0, pageSize),
        ).also { nesteSide ->
            nesteSide.lastOrNull()?.id?.let { sisteId = it }
        }
}
