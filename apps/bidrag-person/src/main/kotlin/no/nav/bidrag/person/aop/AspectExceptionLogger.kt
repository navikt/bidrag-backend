package no.nav.bidrag.person.aop

import no.nav.bidrag.commons.ExceptionLogger
import no.nav.bidrag.person.model.HttpStatusException
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

@Aspect
@Component
class AspectExceptionLogger(private val exceptionLogger: ExceptionLogger) {
    @AfterThrowing(pointcut = "within (no.nav.bidrag.person.controller.*)", throwing = "exception")
    fun logException(joinPoint: JoinPoint, exception: Exception) {
        if (exception is HttpStatusException) {
            return
        }
        exceptionLogger.logException(exception, joinPoint.sourceLocation.withinType.toString())
    }
}
