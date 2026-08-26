package no.nav.bidrag.transport.dokument

const val FARSKAP_UTELUKKET_PREFIKS = "FARSKAP UTELUKKET"

object BidragEnhet {
    const val ENHET_FARSKAP = "4860"
    const val ENHET_UTLAND = "4865"
}

enum class JournalpostStatus(
    val kode: String,
) {
    AVVIK_ENDRE_FAGOMRADE("AF"),
    AVVIK_BESTILL_RESKANNING("AR"),
    AVVIK_BESTILL_SPLITTING("AS"),
    MOTTATT("M"),
    JOURNALFØRT("J"),
    EKSPEDERT("E"),
    EKSPEDERT_JOARK("EJ"),
    MOTTAKSREGISTRERT("M"),
    UKJENT(""),
    DISTRIBUERT("DR"),
    AVBRUTT("A"),
    KLAR_FOR_DISTRIBUSJON("KP"),
    DOKUMENT_SLETTET("DS"),
    RETUR("RE"),
    FERDIGSTILT("FS"),
    FEILREGISTRERT("F"),
    RESERVERT("R"),
    UTGÅR("U"),
    SLETTET("S"),
    UNDER_OPPRETTELSE("UO"),
    TIL_LAGRING("T"),
    OPPRETTET("O"),
    UNDER_PRODUKSJON("D"),
    ;

    companion object {
        fun fraKode(kode: String?): JournalpostStatus? =
            try {
                JournalpostStatus.values().firstOrNull { it.kode == kode } ?: kode?.let { JournalpostStatus.valueOf(it) }
            } catch (e: Exception) {
                null
            }
    }
}
