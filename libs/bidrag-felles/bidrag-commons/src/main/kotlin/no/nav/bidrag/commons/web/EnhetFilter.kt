package no.nav.bidrag.commons.web

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.bidrag.commons.util.sanitizeForLog
import org.slf4j.MDC

private val LOGGER = KotlinLogging.logger {}

class EnhetFilter : Filter {

    override fun doFilter(
        servletRequest: ServletRequest,
        servletResponse: ServletResponse,
        filterChain: FilterChain,
    ) {
        if (servletRequest is HttpServletRequest) {
            val requestURI = servletRequest.requestURI
            if (isNotRequestToActuatorEndpoint(requestURI)) {
                val enhetsnummer = servletRequest.getHeader(X_ENHET_HEADER)
                if (enhetsnummer != null) {
                    ENHETSNUMMER_VALUE.set(enhetsnummer)
                    MDC.put(ENHET_MDC, enhetsnummer)
                    (servletResponse as HttpServletResponse).addHeader(X_ENHET_HEADER, enhetsnummer)
                    LOGGER.debug { "Behandler request '${requestURI.sanitizeForLog()}' for enhet med enhetsnummer ${enhetsnummer.sanitizeForLog()}" }
                } else {
                    ENHETSNUMMER_VALUE.set(null)
                    LOGGER.debug { "Behandler request '${requestURI.sanitizeForLog()}' uten informasjon om enhetsnummer." }
                }
            }
        } else {
            val filterRequest = servletRequest.javaClass.simpleName
            LOGGER.error { "Filtrering gjøres ikke av en HttpServletRequest: ${filterRequest.sanitizeForLog()}" }
        }
        filterChain.doFilter(servletRequest, servletResponse)
        MDC.clear()
    }

    private fun isNotRequestToActuatorEndpoint(requestURI: String?): Boolean {
        checkNotNull(requestURI) { "should only use this class in an web environment which receives requestUri!!!" }
        return !requestURI.contains("/actuator/")
    }

    companion object {
        private val ENHETSNUMMER_VALUE = ThreadLocal<String>()
        private const val ENHET_MDC = "enhet"
        const val X_ENHET_HEADER = "X-Enhet"

        @JvmStatic
        fun fetchForThread(): String = ENHETSNUMMER_VALUE.get() ?: ""
    }
}
