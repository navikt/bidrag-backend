package no.nav.bidrag.dokument.bestilling.bestilling.produksjon

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.dokument.bestilling.bestilling.dto.BestillingSystem
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentBestillingResult
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMal
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMalProduksjon
import no.nav.bidrag.dokument.bestilling.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.transport.dokumentmaler.DokumentBestilling
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component(BestillingSystem.DOKUMENT_PRODUKSJON)
class BidragDokumentProduksjonProducer(
    val bidragDokumentProduksjonConsumer: BidragDokumentProduksjonConsumer,
) : DokumentProducer {
    override fun produser(
        dokumentBestilling: DokumentBestilling,
        dokumentMal: DokumentMal,
    ): DokumentBestillingResult {
        val pdf = bidragDokumentProduksjonConsumer.opprettPDF((dokumentMal as DokumentMalProduksjon).malId, dokumentBestilling)

        // TODO: Error handling
        return DokumentBestillingResult(
            dokumentReferanse = dokumentBestilling.dokumentreferanse!!,
            innhold = pdf,
            bestillingSystem = BestillingSystem.DOKUMENT_PRODUKSJON,
        )
    }
}
