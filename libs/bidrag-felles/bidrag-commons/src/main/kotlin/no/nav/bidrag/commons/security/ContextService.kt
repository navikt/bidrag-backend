package no.nav.bidrag.commons.security

import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object ContextService {
    fun hentPåloggetSaksbehandler(): String =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = { it.getClaims("aad")?.get("NAVident")?.toString() ?: error("Finner ikke NAVident i token") },
                onFailure = { error("Finner ikke NAVident i token") },
            )

    fun erMaskinTilMaskinToken(): Boolean {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("aad")
        return claims.get("oid") != null &&
            claims.get("oid") == claims.get("sub") &&
            claims.getAsList("roles").contains("access_as_application")
    }
}
