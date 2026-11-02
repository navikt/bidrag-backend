package no.nav.bidrag.texas

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = "nais.token")
data class NaisTokenProperties(
    val endpoint: String,
    @NestedConfigurationProperty
    val exchange: NaisTokenExchangeProperties,
)

data class NaisTokenExchangeProperties(
    val endpoint: String,
)
