package no.nav.bidrag.aktoerregister.service

import no.nav.bidrag.aktoerregister.dto.enumer.Hendelsestype
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import org.springframework.stereotype.Component

@Component
class Aktørendringstracker {

    /**
     * Sammenligner to aktører og returnerer hvilke felt som har endret seg.
     * Brukes for å spore endringer når en aktør oppdateres.
     */
    fun utledEndringer(gammelAktør: Aktør, nyAktør: Aktør): Set<Hendelsestype> {
        val endringer = mutableSetOf<Hendelsestype>()

        if (gammelAktør.aktørIdent != nyAktør.aktørIdent) {
            endringer.add(Hendelsestype.IDENT_OPPDATERING)
        }

        if (gammelAktør.fornavn?.lowercase()?.trim() != nyAktør.fornavn?.lowercase()?.trim() ||
            gammelAktør.etternavn?.lowercase()?.trim() != nyAktør.etternavn?.lowercase()?.trim()
        ) {
            endringer.add(Hendelsestype.NAVN_OPPDATERING)
        }

        if (harKontonummerEndret(gammelAktør, nyAktør)) {
            endringer.add(Hendelsestype.KONTONUMMER_OPPDATERING)
        }

        if (harAdresseEndret(gammelAktør, nyAktør)) {
            endringer.add(Hendelsestype.ADRESSE_OPPDATERING)
        }

        if (gammelAktør.fødtDato != nyAktør.fødtDato) {
            endringer.add(Hendelsestype.FODT_DATO_OPPDATERING)
        }

        if (gammelAktør.dødDato != nyAktør.dødDato) {
            endringer.add(Hendelsestype.DOD_DATO_OPPDATERING)
        }

        if (gammelAktør.gradering != nyAktør.gradering) {
            endringer.add(Hendelsestype.GRADERING_OPPDATERING)
        }

        if (harDødsboEndret(gammelAktør, nyAktør)) {
            endringer.add(Hendelsestype.DODSBO_OPPDATERING)
        }

        if (gammelAktør.språkkode != nyAktør.språkkode) {
            endringer.add(Hendelsestype.SPRAK_OPPDATERING)
        }

        return endringer
    }

    /**
     * Finner hvilke felt som finnes på en ny aktør.
     * Brukes når en aktør opprettes for første gang.
     */
    fun trackNyAktør(aktør: Aktør): Set<Hendelsestype> {
        val felter = mutableSetOf<Hendelsestype>()

        // Ident finnes alltid når aktør opprettes
        felter.add(Hendelsestype.IDENT_OPPDATERING)

        if (aktør.fornavn != null) {
            felter.add(Hendelsestype.NAVN_OPPDATERING)
        }

        if (harKontonummer(aktør)) {
            felter.add(Hendelsestype.KONTONUMMER_OPPDATERING)
        }

        if (harAdresse(aktør)) {
            felter.add(Hendelsestype.ADRESSE_OPPDATERING)
        }

        if (aktør.fødtDato != null) {
            felter.add(Hendelsestype.FODT_DATO_OPPDATERING)
        }

        if (aktør.dødDato != null) {
            felter.add(Hendelsestype.DOD_DATO_OPPDATERING)
        }

        if (aktør.gradering != null) {
            felter.add(Hendelsestype.GRADERING_OPPDATERING)
        }

        if (harDødsbo(aktør)) {
            felter.add(Hendelsestype.DODSBO_OPPDATERING)
        }

        if (aktør.språkkode != null) {
            felter.add(Hendelsestype.SPRAK_OPPDATERING)
        }

        return felter
    }

    private fun harKontonummerEndret(gammel: Aktør, ny: Aktør): Boolean = gammel.norskKontonr != ny.norskKontonr ||
        gammel.iban != ny.iban ||
        gammel.swift != ny.swift ||
        gammel.bankNavn != ny.bankNavn ||
        gammel.bankLandkode != ny.bankLandkode ||
        gammel.bankCode != ny.bankCode ||
        gammel.valutaKode != ny.valutaKode

    private fun harKontonummer(aktør: Aktør): Boolean = aktør.norskKontonr != null ||
        aktør.iban != null ||
        aktør.swift != null ||
        aktør.bankNavn != null ||
        aktør.bankLandkode != null ||
        aktør.bankCode != null ||
        aktør.valutaKode != null

    private fun harAdresseEndret(gammel: Aktør, ny: Aktør): Boolean = gammel.adresselinje1 != ny.adresselinje1 ||
        gammel.adresselinje2 != ny.adresselinje2 ||
        gammel.adresselinje3 != ny.adresselinje3 ||
        gammel.leilighetsnummer != ny.leilighetsnummer ||
        gammel.postnr != ny.postnr ||
        gammel.poststed != ny.poststed ||
        gammel.land != ny.land

    private fun harAdresse(aktør: Aktør): Boolean = aktør.adresselinje1 != null ||
        aktør.adresselinje2 != null ||
        aktør.adresselinje3 != null ||
        aktør.leilighetsnummer != null ||
        aktør.postnr != null ||
        aktør.poststed != null ||
        aktør.land != null

    private fun harDødsboEndret(gammel: Aktør, ny: Aktør): Boolean = gammel.dødsbo?.kontaktperson != ny.dødsbo?.kontaktperson ||
        gammel.dødsbo?.adresselinje1 != ny.dødsbo?.adresselinje1 ||
        gammel.dødsbo?.adresselinje2 != ny.dødsbo?.adresselinje2 ||
        gammel.dødsbo?.adresselinje3 != ny.dødsbo?.adresselinje3 ||
        gammel.dødsbo?.leilighetsnummer != ny.dødsbo?.leilighetsnummer ||
        gammel.dødsbo?.postnr != ny.dødsbo?.postnr ||
        gammel.dødsbo?.poststed != ny.dødsbo?.poststed ||
        gammel.dødsbo?.land != ny.dødsbo?.land

    private fun harDødsbo(aktør: Aktør): Boolean = aktør.dødsbo?.kontaktperson != null ||
        aktør.dødsbo?.adresselinje1 != null ||
        aktør.dødsbo?.adresselinje2 != null ||
        aktør.dødsbo?.adresselinje3 != null ||
        aktør.dødsbo?.leilighetsnummer != null ||
        aktør.dødsbo?.postnr != null ||
        aktør.dødsbo?.poststed != null ||
        aktør.dødsbo?.land != null
}
