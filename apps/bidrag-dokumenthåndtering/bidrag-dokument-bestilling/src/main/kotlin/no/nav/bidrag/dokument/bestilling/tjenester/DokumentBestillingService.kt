package no.nav.bidrag.dokument.bestilling.tjenester

import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingForespørsel
import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingResponse
import no.nav.bidrag.dokument.bestilling.bestilling.DokumentBestillingManager
import no.nav.bidrag.dokument.bestilling.bestilling.DokumentFetchingManager
import no.nav.bidrag.dokument.bestilling.bestilling.dto.BestillingSystem
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentBestillingResult
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMal
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMalBucket
import no.nav.bidrag.dokument.bestilling.model.kanIkkeBestilleDokumentMal
import no.nav.bidrag.dokument.bestilling.model.manglerDataGrunnlag
import no.nav.bidrag.transport.dokument.DokumentArkivSystemDto
import org.springframework.stereotype.Service

@Service
class DokumentBestillingService(
    val dokumentBestillingManager: DokumentBestillingManager,
    private val fetchingManager: DokumentFetchingManager,
) {
    fun bestill(
        bestillingRequest: DokumentBestillingForespørsel,
        dokumentMal: DokumentMal,
    ): DokumentBestillingResponse {
        if (dokumentMal is DokumentMalBucket) kanIkkeBestilleDokumentMal(dokumentMal.kode)
        val result = dokumentBestillingManager.bestill(bestillingRequest, dokumentMal)
        return DokumentBestillingResponse(
            dokumentId = result.dokumentReferanse,
            journalpostId = result.journalpostId!!,
            arkivSystem =
                when (result.bestillingSystem) {
                    BestillingSystem.BREVSERVER -> DokumentArkivSystemDto.MIDLERTIDLIG_BREVLAGER
                    else -> null
                },
        )
    }

    fun hentDokument(
        bestillingRequest: DokumentBestillingForespørsel?,
        dokumentMal: DokumentMal,
    ): ByteArray {
        val kreverDataGrunnlag = dokumentMal.kreverDataGrunnlag
        if (kreverDataGrunnlag && bestillingRequest == null) manglerDataGrunnlag(dokumentMal)
        return fetchingManager.fetchDocumentByte(dokumentMal)
    }

    fun bestillOgHent(
        bestillingRequest: DokumentBestillingForespørsel,
        dokumentMal: DokumentMal,
    ): DokumentBestillingResult = dokumentBestillingManager.bestill(bestillingRequest, dokumentMal)
}
