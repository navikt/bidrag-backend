package no.nav.bidrag.person.query

fun String?.formaterNavnMedStorForbokstav() = this?.lowercase()
    ?.split("-")
    ?.joinToString("-") { split -> split.replaceFirstChar { it.uppercase() } }
    ?.split(" ")
    ?.joinToString(" ") { split -> split.replaceFirstChar { it.uppercase() } } ?: ""
