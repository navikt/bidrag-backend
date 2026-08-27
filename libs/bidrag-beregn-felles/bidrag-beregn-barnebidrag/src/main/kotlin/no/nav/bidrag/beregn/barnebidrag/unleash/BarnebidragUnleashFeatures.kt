package no.nav.bidrag.beregn.barnebidrag.unleash

import io.getunleash.variant.Variant
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider

/**
 * Eksempel for hvordan UnleashFeatures kan defineres.
 *
 * Dette kan da kalles på følgende måte:
 * ```kotlin
 *   UnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT.isEnabled
 * ```
 */
enum class BarnebidragUnleashFeatures(val featureName: String, defaultValue: Boolean) {
    BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT("beregning.bidrag_beregning_fra-forste-periode-over-tolv-prosent", false),
    BIDRAG_REDUKSJON_UNDERHOLDSKOSTNAD("beregning.bidrag_reduksjon_underholdskostnad", false),
    ;

    private var defaultValue = false

    init {
        this.defaultValue = defaultValue
    }

    val isEnabled: Boolean
        get() = UnleashFeaturesProvider.isEnabled(featureName, defaultValue)

    val variant: Variant?
        get() = UnleashFeaturesProvider.getVariant(featureName)
}
