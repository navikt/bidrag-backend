package no.nav.bidrag.commons.unleash

import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import no.nav.bidrag.commons.web.MdcConstants
import org.slf4j.MDC

class DefaultUnleashContextProvider : UnleashContextProvider {
    companion object {
        fun generateUnleashContext(): UnleashContext =
            UnleashContext
                .builder()
                .userId(MDC.get("user"))
                .addProperty("saksnummer", MDC.get(MdcConstants.MDC_SAKSNUMMER))
                .addProperty("enhet", MDC.get(MdcConstants.MDC_ENHET))
                .appName(MDC.get("applicationKey"))
                .build()

        fun updateSaksnummer(saksummer: String) {
            MDC.put(MdcConstants.MDC_SAKSNUMMER, saksummer)
        }
    }

    override fun getContext(): UnleashContext = generateUnleashContext()
}
