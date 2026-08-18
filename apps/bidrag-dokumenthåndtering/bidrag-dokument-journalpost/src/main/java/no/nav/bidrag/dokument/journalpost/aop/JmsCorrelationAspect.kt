package no.nav.bidrag.dokument.journalpost.aop

import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.dokument.journalpost.mq.BrevKvittering
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
@Aspect
@Suppress("unused")
class JmsCorrelationAspect {
    companion object {
        const val CORRELATION_ID = "correlationId"
    }

    @Suppress("ktlint:standard:max-line-length")
    @Before(
        value = "execution(* no.nav.bidrag.dokument.journalpost.consumer.mq.BrevserverKvitteringListener.receiveMessage(..)) && args(brevKvittering)",
    )
    fun addCorrelationIdToThreadBrevkvitteringQueue(
        joinPoint: JoinPoint,
        brevKvittering: BrevKvittering,
    ) {
        val correlationId = CorrelationId.generateTimestamped("brevkvittering_" + brevKvittering.brevRef)
        MDC.put(CORRELATION_ID, correlationId.get())
    }

    @After(value = "execution(* no.nav.bidrag.dokument.journalpost.consumer.mq.BrevserverKvitteringListener.*(..))")
    fun clearCorrelationIdFromBrevkvitteringQueue(joinPoint: JoinPoint) {
        MDC.clear()
    }
}
