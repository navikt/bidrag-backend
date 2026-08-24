package no.nav.bidrag.dokument.bestilling.bestilling.fetcher

import no.nav.bidrag.dokument.bestilling.bestilling.dto.BestillingSystem
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMal
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMalBucket
import no.nav.bidrag.dokument.bestilling.persistence.bucket.GcpCloudStorage
import org.springframework.stereotype.Component

@Component(BestillingSystem.BUCKET)
class StaticFileFetcher(
    private val gcpCloudStorage: GcpCloudStorage,
) : DocumentFetcher {
    override fun fetch(dokumentMalEnum: DokumentMal): ByteArray {
        if (dokumentMalEnum !is DokumentMalBucket) throw RuntimeException("Kan ikke hente dokumentmal ${dokumentMalEnum.kode} fra bucket")
        return gcpCloudStorage.hentFil(dokumentMalEnum.filePath)
    }

    override fun fetch(referanse: String): ByteArray {
        TODO("Not yet implemented")
    }
}
