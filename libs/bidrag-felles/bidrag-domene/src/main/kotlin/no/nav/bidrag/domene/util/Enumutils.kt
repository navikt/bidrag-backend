package no.nav.bidrag.domene.util

import kotlin.enums.EnumEntries

/**
 * Finner en enum-verdi basert på navn.
 * Returnerer null hvis ingen treff blir funnet.
 */
fun <E : Enum<E>> EnumEntries<E>.valueOfOrNull(navn: String) = firstOrNull { it.name == navn.uppercase() }

/**
 * Finner en enum-verdi ved å matche en parameterverdi.
 * Returnerer null hvis ingen treff blir funnet.
 *
 * Eksempel: Transaksjonskode.entries.finnMedParameterverdi("A3") { it.korreksjonskode } returnerer A1
 */
inline fun <E : Enum<E>, T> EnumEntries<E>.finnMedParameterverdi(
    verdi: T,
    parameter: (E) -> T,
) = firstOrNull { parameter(it) == verdi }

/**
 * Finner alle enum-verdier som matcher en parameterverdi.
 * Returnerer en liste med alle treff. Listen er tom hvis ingen treff blir funnet.
 *
 * Eksempel: Transaksjonskode.entries.finnAlleMedParameterverdi(false) { it.negativtBeløp } returnerer alle med negativtBeløp = false
 */
inline fun <E : Enum<E>, T> EnumEntries<E>.finnAlleMedParameterverdi(
    verdi: T,
    parameter: (E) -> T,
): List<E> = filter { parameter(it) == verdi }
