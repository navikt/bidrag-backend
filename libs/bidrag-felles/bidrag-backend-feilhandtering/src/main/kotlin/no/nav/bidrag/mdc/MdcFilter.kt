package no.nav.bidrag.mdc
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import java.util.function.Supplier

class MdcFilter(private val userNameSupplier: Supplier<String?>, private val appName: String) : OncePerRequestFilter() {
    private val idUtils = IdUtils()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            MdcHelper.appname = appName
            val callId = resolveCallId(request)
            MdcHelper.callId = callId

            val username = userNameSupplier.get()
            if (username != null) {
                MdcHelper.userIdent = username
            }

            filterChain.doFilter(request, response)
        } finally {
            MdcHelper.clear()
        }
    }

    private fun resolveCallId(httpServletRequest: HttpServletRequest): String = NAV_CALL_ID_HEADER_NAMES
        .mapNotNull { httpServletRequest.getHeader(it) }
        .firstOrNull { it.isNotEmpty() }
        ?: idUtils.generateId()

    companion object {
        // there is no consensus in NAV about header-names for correlation ids, so we support 'em all!
        // https://nav-it.slack.com/archives/C9UQ16AH4/p1538488785000100
        val NAV_CALL_ID_HEADER_NAMES =
            arrayOf(
                "Nav-Call-Id",
                "Nav-CallId",
                "Nav-Callid",
                "X-Correlation-Id",
            )
    }
}
