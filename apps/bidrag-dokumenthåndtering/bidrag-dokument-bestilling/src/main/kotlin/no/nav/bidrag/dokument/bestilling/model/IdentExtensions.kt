package no.nav.bidrag.dokument.bestilling.model

import no.nav.bidrag.dokument.bestilling.consumer.dto.fornavnEtternavn
import no.nav.bidrag.dokument.bestilling.consumer.dto.isKode6
import no.nav.bidrag.transport.person.PersonDto
import java.time.LocalDate

typealias Ident = String

val Ident.erDødfødt get() = this.matches("^[0-9]{6}000[0-1][0-9]".toRegex())
val Ident.erSamhandler get() = this.matches("^[8-9][0-9]{10}$".toRegex())

fun PersonDto.tilVisningsnavnVoksen() =
    if (isKode6()) {
        ""
    } else {
        fornavnEtternavn()
    }

fun PersonDto.tilVisningsnavnBarn(språk: String) =
    if (isKode6()) {
        hentKode6NavnBarn(språk)
    } else {
        fornavnEtternavn()
    }

fun PersonDto.hentFodselsdato(): LocalDate? = if (isKode6()) null else fødselsdato

fun PersonDto.hentKode6NavnBarn(
    språk: String,
): String {
    val fodtaar = fødselsdato?.year
    return when (språk) {
        SpråkKoder.BOKMAL -> if (fodtaar == null) "(BARN)" else "(BARN FØDT I $fodtaar)"
        SpråkKoder.NYNORSK -> if (fodtaar == null) "(BARN)" else "(BARN FØDD I $fodtaar)"
        else -> if (fodtaar == null) "(CHILD)" else "(CHILD BORN IN $fodtaar)"
    }
}
