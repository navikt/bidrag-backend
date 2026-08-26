package no.nav.bidrag.commons.security.service

import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

class OidcTokenManager {
    companion object {
        const val ISSO_ISSUER = "isso"
        const val AZURE_ISSUER = "aad"
        const val TOKENX_ISSUER = "tokenx"
        const val STS_ISSUER = "sts"
    }

    fun fetchTokenAsString(): String = fetchToken().encodedToken

    private fun hasIssuers(): Boolean = SpringTokenValidationContextHolder().getTokenValidationContext().issuers.isNotEmpty()

    fun isValidTokenIssuedByAzure(): Boolean =
        hasIssuers() && SpringTokenValidationContextHolder().getTokenValidationContext().getJwtToken(AZURE_ISSUER) != null

    fun isValidTokenIssuedByTokenX(): Boolean =
        hasIssuers() && SpringTokenValidationContextHolder().getTokenValidationContext().getJwtToken(TOKENX_ISSUER) != null

    fun isValidTokenIssuedByOpenAm(): Boolean =
        hasIssuers() && SpringTokenValidationContextHolder().getTokenValidationContext().getJwtToken(ISSO_ISSUER) != null

    fun isValidTokenIssuedBySTS(): Boolean =
        hasIssuers() && SpringTokenValidationContextHolder().getTokenValidationContext().getJwtToken(STS_ISSUER) != null

    fun hentToken(): String? {
        if (SikkerhetsKontekst.erIApplikasjonKontekst()) return null
        if (SpringTokenValidationContextHolder().getTokenValidationContext().hasValidToken()) {
            return SpringTokenValidationContextHolder()
                .getTokenValidationContext()
                .firstValidToken!!
                .encodedToken
        }
        return null
    }

    @Deprecated("Bruk TokenUtils istedenfor")
    fun hentSaksbehandlerIdentFraToken(): String? = TokenUtils.hentBruker()

    @Deprecated("Bruk TokenUtils istedenfor")
    fun erApplikasjonBruker(): Boolean = SikkerhetsKontekst.erIApplikasjonKontekst() || TokenUtils.erApplikasjonsbruker()

    fun getIssuer(): String = fetchToken().issuer

    fun fetchToken(): JwtToken =
        SpringTokenValidationContextHolder().getTokenValidationContext().firstValidToken
            ?: throw IllegalStateException("Fant ingen gyldig token i kontekst")
}
