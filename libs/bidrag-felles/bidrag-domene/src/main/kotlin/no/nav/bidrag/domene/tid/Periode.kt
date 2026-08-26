@file:Suppress("unused")

package no.nav.bidrag.domene.tid

import java.time.temporal.Temporal

sealed class Periode<T> : Comparable<Periode<T>> where T : Comparable<T>, T : Temporal {
    abstract val fom: T
    abstract val til: T?

    protected fun validate() {
        require(tilEllerMax() >= fom) { "Til før fra-og-med: $fom > $til" }
    }

    abstract fun tilEllerMax(): T

    infix fun inneholder(dato: T): Boolean = dato in fom..tilEllerMax()

    infix fun inneholder(annen: Periode<T>): Boolean = annen.fom >= this.fom && annen.tilEllerMax() <= this.tilEllerMax()

    infix fun omsluttesAv(annen: Periode<T>): Boolean = annen.fom <= fom && annen.tilEllerMax() >= tilEllerMax()

    infix fun overlapper(other: Periode<T>): Boolean = inneholder(other.fom) || inneholder(other.tilEllerMax()) || other.inneholder(fom)

    open infix fun snitt(annen: Periode<T>): Periode<T>? =
        if (!overlapper(annen)) {
            null
        } else if (this == annen) {
            this
        } else {
            lagPeriode(
                maxOf(fom, annen.fom),
                if (til == null && annen.til == null) null else minOf(tilEllerMax(), annen.tilEllerMax()),
            )
        }

    open infix fun union(annen: Periode<T>): Periode<T> =
        if (overlapper(annen) || this.påfølgesAv(annen) || annen.påfølgesAv(this)) {
            lagPeriode(
                minOf(fom, annen.fom),
                if (til == null || annen.til == null) null else maxOf(tilEllerMax(), annen.tilEllerMax()),
            )
        } else {
            error("Kan ikke lage union av perioder som $this og $annen som ikke overlapper eller direkte følger hverandre.")
        }

    infix fun overlapperKunIStartenAv(annen: Periode<T>) = annen.fom in fom..tilEllerMax() && tilEllerMax() < annen.tilEllerMax()

    infix fun overlapperKunISluttenAv(annen: Periode<T>) = annen.tilEllerMax() in fom..tilEllerMax() && fom > annen.fom

    abstract infix fun påfølgesAv(påfølgende: Periode<T>): Boolean

    abstract fun lengdeIHeleMåneder(): Long

    override fun compareTo(other: Periode<T>): Int =
        Comparator.comparing(Periode<T>::fom).thenComparing(Periode<T>::tilEllerMax).compare(this, other)

    abstract fun lagPeriode(
        fom: T,
        til: T?,
    ): Periode<T>
}

fun <T> List<Periode<T>>.erSammenhengende(): Boolean where T : Comparable<T>, T : Temporal =
    this.sorted().foldIndexed(true) { index, acc, periode ->
        if (index == 0) {
            acc
        } else {
            val forrigePeriode = this[index - 1]
            when {
                forrigePeriode.påfølgesAv(periode) -> acc
                else -> false
            }
        }
    }

fun <T> List<Periode<T>>.harOverlappende(): Boolean where T : Comparable<T>, T : Temporal =
    this
        .sorted()
        .zipWithNext { a, b ->
            a.overlapper(b)
        }.any { it }
