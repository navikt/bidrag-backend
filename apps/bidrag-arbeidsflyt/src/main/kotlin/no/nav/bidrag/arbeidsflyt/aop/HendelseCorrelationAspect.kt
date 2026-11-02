package no.nav.bidrag.arbeidsflyt.aop

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.model.CORRELATION_ID
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.util.sanitizeForLog
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
@Aspect
class HendelseCorrelationAspect {
    companion object {
        @JvmStatic
        private val LOGGER = KotlinLogging.logger {}
    }

    @Before(value = "execution(* no.nav.bidrag.arbeidsflyt.hendelse.KafkaDLQRetryScheduler.processMessages(..))")
    fun schedulerCorrelationIdToThread(joinPoint: JoinPoint) {
        val correlationId = CorrelationId.generateTimestamped("dl_kafka_scheduler").get()
        MDC.put(CORRELATION_ID, correlationId)
    }

    @Before(value = "execution(* no.nav.bidrag.arbeidsflyt.service.JsonMapperService.mapJournalpostHendelse(..)) && args(hendelse)")
    fun addCorrelationIdToThread(
        joinPoint: JoinPoint,
        hendelse: String,
    ) {
        try {
            val jsonNode = commonObjectmapper.readTree(hendelse)
            val correlationIdJsonNode = jsonNode["sporing"]?.get(CORRELATION_ID)

            if (correlationIdJsonNode == null) {
                val correlationId = CorrelationId.generateTimestamped("unknown").get()
                secureLogger.warn { "Unable to find correlation Id in '${hendelse.trim(' ').sanitizeForLog()}', using '$correlationId'" }
                MDC.put(CORRELATION_ID, correlationId)
            } else {
                val correlationId = CorrelationId.existing(correlationIdJsonNode.asText())
                MDC.put(CORRELATION_ID, correlationId.get())
            }
        } catch (e: Exception) {
            secureLogger.error(e) { "Unable to parse hendelse '${hendelse.sanitizeForLog()}'" }
        }
    }

    @Before(value = "execution(* no.nav.bidrag.arbeidsflyt.service.JsonMapperService.mapOppgaveHendelseV2(..)) && args(hendelse)")
    fun addCorrelationIdFromOppgaveHendelseToThread(
        joinPoint: JoinPoint,
        hendelse: String,
    ) {
        val correlationId = CorrelationId.generateTimestamped("oppgave")
        MDC.put(CORRELATION_ID, correlationId.get())
    }

    @After(value = "execution(* no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService.*(..))")
    fun clearCorrelationIdFromScheduler(joinPoint: JoinPoint) {
        MDC.clear()
    }

    @Before(value = "execution(* no.nav.bidrag.arbeidsflyt.hendelse.KafkaDLQRetryScheduler.*(..))")
    fun clearSchedulerCorrelationId(joinPoint: JoinPoint) {
        MDC.clear()
    }
}
