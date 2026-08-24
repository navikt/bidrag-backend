package no.nav.bidrag.dokument.bestilling.bestilling.fetcher

import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMal

interface DocumentFetcher {
    fun fetch(dokumentMalEnum: DokumentMal): ByteArray

    fun fetch(referanse: String): ByteArray
}
