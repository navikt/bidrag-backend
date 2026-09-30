package no.nav.bidrag.regnskap

import no.nav.bidrag.commons.unleash.EnableUnleashFeatures
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!test & !h2")
@EnableUnleashFeatures
class UnleashConfig
