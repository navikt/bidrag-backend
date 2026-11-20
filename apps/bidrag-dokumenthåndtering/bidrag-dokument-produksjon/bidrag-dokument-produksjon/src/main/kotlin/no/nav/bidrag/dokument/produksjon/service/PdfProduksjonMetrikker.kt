package no.nav.bidrag.dokument.produksjon.service

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.BaseUnits
import org.springframework.stereotype.Component
import java.time.Duration

const val METRIKK_PDF_PRODUKSJON = "pdf.produksjon"
const val METRIKK_PDF_PRODUKSJON_STØRRELSE = "pdf.produksjon.storrelse"
const val TAG_STEG = "steg"
const val TAG_KATEGORI = "kategori"
const val TAG_DOKUMENTMAL = "dokumentmal"
const val TAG_UTFALL = "utfall"
const val TAG_DOKUMENTTYPE = "type"
const val UGYLDIG_TAGVERDI = "ugyldig"

private val GYLDIG_TAGVERDI = Regex("^[\\p{L}\\p{N}_.\\-]{1,64}$")

private val TIDSBØTTER =
    listOf(50L, 100L, 250L, 500L, 1_000L, 2_000L, 3_000L, 5_000L, 8_000L, 13_000L, 20_000L, 30_000L, 60_000L)
        .map(Duration::ofMillis)
        .toTypedArray()

private const val KILOBYTE = 1_024.0
private val STØRRELSESBØTTER =
    listOf(10.0, 50.0, 100.0, 250.0, 500.0, 1_000.0, 2_500.0, 5_000.0, 10_000.0)
        .map { it * KILOBYTE }
        .toDoubleArray()

enum class PdfProduksjonSteg(
    val tagverdi: String,
) {
    HENT_HTML("hent_html"),
    RENS_HTML("rens_html"),
    KONVERTER_PDF("konverter_pdf"),
    TOTALT("totalt"),
}

enum class PdfProduksjonUtfall(
    val tagverdi: String,
) {
    OK("ok"),
    FEIL("feil"),
}

enum class PdfProduksjonDokumenttype(
    val tagverdi: String,
) {
    HTML("html"),
    PDF("pdf"),
}

@Component
class PdfProduksjonMetrikker(
    private val meterRegistry: MeterRegistry,
) {
    fun <T> målSteg(
        steg: PdfProduksjonSteg,
        kategori: String,
        dokumentmal: String,
        utfallFor: (T) -> PdfProduksjonUtfall = { if (it == null) PdfProduksjonUtfall.FEIL else PdfProduksjonUtfall.OK },
        block: () -> T,
    ): T {
        val sample = Timer.start(meterRegistry)
        var utfall = PdfProduksjonUtfall.FEIL
        try {
            return block().also { utfall = utfallFor(it) }
        } finally {
            sample.stop(timer(steg, kategori, dokumentmal, utfall))
        }
    }

    fun registrerStørrelse(
        dokumenttype: PdfProduksjonDokumenttype,
        kategori: String,
        dokumentmal: String,
        antallBytes: Int,
    ) {
        DistributionSummary
            .builder(METRIKK_PDF_PRODUKSJON_STØRRELSE)
            .description("Størrelse på HTML og PDF i PDF-produksjonen")
            .baseUnit(BaseUnits.BYTES)
            .tag(TAG_DOKUMENTTYPE, dokumenttype.tagverdi)
            .tag(TAG_KATEGORI, tagverdi(kategori))
            .tag(TAG_DOKUMENTMAL, tagverdi(dokumentmal))
            .serviceLevelObjectives(*STØRRELSESBØTTER)
            .register(meterRegistry)
            .record(antallBytes.toDouble())
    }

    private fun timer(
        steg: PdfProduksjonSteg,
        kategori: String,
        dokumentmal: String,
        utfall: PdfProduksjonUtfall,
    ): Timer = Timer
        .builder(METRIKK_PDF_PRODUKSJON)
        .description("Tid brukt per steg i PDF-produksjonen")
        .tag(TAG_STEG, steg.tagverdi)
        .tag(TAG_KATEGORI, tagverdi(kategori))
        .tag(TAG_DOKUMENTMAL, tagverdi(dokumentmal))
        .tag(TAG_UTFALL, utfall.tagverdi)
        .serviceLevelObjectives(*TIDSBØTTER)
        .register(meterRegistry)
}

// Kategori og dokumentmal kommer fra path-parametre. Avgrens verdiene så en ugyldig sti ikke gir nye tidsserier uten grense.
internal fun tagverdi(verdi: String): String = GYLDIG_TAGVERDI.matchEntire(verdi.trim().lowercase())?.value ?: UGYLDIG_TAGVERDI
