package no.nav.bidrag.texas

import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.web.client.RestClient.builder

class NaisTokenService(
    environment: Environment,
    val tokenSupplier: () -> String,
) {
    private val oboEndpoint = environment.getRequiredProperty("nais.token.exchange.endpoint")
    private val m2mEndpoint = environment.getRequiredProperty("nais.token.endpoint")

    private val log = LoggerFactory.getLogger(javaClass)
    private val texas = NaisTokenClient(
        client = builder().build(),
        oboEndpoint = oboEndpoint,
        m2mEndpoint = m2mEndpoint,
    )

    init {
        log.debug("NaisTokenService satt opp med oboEndpoint=$oboEndpoint og m2mEndpoint=$m2mEndpoint")
    }

    /** Henter token for innlogget bruker */
    fun oboToken(target: String): String {
        val token = tokenSupplier()
        return texas.oboToken(target, token).access_token
    }

    /** Henter token for denne applikajsonen */
    fun m2mToken(target: String): String = texas.m2mToken(target).access_token
}
