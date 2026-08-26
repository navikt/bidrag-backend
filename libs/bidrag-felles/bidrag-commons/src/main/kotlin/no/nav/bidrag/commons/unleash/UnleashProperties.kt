package no.nav.bidrag.commons.unleash

import org.springframework.beans.factory.annotation.Value

class UnleashProperties {
    @Value("\${NAIS_APP_NAME}")
    lateinit var appName: String

    @Value("\${UNLEASH_SERVER_API_ENV}")
    lateinit var environment: String

    @Value("\${UNLEASH_SERVER_API_URL}")
    lateinit var unleashApi: String

    @Value("\${UNLEASH_SERVER_API_TOKEN}")
    lateinit var apiKey: String
}
