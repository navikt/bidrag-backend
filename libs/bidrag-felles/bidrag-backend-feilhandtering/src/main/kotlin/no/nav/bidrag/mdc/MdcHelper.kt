package no.nav.bidrag.mdc

import no.nav.bidrag.mdc.MdcConstants.MDC_APP_NAME
import no.nav.bidrag.mdc.MdcConstants.MDC_CALL_ID
import no.nav.bidrag.mdc.MdcConstants.MDC_USER_ID
import org.slf4j.MDC

internal object MdcHelper {

    var callId: String?
        get() = MDC.get(MDC_CALL_ID)
        set(value) = MDC.put(MDC_CALL_ID, value)

    var userIdent: String?
        get() = MDC.get(MDC_USER_ID)
        set(value) = MDC.put(MDC_USER_ID, value)
    var appname: String?
        get() = MDC.get(MDC_APP_NAME)
        set(value) = MDC.put(MDC_APP_NAME, value)

    fun clear() {
        MDC.remove(MDC_CALL_ID)
        MDC.remove(MDC_USER_ID)
        MDC.remove(MDC_APP_NAME)
    }
}
