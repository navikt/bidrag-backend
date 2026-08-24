package no.nav.bidrag.dokument.bestilling.consumer.dto

import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.transport.person.PersonDto

fun PersonDto.fornavnEtternavn() = visningsnavn.replace("’", "'") // listOfNotNull(fornavn?.verdi, mellomnavn?.verdi, etternavn?.verdi).joinToString(" ")

fun PersonDto.isKode6() = diskresjonskode == Diskresjonskode.SPSF || diskresjonskode == Diskresjonskode.P19 || diskresjonskode == Diskresjonskode.SPFO

fun PersonDto.isDod() = dødsdato != null
