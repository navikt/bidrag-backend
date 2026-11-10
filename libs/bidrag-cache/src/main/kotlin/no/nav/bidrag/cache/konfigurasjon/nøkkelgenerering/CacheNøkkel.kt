package no.nav.bidrag.cache.konfigurasjon.nøkkelgenerering

import java.security.MessageDigest
import java.util.HexFormat

/**
 * Nøkkel for en cacheverdi. [brukerIdent] er null når verdien deles mellom alle brukere.
 *
 * [toString] brukes som nøkkel i Valkey. Brukerident og parametere hashes med SHA-256 slik at
 * fødselsnumre og andre personopplysninger ikke ligger i klartekst. Caffeine bruker [equals] og
 * [hashCode] og påvirkes ikke.
 */
data class CacheNøkkel(
    val metode: String,
    val brukerIdent: String?,
    val parametere: Any,
) {
    override fun toString(): String = "$metode::${sha256(listOfNotNull(brukerIdent, parametere).joinToString("::"))}"

    private fun sha256(verdi: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(verdi.toByteArray()))
}
