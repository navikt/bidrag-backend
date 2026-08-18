package no.nav.bidrag.regnskap.aop

import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.CorrelationId.Companion.CORRELATION_ID_HEADER
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.*

@Component
@Aspect
class HendelseCorrelationAspect {

    companion object {
        private const val HENDELSE_KAFKA_POINTCUT = "execution(* no.nav.bidrag.regnskap.hendelse.kafka.*.*.*(..))"
        private const val FJERN_NORSKE_BOKSTAVER_REGEX = "[^A-Za-z0-9 ]"
    }

    @Before(value = HENDELSE_KAFKA_POINTCUT)
    fun opprettCorrelationId(joinPoint: JoinPoint) {
        if (MDC.get(CORRELATION_ID_HEADER) != null) return

        val correlationId = lagCorrelationIdFraMetodenavn(joinPoint.signature.name)
        MDC.put(CORRELATION_ID_HEADER, correlationId)
    }

    @After(value = HENDELSE_KAFKA_POINTCUT)
    fun fjernCorrelationId(joinPoint: JoinPoint) {
        MDC.clear()
    }

    private fun lagCorrelationIdFraMetodenavn(metodenavn: String): String {
        val rensetMetodenavn = metodenavn.replace(FJERN_NORSKE_BOKSTAVER_REGEX.toRegex(), "")
        val uuidPrefix = UUID.randomUUID().toString().substring(0, 8)
        return CorrelationId.existing("${uuidPrefix}_$rensetMetodenavn").get()
    }
}
