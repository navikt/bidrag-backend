package no.nav.bidrag.commons.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import no.nav.bidrag.commons.unleash.FellesUnleashFeatures

class BidragDebugLogFilter : Filter<ILoggingEvent>() {
    private fun logPackage(event: ILoggingEvent): Boolean {
        val variant = FellesUnleashFeatures.DEBUG_LOGGING.variant ?: return true
        val variantPayload = variant.payload
        if (!variantPayload.isEmpty && variant.name == "package_name" && variantPayload.get().type == "string") {
            val logPackages = variantPayload.get().value ?: return true
            val loggerName = if (event.loggerName == "secureLogger") event.callerData[0].className else event.loggerName
            return loggerName.contains(logPackages)
        }
        return true
    }

    override fun decide(event: ILoggingEvent): FilterReply =
        if (event.level == Level.DEBUG) {
            if (FellesUnleashFeatures.DEBUG_LOGGING.isEnabled && logPackage(event)) {
                FilterReply.NEUTRAL
            } else {
                FilterReply.DENY
            }
        } else {
            FilterReply.NEUTRAL
        }
}
