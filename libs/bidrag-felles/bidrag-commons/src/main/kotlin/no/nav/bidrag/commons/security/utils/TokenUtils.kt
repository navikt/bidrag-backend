package no.nav.bidrag.commons.security.utils

import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.security.service.OidcTokenManager
import java.text.ParseException

enum class TokenUtsteder {
    AZURE,
    TOKENX,
    STS,
    UKJENT,
}

object TokenUtils {
    private val LOGGER = KotlinLogging.logger {}
    private const val ISSUER_AZURE_AD_IDENTIFIER = "login.microsoftonline.com"
    private const val ISSUER_TOKENX_IDENTIFIER = "tokenx"
    private const val ISSUER_IDPORTEN_IDENTIFIER = "idporten"
    private const val ISSUER_STS_IDENTIFIER = "security-token-service"
    private const val ISSUER_MASKINPORTEN_IDENTIFIER = "maskinporten"
    private const val AZURE_NAV_TENANT_ID_PROD = "62366534-1ec3-4962-8869-9b5535279d0b"
    private const val AZURE_NAV_TENANT_ID_DEV = "966ac572-f5b7-4bbe-aa88-c76419c0f851"

    @JvmStatic
    fun hentSaksbehandlerIdent(): String? = if (!erApplikasjonsbruker()) hentBruker() else null

    @JvmStatic
    fun erApplikasjonsbruker(): Boolean = SikkerhetsKontekst.erIApplikasjonKontekst() || erApplikasjonsbruker(hentToken())

    @JvmStatic
    fun hentApplikasjonsnavn(): String? = hentToken()?.let { hentApplikasjonsnavn(it) }

    @JvmStatic
    fun hentBruker(): String? = hentBruker(hentToken())

    @JvmStatic
    private fun hentTokenTenantId(): String? {
        val token = hentToken()
        return try {
            konverterTokenTilJwt(token)?.jwtClaimsSet?.getStringClaim("tid")
        } catch (e: Exception) {
            LOGGER.error(e) { "Klarte ikke parse token" }
            return null
        }
    }

    @JvmStatic
    fun erProd(): Boolean = hentTokenTenantId() == AZURE_NAV_TENANT_ID_PROD

    @JvmStatic
    fun erDev(): Boolean = hentTokenTenantId() == AZURE_NAV_TENANT_ID_DEV

    @JvmStatic
    fun erTokenUtstedtAv(tokenUtsteder: TokenUtsteder): Boolean = hentToken()?.let { hentTokenUtsteder(it) == tokenUtsteder } ?: false

    @JvmStatic
    private fun hentApplikasjonsnavn(token: String): String? {
        return try {
            konverterTokenTilJwt(token)?.let { hentApplikasjonNavnFraToken(it) }
        } catch (e: Exception) {
            LOGGER.error(e) { "Klarte ikke parse token" }
            return null
        }
    }

    private fun hentTokenUtsteder(token: String): TokenUtsteder {
        return try {
            val tokenJWT = konverterTokenTilJwt(token) ?: return TokenUtsteder.UKJENT
            if (erTokenUtstedtAvAzure(tokenJWT)) {
                TokenUtsteder.AZURE
            } else if (erTokenUtstedtAvSTS(tokenJWT)) {
                TokenUtsteder.STS
            } else if (erTokenUtstedtAvTokenX(tokenJWT)) {
                TokenUtsteder.TOKENX
            } else {
                TokenUtsteder.UKJENT
            }
        } catch (e: ParseException) {
            LOGGER.error(e) { "Kunne ikke hente informasjon om tokenets issuer" }
            TokenUtsteder.UKJENT
        }
    }

    @JvmStatic
    private fun erApplikasjonsbruker(idToken: String?): Boolean = try {
        konverterTokenTilJwt(idToken)?.let {
            val claims = it.jwtClaimsSet
            val systemRessurs = "Systemressurs" == claims.getStringClaim("identType")
            val roles = claims.getStringListClaim("roles")
            val azureApp = roles != null && roles.contains("access_as_application")
            systemRessurs || azureApp
        } ?: false
    } catch (e: ParseException) {
        throw IllegalStateException("Kunne ikke hente informasjon om tokenets issuer", e)
    }

    @JvmStatic
    private fun hentBruker(token: String?): String? {
        return try {
            konverterTokenTilJwt(token)?.let { hentBruker(it) }
        } catch (e: Exception) {
            LOGGER.error(e) { "Klarte ikke parse token" }
            return null
        }
    }

    private fun erTokenUtstedtAvSTS(signedJWT: SignedJWT): Boolean = try {
        val issuer = signedJWT.jwtClaimsSet.issuer
        erTokenUtstedtAvSTS(issuer)
    } catch (e: ParseException) {
        throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", e)
    }

    private fun erTokenUtstedtAvAzure(signedJWT: SignedJWT): Boolean = try {
        val issuer = signedJWT.jwtClaimsSet.issuer
        erTokenUtstedtAvAzure(issuer)
    } catch (e: ParseException) {
        throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", e)
    }

    private fun erTokenUtstedtAvIdPorten(signedJWT: SignedJWT): Boolean = try {
        val idp = signedJWT.jwtClaimsSet.getStringClaim("idp")
        erIdPorten(idp)
    } catch (e: ParseException) {
        throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", e)
    }

    private fun erTokenUtstedtAvTokenX(signedJWT: SignedJWT): Boolean = try {
        val issuer = signedJWT.jwtClaimsSet.issuer
        erTokenUtstedtAvTokenX(issuer)
    } catch (e: ParseException) {
        throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", e)
    }

    private fun erTokenUtstedtAvAzure(issuer: String?): Boolean = issuer != null && issuer.contains(ISSUER_AZURE_AD_IDENTIFIER)

    private fun erTokenUtstedtAvSTS(issuer: String?): Boolean = issuer != null && issuer.contains(ISSUER_STS_IDENTIFIER)

    private fun erIdPorten(issuer: String?): Boolean = !issuer.isNullOrEmpty() && issuer.contains(ISSUER_IDPORTEN_IDENTIFIER)

    private fun erTokenUtstedtAvTokenX(issuer: String?): Boolean = !issuer.isNullOrEmpty() && issuer.contains(ISSUER_TOKENX_IDENTIFIER)

    private fun erTokenUtstedtAvMaskinporten(signedJWT: SignedJWT): Boolean = try {
        val issuer = signedJWT.jwtClaimsSet.issuer
        erTokenUtstedtAvMaskinporten(issuer)
    } catch (e: ParseException) {
        throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", e)
    }

    private fun erTokenUtstedtAvMaskinporten(issuer: String?): Boolean = !issuer.isNullOrEmpty() && issuer.contains(ISSUER_MASKINPORTEN_IDENTIFIER)

    private fun hentApplikasjonNavnFraToken(signedJWT: SignedJWT): String? {
        return try {
            val claims = signedJWT.jwtClaimsSet
            if (erTokenUtstedtAvAzure(signedJWT)) {
                val application = claims.getStringClaim("azp_name") ?: claims.getStringClaim("azp")
                return hentApplikasjonNavnFraAzp(application)
            } else if (erTokenUtstedtAvTokenX(signedJWT) || erTokenUtstedtAvMaskinporten(signedJWT)) {
                val application = claims.getStringClaim("client_id")
                return hentApplikasjonNavnFraAzp(application)
            } else {
                claims.audience[0]
            }
        } catch (e: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets issuer", e)
        }
    }

    private fun hentBrukerIdFraIdportenToken(signedJWT: SignedJWT): String? = try {
        val claims = signedJWT.jwtClaimsSet
        claims.getStringClaim("pid")
    } catch (e: ParseException) {
        throw IllegalStateException("Kunne ikke hente personid fra idporten tokenr", e)
    }

    private fun hentBrukerIdFraAzureToken(signedJWT: SignedJWT): String? = try {
        val claims = signedJWT.jwtClaimsSet
        val navIdent = claims.getStringClaim("NAVident")
        val application = claims.getStringClaim("azp_name")
        navIdent ?: hentApplikasjonNavnFraAzp(application)
    } catch (e: ParseException) {
        throw IllegalStateException("Kunne ikke hente informasjon om tokenets issuer", e)
    }

    private fun hentBruker(signedJWT: SignedJWT): String? = try {
        if (erTokenUtstedtAvAzure(signedJWT)) {
            hentBrukerIdFraAzureToken(signedJWT)
        } else if (erTokenUtstedtAvIdPorten(signedJWT)) {
            hentBrukerIdFraIdportenToken(signedJWT)
        } else {
            signedJWT.jwtClaimsSet.subject
        }
    } catch (e: ParseException) {
        throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", e)
    }

    private fun hentApplikasjonNavnFraAzp(azpName: String?): String? = if (azpName == null) {
        null
    } else {
        val azpNameSplit =
            azpName
                .split(":".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
        azpNameSplit[azpNameSplit.size - 1]
    }

    private fun hentToken(): String? = try {
        OidcTokenManager().hentToken()
    } catch (_: Exception) {
        null
    }

    private fun konverterTokenTilJwt(idToken: String?): SignedJWT? = idToken?.let { JWTParser.parse(idToken) as SignedJWT }
}
