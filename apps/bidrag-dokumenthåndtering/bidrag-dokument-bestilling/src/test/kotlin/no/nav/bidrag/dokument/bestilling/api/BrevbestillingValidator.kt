package no.nav.bidrag.dokument.bestilling.api

import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.BidragBarn
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.BrevBestilling
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.BrevKontaktinfo
import no.nav.bidrag.dokument.bestilling.consumer.dto.EnhetKontaktInfoDto
import no.nav.bidrag.dokument.bestilling.consumer.dto.fornavnEtternavn
import no.nav.bidrag.dokument.bestilling.utils.BREVREF
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDD_INNTEKTGRENSE_2020_2021
import no.nav.bidrag.transport.dokumentmaler.PeriodeFraTom
import no.nav.bidrag.transport.person.PersonAdresseDto
import no.nav.bidrag.transport.person.PersonDto
import java.math.BigDecimal

fun BrevBestilling.validateKontaktInformasjon(
    enhetKontaktInfo: EnhetKontaktInfoDto,
    bm: PersonDto,
    bp: PersonDto,
    adresse: PersonAdresseDto,
    brevRef: String = BREVREF,
) {
    brev?.tknr shouldBe enhetKontaktInfo.enhetIdent
    brev?.spraak shouldBe "NB"
    brev?.brevref shouldBe brevRef

    brev?.kontaktInfo?.avsender?.navn shouldBe enhetKontaktInfo.enhetNavn
    brev?.kontaktInfo?.tlfAvsender?.telefonnummer shouldBe "55553333"
    brev?.kontaktInfo?.returAdresse?.enhet shouldBe "4806"
    brev?.kontaktInfo?.returAdresse?.navn shouldBe enhetKontaktInfo.enhetNavn
    brev?.kontaktInfo?.returAdresse?.adresselinje1 shouldBe enhetKontaktInfo.postadresse?.adresselinje1
    brev?.kontaktInfo?.returAdresse?.adresselinje2 shouldBe enhetKontaktInfo.postadresse?.adresselinje2
    brev?.kontaktInfo?.returAdresse?.postnummer shouldBe enhetKontaktInfo.postadresse?.postnummer
    brev?.kontaktInfo?.returAdresse?.poststed shouldBe enhetKontaktInfo.postadresse?.poststed
    brev?.kontaktInfo?.returAdresse?.land shouldBe ""
    brev?.kontaktInfo?.returAdresse?.shouldBeEqualToComparingFields(brev?.kontaktInfo?.postadresse as BrevKontaktinfo.Adresse)

    brev?.mottaker?.navn shouldBe bm.visningsnavn
    brev?.mottaker?.adresselinje1 shouldBe adresse.adresselinje1
    brev?.mottaker?.adresselinje2 shouldBe adresse.adresselinje2
    brev?.mottaker?.adresselinje3 shouldBe "3030 Drammen"
    brev?.mottaker?.boligNr shouldBe adresse.bruksenhetsnummer
    brev?.mottaker?.postnummer shouldBe adresse.postnummer
    brev?.mottaker?.spraak shouldBe "NB"
    brev?.mottaker?.rolle shouldBe "02"
    brev?.mottaker?.fodselsnummer shouldBe bm.ident.verdi
    brev?.mottaker?.fodselsdato shouldBe bm.fødselsdato

    brev?.parter?.bmfnr shouldBe bm.ident.verdi
    brev?.parter?.bmnavn shouldBe bm.fornavnEtternavn()
    brev?.parter?.bpfnr shouldBe bp.ident.verdi
    brev?.parter?.bpnavn shouldBe bp.fornavnEtternavn()
    brev?.parter?.bmfodselsdato shouldBe bm.fødselsdato
    brev?.parter?.bpfodselsdato shouldBe bp.fødselsdato
}

fun BidragBarn.validerInntekt(
    periode: PeriodeFraTom,
    index: Int,
    inntekt: Int,
    type: String,
    beskrivelse: String?,
) {
    val inntekterPeriode = hentInntektPerioder(periode)
    inntekterPeriode[index].inntektGrense shouldBe FORSKUDD_INNTEKTGRENSE_2020_2021
    inntekterPeriode[index].belopType shouldBe type
    inntekterPeriode[index].beskrivelse shouldBe beskrivelse
    inntekterPeriode[index].belopÅrsinntekt shouldBe BigDecimal(inntekt)
    inntekterPeriode[index].fomDato shouldBe periode.fraDato
    inntekterPeriode[index].tomDato shouldBe periode.tomDato
}
