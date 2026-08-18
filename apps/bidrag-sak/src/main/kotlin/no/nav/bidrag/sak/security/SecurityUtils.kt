package no.nav.bidrag.sak.security

import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.util.Base64

object SecurityUtils {
    private const val BASIC_AUTHENTICATION_PREFIX = "Basic "

    /**
     * Extract username and password from a bas64 encoded basic authorization header string
     *
     * @param base64Auth Content of authorization header, thus "Basic " + Base64 of
     * "username:password", e.g. "Basic dG9tOmFiYzEyMw="
     * @return decoded username and password in a String array of size 2.
     * Username fist, password second.
     */
    fun extractUsernameAndPasswordFromBasicAuthHeader(base64Auth: String): List<String> {
        val base64AuthDetails = base64Auth.substring(BASIC_AUTHENTICATION_PREFIX.length)
        val decodedAuthDetails = Base64.getDecoder().decode(base64AuthDetails)
        val authDetails = String(decodedAuthDetails, StandardCharsets.UTF_8)
        return authDetails.split(":", limit = 2)
    }

    @Throws(ParseException::class)
    fun parseIdToken(idToken: String?): SignedJWT = JWTParser.parse(idToken) as SignedJWT
}
