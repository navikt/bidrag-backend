package no.nav.bidrag.statistikk.hendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.statistikk.SECURE_LOGGER
import no.nav.bidrag.statistikk.service.BehandleHendelseService
import no.nav.bidrag.statistikk.service.JsonMapperService
import org.springframework.kafka.annotation.KafkaListener
private val LOGGER = KotlinLogging.logger {}

interface VedtakHendelseListener {
    fun lesHendelse(hendelse: String)
}

// sporingsdata fra hendelse-json
open class PojoVedtakHendelseListener(
    private val jsonMapperService: JsonMapperService,
    private val behandeHendelseService: BehandleHendelseService,
) : VedtakHendelseListener {
    override fun lesHendelse(hendelse: String) {
        try {
            val vedtakHendelse = jsonMapperService.mapHendelse(hendelse)
            behandeHendelseService.behandleHendelse(vedtakHendelse)
        } catch (e: Exception) {
            LOGGER.error(e) { "Behandling av vedtakshendelse feilet, se sikker logg for mer info" }
            SECURE_LOGGER.error(
                "Behandling av vedtakshendelse feilet for: $hendelse",
                e,
            )
            throw e
        }
    }
}

open class KafkaVedtakHendelseListener(jsonMapperService: JsonMapperService, behandeHendelseService: BehandleHendelseService) : PojoVedtakHendelseListener(jsonMapperService, behandeHendelseService) {
    @KafkaListener(groupId = "bidrag-statistikk-v22", topics = ["\${TOPIC_VEDTAK}"])
    override fun lesHendelse(hendelse: String) {
        super.lesHendelse(hendelse)
    }
}
