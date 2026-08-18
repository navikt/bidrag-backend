package no.nav.bidrag.sak.util

fun <T, R> T?.takeIfNotNullOrEmpty(block: (T) -> R): R? = if ((this == null) ||
    (
        (this is String) &&
            this.trim().isEmpty()
        ) ||
    (
        (this is List<*>) &&
            this.isEmpty()
        )
) {
    null
} else {
    block(this)
}

fun String?.trimToNull(): String? = if (this.isNullOrBlank()) null else this
