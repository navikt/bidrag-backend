package no.nav.bidrag.domene.enums.regnskap.behandlingsstatus

enum class Batchstatus {
    Received,
    InProgress,
    Failed,
    Processing,
    Done,
    DoneWithWarnings,
    Structuring,
    Structured,
}
