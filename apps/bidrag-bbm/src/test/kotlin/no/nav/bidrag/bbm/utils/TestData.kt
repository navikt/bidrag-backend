package no.nav.bidrag.bbm.utils

import no.nav.bidrag.bbm.model.konverterTilBisyskode
import no.nav.bidrag.bbm.persistence.bbm.entity.PeriodeBidrag
import no.nav.bidrag.bbm.persistence.bbm.entity.Samvær
import no.nav.bidrag.bbm.persistence.bisys.entity.Blankett
import no.nav.bidrag.bbm.persistence.bisys.entity.KodeSøknadStatus
import no.nav.bidrag.bbm.persistence.bisys.entity.Rolle
import no.nav.bidrag.bbm.persistence.bisys.entity.Søknad
import no.nav.bidrag.bbm.persistence.bisys.entity.Søknadslinje
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import java.math.BigDecimal
import java.time.LocalDate

val PERSONIDENT_BARN_1 = "12345678910"
val PERSONIDENT_BARN_2 = "22345678910"
val PERSONIDENT_BARN_3 = "32345678910"
val PERSONIDENT_BARN_4 = "42345678910"

val SAKSNUMMER_1 = "1234567"
val SAKSNUMMER_2 = "2234567"
val SAKSNUMMER_3 = "3234567"
val SAKSBEHANDLER_IDENT = "Z999999"

val DATO_SØKNAD_SØKNAD_1 = LocalDate.parse("2024-01-01")
val DATO_SØKNAD_SØKNAD_2 = LocalDate.parse("2024-05-01")

val PERSONIDENT_BP_1 = "98765432109"
val PERSONIDENT_BP_2 = "87654321098"
val PERSONIDENT_BM_1 = "76543210987"
val PERSONIDENT_BM_2 = "65432109876"
val PERSONIDENT_BM_3 = genererFødselsnummer()

fun lagTestdataPeriodeBidrag(): List<PeriodeBidrag> = listOf(
    opprettPeriodeBidrag(saksnummer = SAKSNUMMER_1, barnId = PERSONIDENT_BARN_1),
    opprettPeriodeBidrag(
        saksnummer = SAKSNUMMER_1,
        barnId = PERSONIDENT_BARN_1,
        datoSøknad = LocalDate.parse("2024-05-02"),
        faktiskBeløp = BigDecimal(1000),
    ),
    opprettPeriodeBidrag(
        saksnummer = SAKSNUMMER_1,
        barnId = PERSONIDENT_BARN_1,
        datoSøknad = LocalDate.parse("2024-07-02"),
        faktiskBeløp = BigDecimal(1040),
    ),
    opprettPeriodeBidrag(saksnummer = SAKSNUMMER_2, barnId = PERSONIDENT_BARN_2),
    opprettPeriodeBidrag(
        saksnummer = SAKSNUMMER_2,
        barnId = PERSONIDENT_BARN_2,
        datoSøknad = LocalDate.parse("2024-02-02"),
        faktiskBeløp = BigDecimal(1000),
    ),
    opprettPeriodeBidrag(
        saksnummer = SAKSNUMMER_2,
        barnId = PERSONIDENT_BARN_2,
        datoSøknad = LocalDate.parse("2024-04-02"),
        faktiskBeløp = BigDecimal(1040),
    ),
)

fun lageTestdataSamvær() = listOf(
    opprettSamvær(saksnummer = SAKSNUMMER_1, barnId = PERSONIDENT_BARN_1),
    opprettSamvær(
        saksnummer = SAKSNUMMER_1,
        barnId = PERSONIDENT_BARN_1,
        datoSøknad = LocalDate.parse("2024-05-02"),
        samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
    ),
    opprettSamvær(
        saksnummer = SAKSNUMMER_1,
        barnId = PERSONIDENT_BARN_1,
        datoSøknad = LocalDate.parse("2024-04-02"),
        samværskode = Samværsklasse.SAMVÆRSKLASSE_3,
    ),
    opprettSamvær(saksnummer = SAKSNUMMER_2, barnId = PERSONIDENT_BARN_2),
    opprettSamvær(
        saksnummer = SAKSNUMMER_2,
        barnId = PERSONIDENT_BARN_2,
        datoSøknad = LocalDate.parse("2024-05-02"),
        samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
    ),
    opprettSamvær(
        saksnummer = SAKSNUMMER_2,
        barnId = PERSONIDENT_BARN_2,
        datoSøknad = LocalDate.parse("2024-04-02"),
        samværskode = Samværsklasse.SAMVÆRSKLASSE_3,
    ),
)

fun lageSøknadTestdata() = listOf(
    opprettSøknad(saksnummer = SAKSNUMMER_1, blankettid = 1L, søknadMottattDato = DATO_SØKNAD_SØKNAD_1),
    opprettSøknad(saksnummer = SAKSNUMMER_2, blankettid = 1L, søknadMottattDato = DATO_SØKNAD_SØKNAD_2),
)

fun opprettPeriodeBidrag(
    saksnummer: String = SAKSNUMMER_1,
    datoSøknad: LocalDate = DATO_SØKNAD_SØKNAD_1,
    datoFom: LocalDate = LocalDate.parse("2024-07-01"),
    barnId: String = PERSONIDENT_BARN_1,
    faktiskBeløp: BigDecimal = BigDecimal(1030),
    søknadstype: Stønadstype = Stønadstype.BIDRAG,
) = PeriodeBidrag(
    saksnummer = saksnummer,
    datoSøknad = datoSøknad,
    datoFom = datoFom,
    personidentBarn = barnId,
    beregnetBeløp = BigDecimal(10024),
    faktiskBeløp = faktiskBeløp,
    beløpSamvær = BigDecimal(100),
    soknadstype = søknadstype.konverterTilBisyskode().first(),
)

fun opprettPeriodeBidragKomplett(
    saksnummer: String = SAKSNUMMER_1,
    datoSøknad: LocalDate = DATO_SØKNAD_SØKNAD_1,
    datoFom: LocalDate = LocalDate.parse("2024-07-01"),
    barnId: String = PERSONIDENT_BARN_1,
    beregnetBeløp: BigDecimal = BigDecimal(1050),
    faktiskBeløp: BigDecimal = BigDecimal(1030),
    beløpSamvær: BigDecimal = BigDecimal(500),
    søknadstype: Stønadstype = Stønadstype.BIDRAG,
) = PeriodeBidrag(
    saksnummer = saksnummer,
    datoSøknad = datoSøknad,
    datoFom = datoFom,
    personidentBarn = barnId,
    beregnetBeløp = beregnetBeløp,
    faktiskBeløp = faktiskBeløp,
    beløpSamvær = beløpSamvær,
    soknadstype = søknadstype.konverterTilBisyskode().first(),
)

fun opprettSamvær(
    saksnummer: String = SAKSNUMMER_1,
    datoSøknad: LocalDate = DATO_SØKNAD_SØKNAD_1,
    datoFom: LocalDate = LocalDate.parse("2024-07-01"),
    barnId: String = PERSONIDENT_BARN_1,
    samværskode: Samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1,
    søknadstype: Stønadstype = Stønadstype.BIDRAG,
) = Samvær(
    saksnummer = saksnummer,
    datoSøknad = datoSøknad,
    datoFom = datoFom,
    datoTom = LocalDate.MAX,
    personidentBarn = barnId,
    soknadstype = søknadstype.konverterTilBisyskode().first(),
    samværskode = samværskode.bisysKode,
)

fun opprettSamværKomplett(
    saksnummer: String = SAKSNUMMER_1,
    datoSøknad: LocalDate = DATO_SØKNAD_SØKNAD_1,
    datoFom: LocalDate = LocalDate.parse("2024-07-01"),
    datoTom: LocalDate? = null,
    barnId: String = PERSONIDENT_BARN_1,
    samværskode: Samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1,
    søknadstype: Stønadstype = Stønadstype.BIDRAG,
) = Samvær(
    saksnummer = saksnummer,
    datoSøknad = datoSøknad,
    datoFom = datoFom,
    datoTom = datoTom,
    personidentBarn = barnId,
    soknadstype = søknadstype.konverterTilBisyskode().first(),
    samværskode = samværskode.bisysKode,
)

fun opprettSøknad(
    søknadsid: Long? = null,
    blankettid: Long,
    søknadMottattDato: LocalDate = LocalDate.parse("2024-01-01"),
    søknadFomDato: LocalDate? = LocalDate.parse("2024-01-01"),
    søknadsgruppekode: String = "BI",
    saksnummer: String = SAKSNUMMER_1,
    behanderenhet: String = "4608",
    behandlingsid: String? = null,
) = Søknad(
    søknadsid = søknadsid,
    blankettid = blankettid,
    søknadMottattDato = søknadMottattDato,
    søknadFomDato = søknadFomDato,
    søknadsgruppekode = søknadsgruppekode,
    saksnummer = saksnummer,
    behandlerenhet = behanderenhet,
    behandlingsid = behandlingsid,
)

fun opprettRolle(
    rolleid: Long? = null,
    saksnummer: String,
    fnr: String?,
    rolletype: String,
) = Rolle(
    rolleid = rolleid,
    saksnummer = saksnummer,
    fnr = fnr,
    rolletype = rolletype,
)

fun opprettBlankett(
    blankettid: Long? = null,
    saksnummer: String,
    søknadFraKode: String,
    søknadstype: String,
) = Blankett(
    blankettid = blankettid,
    saksnummer = saksnummer,
    søknadFraKode = søknadFraKode,
    søknadstype = søknadstype,
)

fun opprettSøknadslinje(
    søknadslinjeid: Long? = null,
    søknadsid: Long,
    rolleid: Long,
    innbetaltBeløp: BigDecimal?,
    søknadsstatuskode: String,
    gruppeKombinasjonskode: String,
    saksnummer: String,
    referanseGebyr: String? = null,
) = Søknadslinje(
    søknadslinjeid = søknadslinjeid,
    søknadsid = søknadsid,
    rolleid = rolleid,
    innbetaltBeløp = innbetaltBeløp,
    søknadStatuskode = søknadsstatuskode,
    gruppeKombinasjonskode = gruppeKombinasjonskode,
    saksnummer = saksnummer,
    engangsbeløpReferanse = referanseGebyr,
)

fun lageKodeSøknadStatus() = listOf(
    opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
    opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
)

fun opprettKodeSøknadStatus(
    kode: String,
    lukketStatus: String,
) = KodeSøknadStatus(
    kode = kode,
    lukketStatus = lukketStatus,
)
