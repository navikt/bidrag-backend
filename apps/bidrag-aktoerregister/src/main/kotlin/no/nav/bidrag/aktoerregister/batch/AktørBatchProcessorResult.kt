package no.nav.bidrag.aktoerregister.batch

import no.nav.bidrag.aktoerregister.persistence.entities.Aktør

data class AktørBatchProcessorResult(
    val aktør: Aktør,
    val nyAktør: Aktør,
    val aktørStatus: AktørStatus,
    val originalIdent: String? = null,
)
