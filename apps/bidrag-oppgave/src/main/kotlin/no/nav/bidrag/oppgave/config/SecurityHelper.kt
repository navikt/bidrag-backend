package no.nav.bidrag.oppgave.config

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

fun brukernavnFraJwt(): String? {
    val authentication = SecurityContextHolder.getContext().authentication
    if (authentication is JwtAuthenticationToken) {
        return authentication.token.getClaimAsString("NAVident")
    }

    return authentication?.name
}
