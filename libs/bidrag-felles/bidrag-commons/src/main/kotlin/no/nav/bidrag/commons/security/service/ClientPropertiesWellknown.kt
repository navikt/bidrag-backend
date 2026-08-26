package no.nav.bidrag.commons.security.service

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.net.URI

@Validated
@ConfigurationProperties("no.nav.security.jwt.client")
data class ClientConfigurationWellknownProperties(
    val registration:
        @NotEmpty @Valid
        Map<String, ClientPropertiesWellknown>,
)

class ClientPropertiesWellknown(
    val resourceUrl: URI? = null,
    val wellKnownUrl: URI? = null,
    val tokenXUrl: URI? = null,
    val authenticationTokenX: ClientAuthenticationProperties? = null,
)
