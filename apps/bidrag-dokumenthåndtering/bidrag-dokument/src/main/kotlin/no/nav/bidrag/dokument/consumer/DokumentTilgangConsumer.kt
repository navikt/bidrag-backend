package no.nav.bidrag.dokument.consumer

import no.nav.bidrag.transport.dokument.DokumentTilgangResponse
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate

class DokumentTilgangConsumer(
    private val restTemplate: RestTemplate,
) {
    fun hentTilgangUrl(
        journalpostId: String?,
        dokumentreferanse: String?,
    ): DokumentTilgangResponse? {
        if (journalpostId.isNullOrEmpty()) {
            return restTemplate
                .exchange(
                    PATH_DOKUMENT_TILGANG_DOKREF_TEMPLATE,
                    HttpMethod.GET,
                    null,
                    DokumentTilgangResponse::class.java,
                    uriVariable(dokumentreferanse),
                ).body
        }
        return restTemplate
            .exchange(
                PATH_DOKUMENT_TILGANG_TEMPLATE,
                HttpMethod.GET,
                null,
                DokumentTilgangResponse::class.java,
                uriVariable(journalpostId),
                uriVariable(dokumentreferanse),
            ).body
    }

    private fun uriVariable(value: String?): String = value ?: "null"

    companion object {
        const val PATH_DOKUMENT_TILGANG = "/tilgang/%s/%s"
        const val PATH_DOKUMENT_TILGANG_DOKREF = "/tilgang/dokumentreferanse/%s"
        const val PATH_HENT_DOKUMENT = "/dokument/%s"
        private const val PATH_DOKUMENT_TILGANG_TEMPLATE = "/tilgang/{journalpostId}/{dokumentreferanse}"
        private const val PATH_DOKUMENT_TILGANG_DOKREF_TEMPLATE = "/tilgang/dokumentreferanse/{dokumentreferanse}"
    }
}
