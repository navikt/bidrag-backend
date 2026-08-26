package no.nav.bidrag.commons.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.variant.Variant

class UnleashFeaturesProvider(
    unleashProperties: UnleashProperties,
) {
    init {
        val config = generateUnleashConfig(unleashProperties)
        instance = DefaultUnleash(config)
    }

    companion object {
        private var instance: Unleash? = null

        private fun getInstance(): Unleash {
            checkNotNull(instance) { "Unleash instance not initialized. Application context may not be loaded yet." }
            return instance!!
        }

        fun isEnabled(
            feature: String,
            updateContext: Boolean = false,
        ): Boolean = isEnabled(feature, false, updateContext)

        fun getVariant(feature: String): Variant? =
            try {
                getInstance().getVariant(feature)
            } catch (e: Exception) {
                null
            }

        fun isEnabled(
            feature: String,
            defaultValue: Boolean,
            updateContext: Boolean = false,
        ): Boolean =
            try {
                if (updateContext) {
                    getInstance().isEnabled(feature, DefaultUnleashContextProvider.generateUnleashContext(), defaultValue)
                } else {
                    getInstance().isEnabled(feature, defaultValue)
                }
            } catch (e: Exception) {
                defaultValue
            }
    }
}
