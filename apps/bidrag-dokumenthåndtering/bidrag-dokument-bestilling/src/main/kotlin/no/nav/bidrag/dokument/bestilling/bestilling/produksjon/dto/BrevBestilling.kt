package no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement
import jakarta.xml.bind.annotation.adapters.XmlAdapter
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import no.nav.bidrag.dokument.bestilling.model.LANDKODE3_NORGE
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.ParseException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.GregorianCalendar

val BREV_DATETIME_FORMAT = DateTimeFormatter.ofPattern("ddMMyy")
val BREV_SOKNAD_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")

@XmlRootElement(name = "rtv-brev")
@XmlAccessorType(XmlAccessType.FIELD)
class BrevBestilling {
    @XmlAttribute
    var sysid: String = "BI12"

    @XmlAttribute
    var arkiver: String = "JA"

    @XmlAttribute
    var direkteutskrift: String = "NEI"

    @XmlAttribute
    var format: String = "ENSIDIG"

    @XmlAttribute
    var skriver: String = ""

    @XmlAttribute
    var skrivertype: String = "LOKAL"

    @XmlAttribute
    var skuff: String = ""

    @XmlAttribute
    lateinit var malpakke: String

    @XmlAttribute
    lateinit var passord: String

    @XmlAttribute
    lateinit var saksbehandler: String

    var brev: Brev? = null

    fun brev(init: Brev.() -> Unit): Brev {
        val initBrev = Brev()
        initBrev.init()
        brev = initBrev
        return initBrev
    }
}

@XmlRootElement(name = "brev")
@XmlAccessorType(XmlAccessType.FIELD)
class Brev {
    @XmlAttribute
    lateinit var tknr: String

    @XmlAttribute
    lateinit var spraak: String

    @XmlAttribute
    lateinit var brevref: String

    @XmlElement(name = "brevMottaker")
    var mottaker: BrevMottaker? = null

    @XmlElement(name = "parter")
    var parter: Parter? = null

    @XmlElement(name = "barniSak")
    var barnISak: MutableList<BarnISak> = mutableListOf()

    @XmlElement(name = "soknad")
    var soknad: Soknad? = null

    @XmlElement(name = "bidrVtak")
    var vedtak: MutableList<BidragVedtak> = mutableListOf()

    @XmlElement(name = "soknBost")
    var soknadBost: SoknadBost? = null

    @XmlElement(name = "perForskVtak")
    var forskuddVedtakPeriode: MutableList<ForskuddVedtakPeriode> = mutableListOf()

    @XmlElement(name = "Kontaktinfo")
    var kontaktInfo: BrevKontaktinfo? = null

    @XmlElement(name = "Saksbehandl")
    var saksbehandler: BrevSaksbehandler? = null

    @XmlElement(name = "sjablong")
    var sjablon: BrevSjablon? = null

    @XmlElement(name = "bidrBarn")
    var bidragBarn: MutableList<BidragBarn> = mutableListOf()

    @XmlElement(name = "perInGrForsk", nillable = true)
    var inntektGrunnlagForskuddPerioder: MutableList<InntektGrunnlagForskuddPeriode> = mutableListOf()

    fun inntektGrunnlagForskuddPeriode(init: InntektGrunnlagForskuddPeriode.() -> Unit): InntektGrunnlagForskuddPeriode {
        val initValue = InntektGrunnlagForskuddPeriode()
        initValue.init()
        inntektGrunnlagForskuddPerioder.add(initValue)
        return initValue
    }

    fun brevKontaktinfo(init: BrevKontaktinfo.() -> Unit): BrevKontaktinfo {
        val brevKontaktinfo = BrevKontaktinfo()
        brevKontaktinfo.init()
        kontaktInfo = brevKontaktinfo
        return brevKontaktinfo
    }

    fun brevSaksbehandler(init: BrevSaksbehandler.() -> Unit): BrevSaksbehandler {
        val brevSaksbehandler = BrevSaksbehandler()
        brevSaksbehandler.init()
        saksbehandler = brevSaksbehandler
        return brevSaksbehandler
    }

    fun sjablon(init: BrevSjablon.() -> Unit): BrevSjablon {
        val initSjablon = BrevSjablon()
        initSjablon.init()
        sjablon = initSjablon
        return initSjablon
    }

    fun soknad(init: Soknad.() -> Unit): Soknad {
        val initSoknad = Soknad()
        initSoknad.init()
        soknad = initSoknad
        return initSoknad
    }

    fun soknadBost(init: SoknadBost.() -> Unit): SoknadBost {
        val initSoknadBost = SoknadBost()
        initSoknadBost.init()
        soknadBost = initSoknadBost
        return initSoknadBost
    }

    fun barnISak(init: BarnISak.() -> Unit): BarnISak {
        val initBarnISak = BarnISak()
        initBarnISak.init()
        barnISak.add(initBarnISak)
        return initBarnISak
    }

    fun brevmottaker(init: BrevMottaker.() -> Unit): BrevMottaker {
        val brevMottaker = BrevMottaker()
        brevMottaker.init()
        mottaker = brevMottaker
        return brevMottaker
    }

    fun parter(init: Parter.() -> Unit): Parter {
        val initParter = Parter()
        initParter.init()
        parter = initParter
        return initParter
    }

    fun vedtak(init: BidragVedtak.() -> Unit): BidragVedtak {
        val initVedtak = BidragVedtak()
        initVedtak.init()
        vedtak.add(initVedtak)
        return initVedtak
    }

    fun forskuddVedtak(init: ForskuddVedtakPeriode.() -> Unit): ForskuddVedtakPeriode {
        val initValue = ForskuddVedtakPeriode()
        initValue.init()
        forskuddVedtakPeriode.add(initValue)
        return initValue
    }

    fun bidragBarn(init: BidragBarn.() -> Unit): BidragBarn {
        val initValue = BidragBarn()
        initValue.init()
        bidragBarn.add(initValue)
        return initValue
    }
}

@Suppress("unused")
@XmlRootElement(name = "sjablong")
@XmlAccessorType(XmlAccessType.FIELD)
class BrevSjablon {
    @XmlElement(name = "forskSats", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var forskuddSats: BigDecimal? = null

    @XmlElement(name = "inntIntTb", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var inntektTillegsbidrag: BigDecimal? = null

    @XmlElement(name = "prosInntBp", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var maksProsentInntektBp: BigDecimal? = null

    @XmlElement(name = "multHoyInnt", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var multiplikatorHøyInntektBp: BigDecimal? = null

    @XmlElement(name = "multMaxBidr", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var multiplikatorMaksBidrag: BigDecimal? = null

    @XmlElement(name = "multMaxInnt", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var multiplikatorMaksInntekBarn: BigDecimal? = null

    @XmlElement(name = "multMaxForsk", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var multiplikatorInntekstgrenseForskudd: BigDecimal? = null

    @XmlElement(name = "multInntGeb", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var nedreInntekstgrenseGebyr: BigDecimal? = null

    @XmlElement(name = "prosTb", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var prosentTillegsgebyr: BigDecimal? = null

    @XmlElement(name = "belHoyInnt", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var maksgrenseHøyInntekt: BigDecimal? = null

    @XmlElement(name = "belMaxBidr", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var maksBidragsgrense: BigDecimal? = null

    @XmlElement(name = "belMaxInnt", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var maksInntektsgrense: BigDecimal? = null

    @XmlElement(name = "belMaxForsk", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var maksForskuddsgrense: BigDecimal? = null

    @XmlElement(name = "belInntGeb", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var maksInntektsgebyr: BigDecimal? = null
}

@Suppress("unused")
@XmlRootElement(name = "Saksbehandl")
@XmlAccessorType(XmlAccessType.FIELD)
class BrevSaksbehandler {
    @XmlElement(name = "saksbNavn")
    var navn: String? = null
}

@XmlRootElement(name = "brevMottaker")
@XmlAccessorType(XmlAccessType.FIELD)
class BrevMottaker {
    @XmlElement(name = "navn", nillable = true)
    var navn: String? = null

    @XmlElement(name = "adr1", nillable = true)
    var adresselinje1: String? = null

    @XmlElement(name = "adr2", nillable = true)
    var adresselinje2: String? = null

    @XmlElement(name = "adr3", nillable = true)
    var adresselinje3: String? = null

    @XmlElement(name = "adr4", nillable = true)
    var adresselinje4: String? = null

    @XmlElement(name = "boligNr", nillable = true)
    var boligNr: String? = null

    @XmlElement(name = "bidrRolle", nillable = true)
    var rolle: String? = null

    @XmlElement(name = "fnr", nillable = true)
    var fodselsnummer: String? = null

    @XmlElement(name = "fdato", nillable = true)
    @XmlJavaTypeAdapter(BirthDateAdapter::class)
    var fodselsdato: LocalDate? = null

    @XmlElement(name = "postnr", nillable = true)
    var postnummer: String? = null

    @XmlElement(name = "landKd", nillable = true)
    @XmlJavaTypeAdapter(LandkodeAdapter::class)
    var landkode: String? = null

    @XmlElement(name = "spraak", nillable = true)
    var spraak: String? = null
}

@Suppress("unused")
@XmlRootElement(name = "Kontaktinfo")
@XmlAccessorType(XmlAccessType.FIELD)
class BrevKontaktinfo {
    @XmlElement(name = "NavnAvsender", nillable = true)
    var avsender: Avsender? = null

    @XmlElement(name = "TelfAvsender", nillable = true)
    var tlfAvsender: TlfAvsender? = null

    @XmlElement(name = "Besoksadr", nillable = true)
    var besøkAdresse: Adresse? = null

    @XmlElement(name = "Returadr", nillable = true)
    var returAdresse: Adresse? = null

    @XmlElement(name = "Postadr", nillable = true)
    var postadresse: Adresse? = null

    @XmlRootElement(name = "NavnAvsender")
    @XmlAccessorType(XmlAccessType.FIELD)
    class Avsender {
        @XmlElement(name = "NavnAvsEnh", nillable = true)
        var navn: String? = null
    }

    @XmlRootElement(name = "TelfAvsender")
    @XmlAccessorType(XmlAccessType.FIELD)
    class TlfAvsender {
        @XmlElement(name = "TelfAvsEnh", nillable = true)
        var telefonnummer: String? = null
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    class Adresse {
        @XmlElement(name = "NavEnhId", nillable = true)
        var enhet: String? = null

        @XmlElement(name = "NavEnhNavn", nillable = true)
        var navn: String? = null

        @XmlElement(name = "Telefon", nillable = true)
        var telefon: String? = null

        @XmlElement(name = "AdrLinje1", nillable = true)
        var adresselinje1: String? = null

        @XmlElement(name = "AdrLinje2", nillable = true)
        var adresselinje2: String? = null

        @XmlElement(name = "PostNr", nillable = true)
        var postnummer: String? = null

        @XmlElement(name = "PostSted", nillable = true)
        var poststed: String? = null

        @XmlElement(name = "Land", nillable = true)
        var land: String? = null
    }

    fun returOgPostadresse(init: Adresse.() -> Unit): Adresse {
        val adresse = Adresse()
        adresse.init()
        returAdresse = adresse
        postadresse = adresse
        besøkAdresse = adresse
        return adresse
    }

    fun tlfAvsender(init: TlfAvsender.() -> Unit): TlfAvsender {
        val tlfAvsender = TlfAvsender()
        tlfAvsender.init()
        return tlfAvsender
    }

    fun avsender(init: Avsender.() -> Unit): Avsender {
        val avsender = Avsender()
        avsender.init()
        return avsender
    }
}

@Suppress("unused")
@XmlRootElement(name = "parter")
@XmlAccessorType(XmlAccessType.FIELD)
class Parter {
    // Bidragspliktig (BP)
    @XmlElement(name = "bpfnr", nillable = true)
    var bpfnr: String? = null

    @XmlElement(name = "bpNavn", nillable = true)
    var bpnavn: String? = null

    @XmlElement(name = "bpfDato", nillable = true)
    @XmlJavaTypeAdapter(BirthDateAdapter::class)
    var bpfodselsdato: LocalDate? = null

    @XmlElement(name = "bpKravFremAv", nillable = true)
    var bpkravfremav: String? = null

    @XmlElement(name = "bpbelopGebyr", nillable = true)
    @XmlJavaTypeAdapter(BelopDecimalAdapter::class)
    var bpgebyr: BigDecimal? = null

    @XmlElement(name = "bpLandKd", nillable = true)
    @XmlJavaTypeAdapter(LandkodeAdapter::class)
    var bplandkode: String? = null

    @XmlElement(name = "bpDatoDod", nillable = true)
    @XmlJavaTypeAdapter(BirthDateAdapter::class)
    var bpdatodod: LocalDate? = null

    // Bidragsmottaker (BM)
    @XmlElement(name = "bmfnr", nillable = true)
    var bmfnr: String? = null

    @XmlElement(name = "bmNavn", nillable = true)
    var bmnavn: String? = null

    @XmlElement(name = "bmfDato", nillable = true)
    @XmlJavaTypeAdapter(BirthDateAdapter::class)
    var bmfodselsdato: LocalDate? = null

    @XmlElement(name = "bmKravFremAv", nillable = true)
    var bmkravkfremav: String? = null

    @XmlElement(name = "bmbelopGebyr", nillable = true)
    @XmlJavaTypeAdapter(BelopDecimalAdapter::class)
    var bmgebyr: BigDecimal? = null

    @XmlElement(name = "bmLandKd", nillable = true)
    @XmlJavaTypeAdapter(LandkodeAdapter::class)
    var bmlandkode: String? = null

    @XmlElement(name = "bmDatoDod", nillable = true)
    @XmlJavaTypeAdapter(BirthDateAdapter::class)
    var bmdatodod: LocalDate? = null
}

@Suppress("unused")
@XmlRootElement(name = "soknad")
@XmlAccessorType(XmlAccessType.FIELD)
class Soknad {
    @XmlElement(name = "soknDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var soknDato: LocalDate? = null

    @XmlElement(name = "type", nillable = true)
    var type: String? = null

    @XmlElement(name = "vedtDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var vedtattDato: LocalDate? = null

    @XmlElement(name = "virknDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var virkningDato: LocalDate? = null

    @XmlElement(name = "aarsakKd", nillable = true)
    var aarsakKd: String? = null

    @XmlElement(name = "undergrp", nillable = true)
    var undergrp: String? = null

    @XmlElement(name = "saksnr", nillable = true)
    var saksnr: String? = null

    @XmlElement(name = "svarfrDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var svarfrDato: LocalDate? = null

    @XmlElement(name = "sendtDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var sendtDato: LocalDate? = null

    @XmlElement(name = "saksTypeOmr", nillable = true)
    var saksTypeOmr: String? = null
}

@Suppress("unused")
@XmlRootElement(name = "soknBost")
@XmlAccessorType(XmlAccessType.FIELD)
class SoknadBost {
    @XmlElement(name = "saksnr", nillable = true)
    var saksnr: String? = null

    @XmlElement(name = "BBFogd", nillable = true)
    var bbfogd: String? = null

    @XmlElement(name = "BPFogd", nillable = true)
    var bpfogd: String? = null

    @XmlElement(name = "sakstype", nillable = true)
    var sakstype: String? = null

    @XmlElement(name = "jounalkode", nillable = true)
    var bpgebyr: String? = null

    @XmlElement(name = "indexRegDato", nillable = true)
    var indexRegDato: String? = null // TODO: Date format?

    @XmlElement(name = "indexRegPro", nillable = true)
    var indexRegPro: String? = null

    @XmlElement(name = "hgKode", nillable = true)
    var hgKode: String? = null

    @XmlElement(name = "ugKode", nillable = true)
    var ugKode: String? = null

    @XmlElement(name = "datoSakReg", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var datoSakReg: LocalDate? = null

    @XmlElement(name = "resKode", nillable = true)
    var resKode: String? = null

    @XmlElement(name = "datoVtak", nillable = true)
    var datoVedtak: String? = null

    @XmlElement(name = "rmISak", nillable = true)
    @XmlJavaTypeAdapter(BooleanAdapter::class)
    var rmISak: Boolean? = false

    @XmlElement(name = "forskUtBet", nillable = true)
    @XmlJavaTypeAdapter(BooleanAdapter::class)
    var forskUtBet: Boolean? = false

    @XmlElement(name = "sendtDato")
    @XmlJavaTypeAdapter(DateAdapter::class)
    var sendtDato: LocalDate? = LocalDate.now()

    @XmlElement(name = "gebyrsats", nillable = true)
    @XmlJavaTypeAdapter(BelopDecimalSatsAdapter::class)
    var gebyrsats: BigDecimal? = null

    @XmlElement(name = "innkrSamtid")
    @XmlJavaTypeAdapter(BooleanAdapter::class)
    var innkrSamtid: Boolean? = false

    @XmlElement(name = "mottDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var mottatDato: LocalDate? = null

    @XmlElement(name = "virknDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var virkningsDato: LocalDate? = null

    @XmlElement(name = "myndighet", nillable = true)
    var myndighet: String? = null

    @XmlElement(name = "ffuRefNr", nillable = true)
    var ffuRefNr: String? = null

    @XmlElement(name = "konv", nillable = true)
    var konv: String? = null

    @XmlElement(name = "soknGrKode", nillable = true)
    var soknGrKode: String? = null

    @XmlElement(name = "soknFraKode", nillable = true)
    var soknFraKode: String? = null

    @XmlElement(name = "soknType", nillable = true)
    var soknType: String? = null

    @XmlElement(name = "b4Kode", nillable = true)
    var b4Kode: String? = null

    @XmlElement(name = "b4Belop", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var b4Belop: BigDecimal? = null
}

@Suppress("unused")
@XmlRootElement(name = "barniSak")
@XmlAccessorType(XmlAccessType.FIELD)
class BarnISak {
    @XmlElement(name = "fnr", nillable = true)
    var fnr: String? = null

    @XmlElement(name = "fornavn", nillable = true)
    var fornavn: String? = null

    @XmlElement(name = "fDato", nillable = true)
    @XmlJavaTypeAdapter(BirthDateAdapter::class)
    var fDato: LocalDate? = null

    @XmlElement(name = "navn", nillable = true)
    var navn: String? = null

    @XmlElement(name = "personIdRm", nillable = true)
    var personIdRm: String? = null

    @XmlElement(name = "belopGebyrRm", nillable = true)
    var belopGebyrRm: String? = null

    @XmlElement(name = "belForskudd", nillable = true)
    @XmlJavaTypeAdapter(BelopDecimalAdapter::class)
    var belForskudd: BigDecimal? = null

    @XmlElement(name = "belBidrag", nillable = true)
    @XmlJavaTypeAdapter(BelopDecimalAdapter::class)
    var belBidrag: BigDecimal? = null
}

@DslMarker
annotation class BrevBestillingMarker

@BrevBestillingMarker
fun brevbestilling(init: BrevBestilling.() -> Unit): BrevBestilling {
    val brevBestilling = BrevBestilling()
    brevBestilling.init()
    return brevBestilling
}

class BirthDateAdapter : XmlAdapter<String, LocalDate?>() {
    override fun marshal(date: LocalDate?): String? = date?.format(BREV_DATETIME_FORMAT)

    @Throws(ParseException::class)
    override fun unmarshal(v: String): LocalDate = LocalDate.parse(v, BREV_DATETIME_FORMAT)
}

class DateAdapter : XmlAdapter<String, LocalDate?>() {
    override fun marshal(date: LocalDate?): String? = date?.format(BREV_SOKNAD_DATETIME_FORMAT)

    @Throws(ParseException::class)
    override fun unmarshal(v: String): LocalDate = LocalDate.parse(v, BREV_SOKNAD_DATETIME_FORMAT)
}

class PeriodDateAdapter : XmlAdapter<String, LocalDate?>() {
    fun getLastDayOfPreviousMonth(date: LocalDate): LocalDate {
        if (date.year == 9999) return date
        val cDate = GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault()))
        // Subtract one month from the current date
        cDate.add(Calendar.MONTH, -1)
        // Set the day of the month to the last day of the month
        cDate.set(Calendar.DAY_OF_MONTH, cDate.getActualMaximum(Calendar.DAY_OF_MONTH))
        return LocalDate.from(cDate.toZonedDateTime())
    }

    override fun marshal(date: LocalDate?): String? {
        val fromDate = date?.let { getLastDayOfPreviousMonth(it) } ?: LocalDate.parse("9999-12-31")
        return fromDate.format(BREV_SOKNAD_DATETIME_FORMAT)
    }

    @Throws(ParseException::class)
    override fun unmarshal(v: String): LocalDate = LocalDate.parse(v, BREV_SOKNAD_DATETIME_FORMAT)
}

class BelopAdapterToDesimaler : XmlAdapter<String, BigDecimal?>() {
    override fun marshal(value: BigDecimal?): String? =
        value
            ?.round(MathContext(10))
            ?.multiply(BigDecimal(100))
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.toString()
            ?.padStart(13, '0')
            ?.let { it.substring(0, 11) + "." + it.substring(11) } ?: "00000000000"

    @Throws(ParseException::class)
    override fun unmarshal(value: String?): BigDecimal? = value?.toBigDecimal()
}

class BelopAdapter : XmlAdapter<String, BigDecimal?>() {
    override fun marshal(value: BigDecimal?): String? =
        value
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.toBigInteger()
            ?.toString()
            ?.padStart(11, '0')

    @Throws(ParseException::class)
    override fun unmarshal(value: String?): BigDecimal? = value?.toBigDecimal()
}

class BelopDecimalSatsAdapter : XmlAdapter<String, BigDecimal?>() {
    override fun marshal(value: BigDecimal?): String? = value?.setScale(1, RoundingMode.FLOOR).toString()?.padStart(7, '0')

    @Throws(ParseException::class)
    override fun unmarshal(value: String?): BigDecimal? = value?.toBigDecimal()
}

class BelopNoDecimalAdapter : XmlAdapter<String, BigDecimal?>() {
    override fun marshal(value: BigDecimal?): String? =
        value
            ?.multiply(BigDecimal.TEN)
            ?.setScale(0, RoundingMode.FLOOR)
            ?.divide(BigDecimal.TEN)
            ?.toString()
            ?.padStart(11, '0')

    @Throws(ParseException::class)
    override fun unmarshal(value: String?): BigDecimal? = value?.toBigDecimal()
}

class BelopDecimalAdapter : XmlAdapter<String, BigDecimal?>() {
    override fun marshal(value: BigDecimal?): String? = value?.setScale(2, RoundingMode.FLOOR).toString()?.padStart(11, '0')

    @Throws(ParseException::class)
    override fun unmarshal(value: String?): BigDecimal? = value?.toBigDecimal()
}

class PercentageAdapter : XmlAdapter<String, BigDecimal?>() {
    override fun marshal(value: BigDecimal?): String =
        value
            ?.round(MathContext(10))
            ?.multiply(BigDecimal(100))
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.toString()
            ?.padStart(5, '0')
            ?.let { it.substring(0, 4) + "." + it.substring(4) } ?: "0000"

    @Throws(ParseException::class)
    override fun unmarshal(value: String?): BigDecimal? = value?.toBigDecimal()
}

class NumberAdapter : XmlAdapter<String, Int?>() {
    override fun marshal(value: Int?): String = value?.toString()?.padStart(2, '0') ?: "00"

    @Throws(ParseException::class)
    override fun unmarshal(value: String?): Int? = value?.toInt()
}

class LandkodeAdapter : XmlAdapter<String, String?>() {
    override fun marshal(landkode: String?): String? = if (landkode.isNullOrEmpty() || landkode == LANDKODE3_NORGE) null else landkode

    @Throws(ParseException::class)
    override fun unmarshal(landkode: String): String = landkode
}

class BooleanAdapter : XmlAdapter<String, Boolean?>() {
    override fun marshal(v: Boolean?): String = if (v == true) "J" else "N"

    @Throws(ParseException::class)
    override fun unmarshal(v: String): Boolean = v == "J"
}
