package no.nav.bidrag.dokument.bestilling.bestilling

import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingForespørsel
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentBestillingResult
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMal
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.DokumentProducer
import no.nav.bidrag.transport.dokumentmaler.DokumentBestilling
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class DokumentBestillingManager(
    val applicationContext: ApplicationContext,
) {
    fun bestill(
        reqest: DokumentBestillingForespørsel,
        dokumentMal: DokumentMal,
    ): DokumentBestillingResult {
        val bestillingData = buildDokumentBestilling(reqest, dokumentMal)
        val dokumentProducer = fetchProducer(dokumentMal)
        return dokumentProducer.produser(bestillingData, dokumentMal)
    }

    private fun fetchProducer(dokumentMal: DokumentMal): DokumentProducer =
        applicationContext.getBean(
            dokumentMal.bestillingSystem,
            DokumentProducer::class.java,
        )

    private fun buildDokumentBestilling(
        dokumentBestilling: DokumentBestillingForespørsel,
        dokumentMal: DokumentMal,
    ): DokumentBestilling =
        applicationContext
            .getBean(DokumentMetadataCollector::class.java)
            .collect(dokumentBestilling, dokumentMal)
}
