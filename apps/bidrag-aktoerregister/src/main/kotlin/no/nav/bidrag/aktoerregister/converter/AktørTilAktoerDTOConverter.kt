package no.nav.bidrag.aktoerregister.converter

import no.nav.bidrag.aktoerregister.dto.AdresseDTO
import no.nav.bidrag.aktoerregister.dto.AktoerDTO
import no.nav.bidrag.aktoerregister.dto.AktoerIdDTO
import no.nav.bidrag.aktoerregister.dto.DodsboDTO
import no.nav.bidrag.aktoerregister.dto.KontonummerDTO
import no.nav.bidrag.aktoerregister.dto.NavnDTO
import no.nav.bidrag.aktoerregister.dto.enumer.Gradering
import no.nav.bidrag.aktoerregister.dto.enumer.Identtype
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.Objects

@Component
class AktørTilAktoerDTOConverter : Converter<Aktør, AktoerDTO> {

    override fun convert(aktør: Aktør): AktoerDTO = AktoerDTO(
        aktoerId = convertAktoerId(aktør),
        offentligId = aktør.offentligId,
        offentligIdType = aktør.offentligIdType,
        navn = convertNavn(aktør),
        adresse = convertAdresse(aktør),
        gradering = getGradering(aktør),
        sprakkode = aktør.språkkode,
        tidligereIdenter = convertTidligereIdenter(aktør),
        fodtDato = Objects.toString(aktør.fødtDato, null),
        dodDato = Objects.toString(aktør.dødDato, null),
        dodsbo = convertDodsbo(aktør),
        kontonummer = convertKontonummer(aktør),
    )

    private fun convertAktoerId(aktør: Aktør): AktoerIdDTO = AktoerIdDTO(
        aktoerId = aktør.aktørIdent,
        identtype = Identtype.valueOf(aktør.aktørType),
    )

    private fun convertNavn(aktør: Aktør): NavnDTO = NavnDTO(
        fornavn = aktør.fornavn,
        etternavn = aktør.etternavn,
    )

    private fun getGradering(aktør: Aktør): Gradering? = Gradering.valueOf(aktør.gradering)

    private fun convertAdresse(aktør: Aktør): AdresseDTO = AdresseDTO(
        navn = aktør.etternavn,
        adresselinje1 = aktør.adresselinje1,
        adresselinje2 = aktør.adresselinje2,
        adresselinje3 = aktør.adresselinje3,
        leilighetsnummer = aktør.leilighetsnummer,
        postnr = aktør.postnr,
        poststed = aktør.poststed,
        land = aktør.land,
    )

    private fun convertTidligereIdenter(aktør: Aktør): List<AktoerIdDTO> = aktør.tidligereIdenter.map {
        AktoerIdDTO(
            aktoerId = it.tidligereAktoerIdent,
            identtype = Identtype.valueOf(it.identtype),
        )
    }

    private fun convertDodsbo(aktør: Aktør): DodsboDTO? = aktør.dødsbo?.let {
        DodsboDTO(
            kontaktperson = aktør.dødsbo?.kontaktperson,
            adresse = AdresseDTO(
                adresselinje1 = aktør.dødsbo?.adresselinje1,
                adresselinje2 = aktør.dødsbo?.adresselinje2,
                adresselinje3 = aktør.dødsbo?.adresselinje3,
                leilighetsnummer = aktør.dødsbo?.leilighetsnummer,
                postnr = aktør.dødsbo?.postnr,
                poststed = aktør.dødsbo?.poststed,
                land = aktør.dødsbo?.land,
            ),
        )
    }

    private fun convertKontonummer(aktør: Aktør): KontonummerDTO = KontonummerDTO(
        norskKontonr = aktør.norskKontonr,
        iban = aktør.iban,
        swift = aktør.swift,
        bankCode = aktør.bankCode,
        bankNavn = aktør.bankNavn,
        bankLandkode = aktør.bankLandkode,
        valutaKode = aktør.valutaKode,
    )
}
