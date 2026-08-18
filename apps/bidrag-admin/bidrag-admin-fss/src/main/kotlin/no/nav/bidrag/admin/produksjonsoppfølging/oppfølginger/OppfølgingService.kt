package no.nav.bidrag.admin.produksjonsoppfølging.oppfølginger

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

private val LOGGER = KotlinLogging.logger { }

@Service
class OppfølgingService(
    private val oppfølginger: List<Oppfølging>,
    @param:Value($$"${NAIS_CLUSTER_NAME}") private val clusterName: String,
) {
    @Scheduled(fixedRateString = "5", timeUnit = TimeUnit.MINUTES)
    fun runScheduledCheck() {
        if (clusterName == "prod-fss") {
            oppfølginger.forEach { oppfølging ->
                val klassenavn = oppfølging::class.simpleName
                LOGGER.info { "Kjør oppfølging: $klassenavn" }
                try {
                    oppfølging.folgOpp()
                } catch (e: Exception) {
                    LOGGER.error(e) { "Feil ved kjøring av oppfølging $klassenavn" }
                }
            }
        } else {
            LOGGER.info { "Kjører ikke oppfølginger i cluster $clusterName" }
        }
    }
}
