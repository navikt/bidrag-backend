package no.nav.bidrag.regnskap.hendelse.schedule.krav

import no.nav.bidrag.regnskap.service.KravService
import no.nav.bidrag.regnskap.service.PersistenceService
import org.springframework.stereotype.Component

@Component
class KravSchedulerUtils(
    private val kravService: KravService,
    private val persistenceService: PersistenceService,
) {

    fun erVedlikeholdsmodusPåslått(): Boolean = kravService.erVedlikeholdsmodusPåslått()

    fun harAktivtDriftsavvik(erInnlesing: Boolean = false): Boolean = persistenceService.harAktivtDriftsavvik(erInnlesing)
}
