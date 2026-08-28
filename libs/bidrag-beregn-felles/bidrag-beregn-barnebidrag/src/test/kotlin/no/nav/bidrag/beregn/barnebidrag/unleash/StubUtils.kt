package no.nav.bidrag.beregn.barnebidrag.unleash

import io.mockk.every
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider

fun enableUnleashFeature(feature: BarnebidragUnleashFeatures) = every {
    UnleashFeaturesProvider
        .isEnabled(feature = eq(feature.featureName), defaultValue = any())
} returns true

fun disableUnleashFeature(feature: BarnebidragUnleashFeatures) = every {
    UnleashFeaturesProvider
        .isEnabled(feature = eq(feature.featureName), defaultValue = any())
} returns false
