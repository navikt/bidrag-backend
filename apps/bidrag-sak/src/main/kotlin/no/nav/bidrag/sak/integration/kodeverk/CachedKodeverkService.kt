package no.nav.bidrag.sak.integration.kodeverk

import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.sak.integration.kodeverk.dto.mapTerm
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class CachedKodeverkService(
    private val kodeverkClient: KodeverkClient,
) {
    @Cacheable("kodeverk_postested")
    fun hentPostnummer(): Map<String, String> = kodeverkClient.hentPostnummer().mapTerm()

    @Cacheable("kodeverk_landkoder")
    fun hentLandkoder(): Map<Landkode, String> = kodeverkClient.hentLandkoder().mapTerm().mapKeys { Landkode(it.key) }

    @Cacheable("kodeverk_landkoder_ISO2")
    fun hentLandkoderISO2(): Map<Landkode, String> = kodeverkClient.hentLandkoderISO2().mapTerm().mapKeys { Landkode(it.key) }
}
