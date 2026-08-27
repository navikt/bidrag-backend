package no.nav.bidrag.commons.web

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.web.BidragHttpHeaders.X_ENHET
import no.nav.bidrag.commons.web.BidragHttpHeaders.X_SAKSNUMMER
import no.nav.bidrag.commons.web.MdcConstants.MDC_APP_NAME
import no.nav.bidrag.commons.web.MdcConstants.MDC_ENHET
import no.nav.bidrag.commons.web.MdcConstants.MDC_SAKSNUMMER
import no.nav.bidrag.commons.web.MdcConstants.MDC_USER_ID
import org.slf4j.MDC
import org.springframework.stereotype.Component

/**
 * Adds user to MDC for logging
 */
@Component
class UserMdcFilter : Filter {
    override fun doFilter(
        servletRequest: ServletRequest,
        servletResponse: ServletResponse,
        filterChain: FilterChain,
    ) {
        val user = TokenUtils.hentBruker()
        val appName = TokenUtils.hentApplikasjonsnavn()
        user?.apply {
            MDC.put(USER_MDC, user)
            MDC.put(MDC_USER_ID, user)
        }
        appName?.apply {
            MDC.put(APP_NAME_MDC, appName)
            MDC.put(MDC_APP_NAME, appName)
        }
        hentEnhetHeader(servletRequest)?.let {
            MDC.put(MDC_ENHET, it)
        }
        hentSaksnummerHeader(servletRequest)?.let {
            MDC.put(MDC_SAKSNUMMER, it)
        }
        try {
            filterChain.doFilter(servletRequest, servletResponse)
        } finally {
            MDC.clear()
        }
    }

    fun hentSaksnummerHeader(httpRequest: ServletRequest): String? {
        if (httpRequest !is HttpServletRequest) {
            return null
        }
        return httpRequest.getHeader(X_SAKSNUMMER) ?: httpRequest.getHeader("X-saksnummer")
    }

    fun hentEnhetHeader(httpRequest: ServletRequest): String? {
        if (httpRequest !is HttpServletRequest) {
            return null
        }
        return httpRequest.getHeader(X_ENHET) ?: httpRequest.getHeader("X-enhet")
    }

    companion object {
        private const val USER_MDC = "user"
        private const val APP_NAME_MDC = "applicationKey"
    }
}
