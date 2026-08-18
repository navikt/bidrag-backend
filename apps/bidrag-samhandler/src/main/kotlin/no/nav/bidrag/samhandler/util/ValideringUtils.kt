package no.nav.bidrag.samhandler.util

import kotlin.reflect.KProperty

typealias DuplikatSamhandlerMap = MutableMap<String, MutableList<String>>
typealias ValideringMap = MutableMap<String, String>

fun getPath(vararg properties: KProperty<*>): String = properties.joinToString(".") { it.name }

fun ValideringMap.leggTil(
    feltnavn: String,
    feilmelding: String,
) {
    if (this[feltnavn] == null) {
        this[feltnavn] = feilmelding
    } else {
        this[feltnavn] = "${this[feltnavn]}, $feilmelding"
    }
}

fun DuplikatSamhandlerMap.add(
    samhandlerId: String,
    feltnavn: String,
) {
    if (this[samhandlerId] == null) {
        this[samhandlerId] = mutableListOf()
    }
    this[samhandlerId]!!.add(feltnavn)
}
