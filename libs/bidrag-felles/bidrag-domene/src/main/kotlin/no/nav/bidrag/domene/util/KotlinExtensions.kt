package no.nav.bidrag.domene.util

fun <T : Comparable<T>> maxOfNullable(vararg values: T?): T? {
    val nonNull = values.filterNotNull()
    return nonNull.maxOrNull()
}

fun <T : Comparable<T>> minOfNullable(vararg values: T?): T? {
    val nonNull = values.filterNotNull()
    return nonNull.minOrNull()
}
