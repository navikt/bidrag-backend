package no.nav.bidrag.henvendelse

import kotlin.random.Random

/**
 * En aktørid har 13 siffer, og `SensitiveLogMasker` i bidrag-commons maskerer bare 13 siffer.
 * `genererAktørid()` i bidrag-generer-testdata gir 12 siffer, og en slik id blir ikke maskert.
 */
fun genererAktøridMed13Siffer(): String = Random.nextLong(1_000_000_000_000L, 10_000_000_000_000L).toString()
