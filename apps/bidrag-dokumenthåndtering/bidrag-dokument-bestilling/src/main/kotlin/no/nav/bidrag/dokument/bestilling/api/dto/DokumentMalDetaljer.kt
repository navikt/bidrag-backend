package no.nav.bidrag.dokument.bestilling.api.dto

import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMalType
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentType
import no.nav.bidrag.dokument.bestilling.bestilling.dto.StøttetSpråk

data class DokumentMalDetaljer(
    val malId: String,
    val tittel: String,
    val beskrivelse: String = tittel,
    val type: DokumentType,
    val kanBestilles: Boolean,
    val kreverVedtak: Boolean,
    val kreverBehandling: Boolean,
    val redigerbar: Boolean,
    val språk: List<StøttetSpråk>,
    val statiskInnhold: Boolean = false,
    val nyDokumentProduksjon: Boolean = false,
    val innholdType: DokumentMalType,
    val gruppeVisningsnavn: String? = null,
    val tilhorerEnheter: List<String> = emptyList(),
)
