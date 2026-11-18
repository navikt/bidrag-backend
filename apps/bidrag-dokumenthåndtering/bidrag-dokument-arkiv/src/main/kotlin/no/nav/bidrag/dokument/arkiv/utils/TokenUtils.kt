package no.nav.bidrag.dokument.arkiv.utils

import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import io.github.oshai.kotlinlogging.KotlinLogging
import java.text.ParseException

object TokenUtils {
    private val LOGGER = KotlinLogging.logger { }

    @Throws(ParseException::class)
    fun parseIdToken(idToken: String?): SignedJWT = JWTParser.parse(idToken) as SignedJWT

    @JvmStatic
    fun henteSubject(idToken: String): String = try {
        henteSubject(parseIdToken(idToken))
    } catch (e: Exception) {
        val message = "Klarte ikke parse idToken."
        LOGGER.error(e) { message }
        if (e is RuntimeException) {
            throw e
        } else {
            throw IllegalArgumentException(message, e)
        }
    }

    private fun henteSubject(signedJWT: SignedJWT): String = try {
        if (isTokenIssuedByAzure(signedJWT)) hentSubjectIdFraAzureToken(signedJWT) else signedJWT.jwtClaimsSet.subject
    } catch (var2: ParseException) {
        throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
    }

    @JvmStatic
    fun isTokenIssuedByAzure(signedJWT: SignedJWT): Boolean = try {
        val issuer = signedJWT.jwtClaimsSet.issuer
        isTokenIssuedByAzure(issuer)
    } catch (var2: ParseException) {
        throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
    }

    @JvmStatic
    fun isTokenIssuedByAzure(issuer: String?): Boolean = issuer != null && issuer.contains("login.microsoftonline.com")

    private fun hentSubjectIdFraAzureToken(signedJWT: SignedJWT): String = try {
        val claims = signedJWT.jwtClaimsSet
        val navIdent = claims.getStringClaim("NAVident")
        val application = claims.getStringClaim("azp_name")
        navIdent ?: getApplicationNameFromAzp(application)!!
    } catch (var4: ParseException) {
        throw IllegalStateException("Kunne ikke hente informasjon om tokenets issuer", var4)
    }

    private fun getApplicationNameFromAzp(azpName: String?): String? = if (azpName == null) {
        null
    } else {
        val azpNameSplit = azpName.split(":".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        azpNameSplit[azpNameSplit.size - 1]
    }
}
