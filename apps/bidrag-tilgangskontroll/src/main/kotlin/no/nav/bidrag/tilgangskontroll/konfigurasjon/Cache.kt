package no.nav.bidrag.tilgangskontroll.konfigurasjon

import no.nav.bidrag.cache.konfigurasjon.CacheExpiry
import java.time.Duration
import java.time.LocalTime

/** Samler navn og utløpsregler for begge cachelag. */
object Cache {
    const val PIP_SAK = "PIP_SAK"
    const val TILGANGSMASKIN_KJERNEREGLER = "TILGANGSMASKIN_KJERNEREGLER"
    const val TILGANGSMASKIN_KJERNEREGLER_BULK = "TILGANGSMASKIN_KJERNEREGLER_BULK"
    const val TILGANGSMASKIN_KOMPLETTEREGLER = "TILGANGSMASKIN_KOMPLETTEREGLER"
    const val TILGANGSMASKIN_KOMPLETTEREGLER_BULK = "TILGANGSMASKIN_KOMPLETTEREGLER_BULK"
    const val BRUKERINFO = "BRUKERINFO"
    const val BRUKERGRUPPER = "BRUKERGRUPPER"
    const val BRUKERE_FOR_ENHET = "BRUKERE_FOR_ENHET"
    const val GRUPPE_DETALJER = "GRUPPE_DETALJER"
    const val GRUPPE_DETALJER_TEMA = "GRUPPE_DETALJER_TEMA"
    const val BRUKERENHETER = "BRUKERENHETER"

    private val standardregler = CacheExpiry(expireAt = LocalTime.of(6, 0))

    val innstillinger = mapOf(
        PIP_SAK to CacheExpiry(ttl = Duration.ofSeconds(10)),
        TILGANGSMASKIN_KJERNEREGLER to standardregler,
        TILGANGSMASKIN_KJERNEREGLER_BULK to standardregler,
        TILGANGSMASKIN_KOMPLETTEREGLER to standardregler,
        TILGANGSMASKIN_KOMPLETTEREGLER_BULK to standardregler,
        BRUKERINFO to standardregler,
        BRUKERGRUPPER to standardregler,
        BRUKERE_FOR_ENHET to standardregler,
        GRUPPE_DETALJER to standardregler,
        GRUPPE_DETALJER_TEMA to standardregler,
        BRUKERENHETER to standardregler,
    )
}
