package no.nav.bidrag.regnskap

import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider

enum class UnleashFeatures(val featureName: String) {
    ENDRE_MOTTAKER("regnskap.endre_mottaker"),
    ;

    val isEnabled: Boolean
        get() = UnleashFeaturesProvider.isEnabled(feature = featureName, defaultValue = false)
}
