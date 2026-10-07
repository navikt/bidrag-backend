package no.nav.bidrag.sak.config

import io.getunleash.variant.Variant
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider

enum class UnleashFeatures(
    val featureName: String,
    defaultValue: Boolean,
) {
    TILGANG_TIL_AVSLUTTET_SAK("tilgang_til_avsluttet_sak", false),
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
