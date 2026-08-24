package no.nav.bidrag.dokument.bestilling.bestilling

import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMal
import no.nav.bidrag.dokument.bestilling.bestilling.fetcher.DocumentFetcher
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class DokumentFetchingManager(
    val applicationContext: ApplicationContext,
) {
    fun fetchDocumentByte(
        dokumentMal: DokumentMal,
        dokumentReferanse: String? = null,
    ): ByteArray {
        val dokumentProducer = getFetcher(dokumentMal)
        return dokumentProducer.fetch(dokumentMal)
    }

    private fun getFetcher(dokumentMal: DokumentMal): DocumentFetcher =
        applicationContext.getBean(
            dokumentMal.bestillingSystem,
            DocumentFetcher::class.java,
        )
}
