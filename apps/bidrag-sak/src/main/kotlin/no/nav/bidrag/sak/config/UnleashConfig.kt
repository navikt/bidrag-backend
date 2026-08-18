package no.nav.bidrag.sak.config

import no.nav.bidrag.commons.unleash.EnableUnleashFeatures
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@EnableUnleashFeatures
@Profile("nais")
@Configuration
class UnleashConfig
