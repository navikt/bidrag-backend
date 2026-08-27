package no.nav.bidrag.domene.util

fun String?.trimToNull(): String? = this?.trim()?.ifBlank { null }
