package no.nav.bidrag.commons.service.forsendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.service.consumers.FellesForsendelseConsumer
import no.nav.bidrag.commons.service.consumers.FellesForsendelseConsumerImpl
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
@Import(FellesForsendelseMapper::class, FellesForsendelseConsumerImpl::class)
class FellesForsendelseService(
    private val forsendelseConsumer: FellesForsendelseConsumer,
    private val forsendelseMapper: FellesForsendelseMapper,
) {
    fun opprettForsendelse(
        forsendelseBestilling: FellesForsendelseBestilling,
        distribuerAutomatisk: Boolean = false,
    ): Long {
        val request = forsendelseMapper.tilOpprettForsendelseRequest(forsendelseBestilling, distribuerAutomatisk)
        try {
            val forsendelseId = forsendelseConsumer.opprettForsendelse(request)

            log.debug {
                "Opprettet forsendelse $forsendelseId for bestilling $forsendelseBestilling"
            }
            return forsendelseId
        } catch (e: Exception) {
            log.error(e) {
                "Feil ved opprettelse av forsendelse for bestilling $forsendelseBestilling"
            }
            opprettForsendelseFeilet("Feil ved opprettelse av forsendelse for bestilling $forsendelseBestilling")
        }
    }
}
