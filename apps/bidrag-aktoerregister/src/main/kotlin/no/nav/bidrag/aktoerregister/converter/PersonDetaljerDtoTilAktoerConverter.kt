package no.nav.bidrag.aktoerregister.converter

import no.nav.bidrag.aktoerregister.dto.enumer.Diskresjonskode
import no.nav.bidrag.aktoerregister.dto.enumer.Gradering
import no.nav.bidrag.aktoerregister.dto.enumer.Identtype
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.aktoerregister.persistence.entities.Dødsbo
import no.nav.bidrag.aktoerregister.persistence.entities.TidligereIdenter
import no.nav.bidrag.transport.person.PersondetaljerDto
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class PersonDetaljerDtoTilAktoerConverter : Converter<PersondetaljerDto, Aktør> {

    override fun convert(personDetaljer: PersondetaljerDto): Aktør = Aktør(
        aktørIdent = personDetaljer.person.ident.verdi,
        aktørType = Identtype.PERSONNUMMER.name,
        fornavn = personDetaljer.person.fornavn,
        etternavn = personDetaljer.person.etternavn,
        adresselinje1 = personDetaljer.adresse?.adresselinje1,
        adresselinje2 = personDetaljer.adresse?.adresselinje2,
        adresselinje3 = personDetaljer.adresse?.adresselinje3,
        leilighetsnummer = personDetaljer.adresse?.bruksenhetsnummer,
        postnr = personDetaljer.adresse?.postnummer,
        poststed = personDetaljer.adresse?.poststed,
        land = personDetaljer.adresse?.land3?.verdi,
        norskKontonr = personDetaljer.kontonummer?.norskKontonr,
        bankCode = personDetaljer.kontonummer?.bankkode,
        bankNavn = personDetaljer.kontonummer?.banknavn,
        iban = personDetaljer.kontonummer?.iban,
        bankLandkode = personDetaljer.kontonummer?.banklandkode?.verdi,
        swift = personDetaljer.kontonummer?.swift,
        valutaKode = personDetaljer.kontonummer?.valutakode,
        språkkode = personDetaljer.språk?.uppercase(),
        fødtDato = personDetaljer.person.fødselsdato,
        dødDato = personDetaljer.person.dødsdato,
        gradering = finnGradering(personDetaljer),
        tidligereIdenter = opprettTidligereIndenter(personDetaljer),
        dødsbo = opprettDodsbo(personDetaljer),
    )

    private fun opprettDodsbo(personDetaljer: PersondetaljerDto): Dødsbo? = personDetaljer.dødsbo?.let {
        Dødsbo(
            kontaktperson = it.kontaktperson,
            adresselinje1 = it.kontaktadresse.adresselinje1,
            adresselinje2 = it.kontaktadresse.adresselinje2,
            postnr = it.kontaktadresse.postnummer,
            poststed = it.kontaktadresse.poststed,
            land = it.kontaktadresse.land3?.verdi,
        )
    }

    private fun opprettTidligereIndenter(personDetaljer: PersondetaljerDto): MutableSet<TidligereIdenter> = personDetaljer.tidligereIdenter?.map {
        TidligereIdenter(
            tidligereAktoerIdent = it.verdi,
            identtype = Identtype.PERSONNUMMER.name,
        )
    }?.toMutableSet() ?: mutableSetOf()

    private fun finnGradering(personDetaljer: PersondetaljerDto): String? = Gradering.from(Diskresjonskode.valueOf(personDetaljer.person.diskresjonskode?.name))?.name
}
