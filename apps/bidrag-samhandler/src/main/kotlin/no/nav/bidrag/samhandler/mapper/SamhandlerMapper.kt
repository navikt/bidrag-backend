package no.nav.bidrag.samhandler.mapper

import no.nav.bidrag.domene.ident.SamhandlerId
import no.nav.bidrag.domene.land.Landkode3
import no.nav.bidrag.samhandler.persistence.entity.Samhandler
import no.nav.bidrag.samhandler.util.nullIfEmpty
import no.nav.bidrag.transport.samhandler.AdresseDto
import no.nav.bidrag.transport.samhandler.KontonummerDto
import no.nav.bidrag.transport.samhandler.SamhandlerDto
import no.nav.bidrag.transport.samhandler.SamhandlersøkeresultatDto

object SamhandlerMapper {
    fun mapTilSamhandlerDto(
        samhandler: Samhandler,
        inkluderAuditLog: Boolean = false,
    ): SamhandlerDto = samhandler.let {
        SamhandlerDto(
            samhandlerId = it.ident?.let { ident -> SamhandlerId(ident) },
            navn = it.navn,
            offentligId = samhandler.offentligId,
            offentligIdType = samhandler.offentligIdType,
            språk = samhandler.språk,
            områdekode = samhandler.områdekode,
            adresse =
            AdresseDto(
                it.adresselinje1,
                adresselinje2 = it.adresselinje2,
                adresselinje3 = it.adresselinje3,
                postnr = it.postnummer,
                postnummer = it.postnummer,
                poststed = it.poststed,
                land = it.land?.let { land -> Landkode3(land) },
            ),
            kontonummer =
            KontonummerDto(
                norskKontonummer = it.norskkontonr,
                iban = it.iban,
                swift = it.swift,
                banknavn = it.banknavn,
                landkodeBank = it.banklandkode?.let { banklandkode -> Landkode3(banklandkode) },
                bankCode = it.bankcode,
                valutakode = it.valutakode,
            ),
            kontaktperson = it.kontaktperson,
            kontaktEpost = it.kontaktEpost,
            kontaktTelefon = it.kontaktTelefon,
            notat = it.notat,
            erOpphørt = it.erOpphørt,
            auditLog =
            if (inkluderAuditLog) {
                it.auditLog.map { auditLog ->
                    AuditLogMapper.mapTilAuditLogDto(auditLog)
                }
            } else {
                emptyList()
            },
        )
    }

    fun mapTilSamhandlersøkeresultatDto(samhandlere: List<Samhandler>): SamhandlersøkeresultatDto = SamhandlersøkeresultatDto(
        samhandlere =
        samhandlere.map {
            mapTilSamhandlerDto(it)
        },
        flereForekomster = samhandlere.size >= 2,
    )

    fun mapTilSamhandler(
        samhandlerDto: SamhandlerDto,
        erOpphørt: Boolean = false,
    ): Samhandler = Samhandler(
        ident = samhandlerDto.samhandlerId?.verdi.nullIfEmpty(),
        navn = samhandlerDto.navn,
        offentligId = samhandlerDto.offentligId,
        offentligIdType = samhandlerDto.offentligIdType,
        språk = samhandlerDto.språk,
        områdekode = samhandlerDto.områdekode,
        norskkontonr = samhandlerDto.kontonummer?.norskKontonummer.nullIfEmpty(),
        iban = samhandlerDto.kontonummer?.iban.nullIfEmpty(),
        swift = samhandlerDto.kontonummer?.swift.nullIfEmpty(),
        banknavn = samhandlerDto.kontonummer?.banknavn.nullIfEmpty(),
        banklandkode =
        samhandlerDto.kontonummer
            ?.landkodeBank
            ?.verdi
            .nullIfEmpty(),
        valutakode = samhandlerDto.kontonummer?.valutakode,
        bankcode = samhandlerDto.kontonummer?.bankCode.nullIfEmpty(),
        adresselinje1 = samhandlerDto.adresse?.adresselinje1.nullIfEmpty(),
        adresselinje2 = samhandlerDto.adresse?.adresselinje2.nullIfEmpty(),
        adresselinje3 = samhandlerDto.adresse?.adresselinje3.nullIfEmpty(),
        postnummer = samhandlerDto.adresse?.postnummer.nullIfEmpty(),
        poststed = samhandlerDto.adresse?.poststed.nullIfEmpty(),
        land =
        samhandlerDto.adresse
            ?.land
            ?.verdi
            .nullIfEmpty(),
        kontaktperson = samhandlerDto.kontaktperson.nullIfEmpty(),
        kontaktEpost = samhandlerDto.kontaktEpost.nullIfEmpty(),
        kontaktTelefon = samhandlerDto.kontaktTelefon.nullIfEmpty(),
        notat = samhandlerDto.notat.nullIfEmpty(),
        erOpphørt = erOpphørt,
    )
}
