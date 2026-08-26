package no.nav.bidrag.commons.util

fun String?.trimToNull(): String? = this?.trim()?.ifBlank { null }
