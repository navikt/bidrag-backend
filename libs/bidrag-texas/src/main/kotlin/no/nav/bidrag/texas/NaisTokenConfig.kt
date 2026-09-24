package no.nav.bidrag.texas

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(NaisTokenProperties::class)
class NaisTokenConfig

/**
 * Samme struktur som nais environment properties https://doc.nais.io/auth/reference/#environment-variables
 */

@ConfigurationProperties(prefix = "nais.token")
data class NaisTokenProperties(
    val endpoint: String,
    @NestedConfigurationProperty
    val exchange: NaisTokenExchangeProperties,
)

data class NaisTokenExchangeProperties(
    val endpoint: String,
)
