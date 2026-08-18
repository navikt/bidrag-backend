package no.nav.bidrag.aktoerregister.converter

import no.nav.bidrag.aktoerregister.dto.enumer.Identtype
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.transport.samhandler.SamhandlerDto
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class SamhandlerDtoTilAktoerConverter : Converter<SamhandlerDto, Aktør> {

    override fun convert(samhandler: SamhandlerDto): Aktør = Aktør(
        aktørIdent = samhandler.samhandlerId!!.verdi,
        aktørType = Identtype.AKTOERNUMMER.name,
        etternavn = samhandler.navn,
        offentligId = samhandler.offentligId,
        offentligIdType = samhandler.offentligIdType?.name,
        adresselinje1 = samhandler.adresse?.adresselinje1,
        adresselinje2 = samhandler.adresse?.adresselinje2,
        adresselinje3 = samhandler.adresse?.adresselinje3,
        postnr = samhandler.adresse?.postnr,
        poststed = samhandler.adresse?.poststed,
        land = samhandler.adresse?.land?.verdi,
        norskKontonr = samhandler.kontonummer?.norskKontonummer,
        bankCode = samhandler.kontonummer?.bankCode,
        bankNavn = samhandler.kontonummer?.banknavn,
        iban = samhandler.kontonummer?.iban,
        bankLandkode = samhandler.kontonummer?.landkodeBank?.verdi,
        swift = samhandler.kontonummer?.swift,
        valutaKode = samhandler.kontonummer?.valutakode?.name,
    )
}
