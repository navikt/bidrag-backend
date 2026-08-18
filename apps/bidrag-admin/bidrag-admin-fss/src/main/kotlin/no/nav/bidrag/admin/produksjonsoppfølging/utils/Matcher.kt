package no.nav.bidrag.admin.produksjonsoppfølging.utils

fun interface Matcher<in T> {
    fun matches(value: T): Boolean
}
