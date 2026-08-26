package no.nav.bidrag.commons.unleash

import io.getunleash.util.UnleashConfig
import java.util.UUID

fun generateUnleashConfig(unleashProperties: UnleashProperties): UnleashConfig =
    UnleashConfig
        .builder()
        .appName(unleashProperties.appName)
        .environment(unleashProperties.environment)
        .instanceId(UUID.randomUUID().toString())
        .unleashAPI("${unleashProperties.unleashApi}/api/")
        .apiKey(unleashProperties.apiKey)
        .synchronousFetchOnInitialisation(true)
        .unleashContextProvider(DefaultUnleashContextProvider())
        .build()
