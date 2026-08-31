package no.nav.bidrag.arbeidsflyt

import io.getunleash.variant.Variant
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider

enum class UnleashFeatures(
    val featureName: String,
    defaultValue: Boolean,
) {
    DEBUG_LOGGING("debug_logging", false),
    ;

    private var defaultValue = false

    init {
        this.defaultValue = defaultValue
    }

    val isEnabled: Boolean
        get() = UnleashFeaturesProvider.isEnabled(feature = featureName, defaultValue = defaultValue)

    val variant: Variant?
        get() = UnleashFeaturesProvider.getVariant(featureName)
}
