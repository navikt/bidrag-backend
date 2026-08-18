package no.nav.bidrag.samhandler.util

fun String?.nullIfEmpty() = if (this?.trim().isNullOrEmpty()) null else this

val String?.kontonummerNumerisk get() =
    this
        ?.replace(" ", "")
        ?.replace(".", "")
        ?.nullIfEmpty()
