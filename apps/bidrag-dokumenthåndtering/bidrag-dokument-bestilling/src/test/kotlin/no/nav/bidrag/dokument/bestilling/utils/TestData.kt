package no.nav.bidrag.dokument.bestilling.utils

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.bidrag.commons.web.mock.hentFil
import no.nav.bidrag.dokument.bestilling.api.dto.MottakerAdresseTo
import no.nav.bidrag.dokument.bestilling.api.dto.SamhandlerAdresse
import no.nav.bidrag.dokument.bestilling.api.dto.SamhandlerInformasjon
import no.nav.bidrag.dokument.bestilling.consumer.dto.BehandlingDetaljerDtoV2
import no.nav.bidrag.dokument.bestilling.consumer.dto.EnhetKontaktInfoDto
import no.nav.bidrag.dokument.bestilling.consumer.dto.EnhetPostadresseDto
import no.nav.bidrag.domene.enums.adresse.Adressetype
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.land.Landkode2
import no.nav.bidrag.domene.land.Landkode3
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.organisasjon.dto.SaksbehandlerDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.dokument.OpprettDokumentDto
import no.nav.bidrag.transport.dokument.OpprettJournalpostResponse
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.bidrag.transport.person.PersonAdresseDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.RolleDto
import java.time.LocalDate
import java.time.LocalDateTime

val TEST_DOKUMENT = "Test dokument".toByteArray()
val DEFAULT_TITLE_DOKUMENT = "Tittel på dokumentet"
val DEFAULT_SAKSNUMMER = "123312321321"

val SAMHANDLER_IDENT = "80000123213"
val BREVREF = "BIF1234567"

val SAKSBEHANDLER_IDENT = "Z99999"
val SAKSBEHANDLER_NAVN = "Saksbehandlersen, Saksbehandler Mellomnavn"
val SAK_OPPRETTET_DATO = LocalDate.parse("2023-02-02")

val SAMHANDLER_INFO =
    SamhandlerInformasjon(
        navn = "Samhandler samhandlersen",
        spraak = "NB",
        adresse =
            SamhandlerAdresse(
                adresselinje1 = "Samhandler adresselinje 1",
                adresselinje2 = "Samhandler adresselinje 2",
                adresselinje3 = "Samhandler adresselinje 3",
                postnummer = "3000",
                landkode = "NOR",
            ),
    )

val SAMHANDLER_MOTTAKER_ADRESSE =
    MottakerAdresseTo(
        adresselinje1 = "Samhandler adresselinje 1",
        adresselinje2 = "Samhandler adresselinje 2",
        adresselinje3 = "Samhandler adresselinje 3",
        postnummer = "3000",
        landkode = "NO",
        landkode3 = "NOR",
    )

val ANNEN_MOTTAKER =
    createPersonResponse(
        "444213123123333",
        "Etternavn, BPFornavn Annen mottaker",
        fodselsdato = LocalDate.parse("2001-05-06"),
    )
val BP1 =
    createPersonResponse(
        "444213123123",
        "Etternavn, BPFornavn Bidragspliktig",
        kortNavn = "BPFornavn Etternavn",
        fodselsdato = LocalDate.parse("2001-05-06"),
    )
val BM1 =
    createPersonResponse(
        "26417806511",
        "Etternavn, BMFornavn Bidragsmottaker",
        kortNavn = "BMFornavn Etternavn",
        fodselsdato = LocalDate.parse("2000-03-06"),
    )
val BARN1 =
    createPersonResponse(
        "12461690252",
        "Etternavn, Barn1 Mellomnavn",
        kortNavn = "Barn1 Etternavn",
        fodselsdato = LocalDate.parse("2020-05-06"),
    )
val BARN2 =
    createPersonResponse(
        "02461662466",
        "Etternavn, Barn2 Mellomnavn",
        kortNavn = "Barn2 Etternavn",
        fodselsdato = LocalDate.parse("2018-03-20"),
    )

val BARN3 =
    createPersonResponse(
        "27461456400",
        "Etternavn, Barn3",
        kortNavn = "Barn3 Etternavn",
        fodselsdato = LocalDate.parse("2014-03-20"),
    )

fun createSakResponse(): BidragssakDto =
    BidragssakDto(
        saksnummer = Saksnummer(DEFAULT_SAKSNUMMER),
        eierfogd = Enhetsnummer("4806"),
        roller =
            listOf(
                RolleDto(
                    fødselsnummer = BM1.ident,
                    type = Rolletype.BIDRAGSMOTTAKER,
                ),
                RolleDto(
                    fødselsnummer = BP1.ident,
                    type = Rolletype.BIDRAGSPLIKTIG,
                ),
                RolleDto(
                    fødselsnummer = BARN1.ident,
                    type = Rolletype.BARN,
                ),
                RolleDto(
                    fødselsnummer = BARN2.ident,
                    type = Rolletype.BARN,
                ),
            ),
        saksstatus = Bidragssakstatus.IN,
        kategori = Sakskategori.NASJONAL,
        opprettetDato = SAK_OPPRETTET_DATO,
        levdeAdskilt = false,
        ukjentPart = false,
//        arbeidsfordeling = Arbeidsfordeling.EEN,
    )

fun createPersonResponse(
    ident: String,
    navn: String,
    kortNavn: String? = null,
    fodselsdato: LocalDate? = null,
    dodsdato: LocalDate? = null,
    aktorId: String? = "313213",
    diskresjonskode: Diskresjonskode? = null,
): PersonDto =
    PersonDto(
        ident = Personident(ident),
        navn = navn,
        kortnavn = kortNavn,
        visningsnavn = navn,
        fødselsdato = fodselsdato,
        dødsdato = dodsdato,
        aktørId = aktorId,
        diskresjonskode = diskresjonskode,
    )

fun createPostAdresseResponse(): PersonAdresseDto =
    PersonAdresseDto(
        adresselinje1 = "Adresselinje1",
        adresselinje2 = "Adresselinje2",
        postnummer = "3030",
        poststed = "Drammen",
        land = Landkode2("NO"),
        land3 = Landkode3("NOR"),
        bruksenhetsnummer = "H0201",
        adressetype = Adressetype.BOSTEDSADRESSE,
    )

fun createPostAdresseResponseUtenlandsk(): PersonAdresseDto =
    PersonAdresseDto(
        adresselinje1 = "Utenlandsk Adresselinje1",
        adresselinje2 = "Utenlandsk Adresselinje2",
        adresselinje3 = "United states of America",
        land = Landkode2("US"),
        land3 = Landkode3("USA"),
        adressetype = Adressetype.BOSTEDSADRESSE,
    )

fun createOpprettJournalpostResponse(
    tittel: String = "Tittel på dokument",
    journalpostId: String = "123123",
    dokumentReferanse: String = "dokref1",
): OpprettJournalpostResponse =
    OpprettJournalpostResponse(
        dokumenter =
            listOf(
                OpprettDokumentDto(
                    tittel = tittel,
                    dokumentreferanse = dokumentReferanse,
                ),
            ),
        journalpostId = journalpostId,
    )

fun createEnhetKontaktInformasjon(land: String = "Norge"): EnhetKontaktInfoDto =
    EnhetKontaktInfoDto(
        enhetIdent = "4806",
        enhetNavn = "NAV Familie- og pensjonsytelser Drammen",
        telefonnummer = "55553333",
        postadresse =
            EnhetPostadresseDto(
                adresselinje1 = "Postboks 1583",
                adresselinje2 = "Linje2",
                postnummer = "3040",
                poststed = "Drammen",
                land = land,
            ),
    )

fun lagVedtaksdata(
    filnavn: String,
    gjelderIdent: PersonDto = BM1,
    barnIdent: PersonDto = BARN1,
    barnIdent2: PersonDto = BARN2,
): VedtakDto {
    val fil = hentFil("/__files/$filnavn")
    var stringValue = fil.readText().replace("{bmIdent}", gjelderIdent.ident.verdi)
    stringValue = stringValue.replace("{bmfDato}", gjelderIdent.fødselsdato.toString())
    stringValue = stringValue.replace("{bpIdent}", BP1.ident.verdi)
    stringValue = stringValue.replace("{bpfDato}", BP1.fødselsdato.toString())
    stringValue = stringValue.replace("{barnId}", barnIdent.ident.verdi)
    stringValue = stringValue.replace("{barnfDato}", barnIdent.fødselsdato.toString())
    stringValue = stringValue.replace("{barnId2}", barnIdent2.ident.verdi)
    stringValue = stringValue.replace("{barn2fDato}", barnIdent2.fødselsdato.toString())
    stringValue = stringValue.replace("{dagens_dato}", LocalDateTime.now().toString())
    val grunnlag: VedtakDto = commonObjectmapper.readValue(stringValue)
    return grunnlag
}

fun opprettBehandlingDetaljer() =
    BehandlingDetaljerDtoV2(
        id = 1,
        type = TypeBehandling.SÆRBIDRAG,
        engangsbeløptype = Engangsbeløptype.SÆRBIDRAG,
        årsak = null,
        avslag = null,
        vedtakstype = Vedtakstype.ENDRING,
        opprettetAv =
            SaksbehandlerDto(
                SAKSBEHANDLER_IDENT,
                SAKSBEHANDLER_NAVN,
            ),
        mottattdato = LocalDate.parse("2024-07-15"),
        søktFomDato = LocalDate.parse("2024-08-01"),
        opprettetTidspunkt = LocalDateTime.now(),
        søktAv = SøktAvType.BIDRAGSMOTTAKER,
        søknadsid = 1,
        søknadRefId = 1,
        vedtakRefId = 1,
        erVedtakFattet = false,
        behandlerenhet = "4806",
        saksnummer = DEFAULT_SAKSNUMMER,
        erKlageEllerOmgjøring = false,
        roller =
            setOf(
                no.nav.bidrag.dokument.bestilling.consumer.dto.RolleDto(
                    rolletype = Rolletype.BIDRAGSMOTTAKER,
                    ident = BM1.ident.verdi,
                    navn = BM1.navn,
                    fødselsdato = BM1.fødselsdato,
                ),
                no.nav.bidrag.dokument.bestilling.consumer.dto.RolleDto(
                    rolletype = Rolletype.BIDRAGSPLIKTIG,
                    ident = BP1.ident.verdi,
                    navn = BP1.navn,
                    fødselsdato = BP1.fødselsdato,
                ),
                no.nav.bidrag.dokument.bestilling.consumer.dto.RolleDto(
                    rolletype = Rolletype.BARN,
                    ident = BARN1.ident.verdi,
                    navn = BARN1.navn,
                    fødselsdato = BARN1.fødselsdato,
                ),
            ),
    )
