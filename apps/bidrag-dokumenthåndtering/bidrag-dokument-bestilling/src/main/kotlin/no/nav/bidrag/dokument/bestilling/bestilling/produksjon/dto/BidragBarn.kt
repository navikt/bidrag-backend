package no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import java.math.BigDecimal
import java.time.LocalDate

@Suppress("unused")
@XmlRootElement(name = "bidrBarn")
@XmlAccessorType(XmlAccessType.FIELD)
class BidragBarn {
    @XmlElement(name = "barn", nillable = true)
    var barn: Barn? = null

    @XmlElement(name = "perInntekt", nillable = true)
    var inntektPerioder: MutableList<InntektPeriode> = mutableListOf()

    @XmlElement(name = "perForskBarn", nillable = true)
    var forskuddBarnPerioder: MutableList<ForskuddBarnPeriode> = mutableListOf()

    @XmlElement(name = "perForskSiv", nillable = true)
    var forskuddSivilstandPerioder: MutableList<ForskuddSivilstandPeriode> = mutableListOf()

    @XmlElement(name = "perForskVtak", nillable = true)
    var forskuddVedtakPerioder: MutableList<ForskuddVedtakPeriode> = mutableListOf()

    @XmlElement(name = "perInGrForsk", nillable = true)
    var inntektGrunnlagForskuddPerioder: MutableList<InntektGrunnlagForskuddPeriode> = mutableListOf()

    @XmlElement(name = "perAndelU", nillable = true)
    var andelUnderholdPerioder: MutableList<AndelUnderholdPeriode> = mutableListOf()

    @XmlElement(name = "perUkost", nillable = true)
    var underholdkostnadPerioder: MutableList<UnderholdkostnadPeriode> = mutableListOf()

    @XmlElement(name = "perSamvaer", nillable = true)
    var samværPerioder: MutableList<SamvarPeriode> = mutableListOf()

    @XmlElement(name = "perBidrEvne", nillable = true)
    var bidragEvnePerioder: MutableList<BidragEvnePeriode> = mutableListOf()

    @XmlElement(name = "sertilsk", nillable = true)
    var særbidrag: Særbidrag? = null

    @XmlElement(name = "perSertilsk", nillable = true)
    val særbidragPeriode: MutableList<SærbidragPeriode> = mutableListOf()

    @XmlElement(name = "bidrVtak")
    var vedtak: MutableList<BidragVedtak> = mutableListOf()

    fun barn(init: Barn.() -> Unit): Barn {
        val initValue = Barn()
        initValue.init()
        barn = initValue
        return initValue
    }

    fun vedtakBarn(init: BidragVedtak.() -> Unit): BidragVedtak {
        val initVedtak = BidragVedtak()
        initVedtak.init()
        vedtak.add(initVedtak)
        return initVedtak
    }

    fun særbidrag(init: Særbidrag.() -> Unit): Særbidrag {
        val initValue = Særbidrag()
        initValue.init()
        særbidrag = initValue
        return initValue
    }

    fun særbidragPeriode(init: SærbidragPeriode.() -> Unit): SærbidragPeriode {
        val initValue = SærbidragPeriode()
        initValue.init()
        særbidragPeriode.add(initValue)
        return initValue
    }

    fun forskuddBarnPeriode(init: ForskuddBarnPeriode.() -> Unit): ForskuddBarnPeriode {
        val initValue = ForskuddBarnPeriode()
        initValue.init()
        forskuddBarnPerioder.add(initValue)
        return initValue
    }

    fun inntektPeriode(init: InntektPeriode.() -> Unit): InntektPeriode {
        val initValue = InntektPeriode()
        initValue.init()
        inntektPerioder.add(initValue)
        return initValue
    }

    fun forskuddSivilstandPeriode(init: ForskuddSivilstandPeriode.() -> Unit): ForskuddSivilstandPeriode {
        val initValue = ForskuddSivilstandPeriode()
        initValue.init()
        forskuddSivilstandPerioder.add(initValue)
        return initValue
    }

    fun forskuddVedtakPeriode(init: ForskuddVedtakPeriode.() -> Unit): ForskuddVedtakPeriode {
        val initValue = ForskuddVedtakPeriode()
        initValue.init()
        forskuddVedtakPerioder.add(initValue)
        return initValue
    }

    fun inntektGrunnlagForskuddPeriode(init: InntektGrunnlagForskuddPeriode.() -> Unit): InntektGrunnlagForskuddPeriode {
        val initValue = InntektGrunnlagForskuddPeriode()
        initValue.init()
        inntektGrunnlagForskuddPerioder.add(initValue)
        return initValue
    }

    fun andelUnderholdPeriode(init: AndelUnderholdPeriode.() -> Unit): AndelUnderholdPeriode {
        val initValue = AndelUnderholdPeriode()
        initValue.init()
        andelUnderholdPerioder.add(initValue)
        return initValue
    }

    fun underholdKostnadPeriode(init: UnderholdkostnadPeriode.() -> Unit): UnderholdkostnadPeriode {
        val initValue = UnderholdkostnadPeriode()
        initValue.init()
        underholdkostnadPerioder.add(initValue)
        return initValue
    }

    fun samværPeriode(init: SamvarPeriode.() -> Unit): SamvarPeriode {
        val initValue = SamvarPeriode()
        initValue.init()
        samværPerioder.add(initValue)
        return initValue
    }

    fun bidragEvnePeriode(init: BidragEvnePeriode.() -> Unit): BidragEvnePeriode {
        val initValue = BidragEvnePeriode()
        initValue.init()
        bidragEvnePerioder.add(initValue)
        return initValue
    }
}

@Suppress("unused")
@XmlRootElement(name = "perForskBarn")
@XmlAccessorType(XmlAccessType.FIELD)
class ForskuddBarnPeriode {
    @XmlElement(name = "fomDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var fomDato: LocalDate? = null

    @XmlElement(name = "tomDato", nillable = true)
    @XmlJavaTypeAdapter(PeriodDateAdapter::class)
    var tomDato: LocalDate? = null

    @XmlElement(name = "antBarn", nillable = true)
    @XmlJavaTypeAdapter(NumberAdapter::class)
    var antallBarn: Int? = 0
}

@Suppress("unused")
@XmlRootElement(name = "perForskSiv")
@XmlAccessorType(XmlAccessType.FIELD)
class ForskuddSivilstandPeriode {
    @XmlElement(name = "fomDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var fomDato: LocalDate? = null

    @XmlElement(name = "tomDato", nillable = true)
    @XmlJavaTypeAdapter(PeriodDateAdapter::class)
    var tomDato: LocalDate? = null

    @XmlElement(name = "forsorgKd", nillable = true)
    var kode: String? = null

    @XmlElement(name = "forsorgBesk", nillable = true)
    var beskrivelse: String? = null
}

@Suppress("unused")
@XmlRootElement(name = "perForskVtak")
@XmlAccessorType(XmlAccessType.FIELD)
class ForskuddVedtakPeriode {
    @XmlElement(name = "fomDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var fomDato: LocalDate? = null

    @XmlElement(name = "tomDato", nillable = true)
    @XmlJavaTypeAdapter(PeriodDateAdapter::class)
    var tomDato: LocalDate? = null

    @XmlElement(name = "maksInnt", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var maksInntekt: BigDecimal? = null

    @XmlElement(name = "resKd", nillable = true)
    var resultatKode: String? = null

    @XmlElement(name = "forskKd", nillable = true)
    var forskKode: String? = null

    @XmlElement(name = "forskBelop", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapterToDesimaler::class)
    var beløp: BigDecimal? = null

    @XmlElement(name = "forskPst", nillable = true)
    var prosent: String? = null

    @XmlElement(name = "fnr", nillable = true)
    var fnr: String? = null
}

@Suppress("unused")
@XmlRootElement(name = "perForskVtak")
@XmlAccessorType(XmlAccessType.FIELD)
class InntektGrunnlagForskuddPeriode {
    @XmlElement(name = "fomDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var fomDato: LocalDate? = null

    @XmlElement(name = "tomDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var tomDato: LocalDate? = null

    @XmlElement(name = "forsorgKd", nillable = true)
    var forsorgerKode: String? = null

    @XmlElement(name = "antBarn", nillable = true)
    @XmlJavaTypeAdapter(NumberAdapter::class)
    var antallBarn: Int? = 0

    @XmlElement(name = "belop75fra", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belop75fra: BigDecimal? = null

    @XmlElement(name = "belop75til", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belop75til: BigDecimal? = null

    @XmlElement(name = "belop50fra", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belop50fra: BigDecimal? = null

    @XmlElement(name = "belop50til", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belop50til: BigDecimal? = null
}

@Suppress("unused")
@XmlRootElement(name = "perInntekt")
@XmlAccessorType(XmlAccessType.FIELD)
class InntektPeriode {
    @XmlElement(name = "fomDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var fomDato: LocalDate? = null

    @XmlElement(name = "tomDato", nillable = true)
    @XmlJavaTypeAdapter(PeriodDateAdapter::class)
    var tomDato: LocalDate? = null

    @XmlElement(name = "belopTyp", nillable = true)
    var belopType: String? = null

    @XmlElement(name = "belopAar", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belopÅrsinntekt: BigDecimal? = null

    @XmlElement(name = "fnr", nillable = true)
    var fnr: String? = null

    @XmlElement(name = "bidrRolle", nillable = true)
    var rolle: String? = null

    @XmlElement(name = "belopBeskr", nillable = true)
    var beskrivelse: String? = null

    @XmlElement(name = "inntGrense", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var inntektGrense: BigDecimal? = null
}

@Suppress("unused")
@XmlRootElement(name = "barn")
@XmlAccessorType(XmlAccessType.FIELD)
class Barn {
    @XmlElement(name = "navn", nillable = true)
    var navn: String? = null

    @XmlElement(name = "fnr", nillable = true)
    var fnr: String? = null

    @XmlElement(name = "fDato", nillable = true)
    @XmlJavaTypeAdapter(BirthDateAdapter::class)
    var fDato: LocalDate? = null

    @XmlElement(name = "saksnr", nillable = true)
    var saksnr: String? = null
}

@Suppress("unused")
@XmlRootElement(name = "perAndelU")
@XmlAccessorType(XmlAccessType.FIELD)
class AndelUnderholdPeriode {
    @XmlElement(name = "fomDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var fomDato: LocalDate? = null

    @XmlElement(name = "tomDato", nillable = true)
    @XmlJavaTypeAdapter(PeriodDateAdapter::class)
    var tomDato: LocalDate? = null

    @XmlElement(name = "belopInntBp", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belopInntektBp: BigDecimal? = null

    @XmlElement(name = "belopInntBm", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belopInntektBm: BigDecimal? = null

    @XmlElement(name = "belopInntBb", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belopInntektBarn: BigDecimal? = null

    @XmlElement(name = "belopInntSum", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belopInntektSum: BigDecimal? = null

    @XmlElement(name = "fordNokkel", nillable = true)
    @XmlJavaTypeAdapter(PercentageAdapter::class)
    var fordNokkel: BigDecimal? = null

    @XmlElement(name = "andelTeller", nillable = true)
    @XmlJavaTypeAdapter(NumberAdapter::class)
    var andelTeller: Int? = null

    @XmlElement(name = "andelNevner", nillable = true)
    @XmlJavaTypeAdapter(NumberAdapter::class)
    var andelNevner: Int? = null

    @XmlElement(name = "belopUkost", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belopUnderholdKostnad: BigDecimal? = null

    @XmlElement(name = "belopBp", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belopBp: BigDecimal? = null
}

@Suppress("unused")
@XmlRootElement(name = "perUkost")
@XmlAccessorType(XmlAccessType.FIELD)
class UnderholdkostnadPeriode {
    @XmlElement(name = "fomDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var fomDato: LocalDate? = null

    @XmlElement(name = "tomDato", nillable = true)
    @XmlJavaTypeAdapter(PeriodDateAdapter::class)
    var tomDato: LocalDate? = null

    @XmlElement(name = "tilsyntypKd", nillable = true)
    var tilsyntypKd: String? = null

    @XmlElement(name = "skolealderTp", nillable = true)
    var skolealderTp: String? = null

    @XmlElement(name = "stonadTypKd", nillable = true)
    var stonadTypKd: String? = null

    @XmlElement(name = "manuellJN", nillable = true)
    @XmlJavaTypeAdapter(BooleanAdapter::class)
    var manuellJN: Boolean? = null

    @XmlElement(name = "belopFbrKost", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belopFbrKost: BigDecimal? = null

    @XmlElement(name = "belopBoutg", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belopBoutg: BigDecimal? = null

    @XmlElement(name = "belGkjBTils", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belGkjBTils: BigDecimal? = null

    @XmlElement(name = "belFaktBTils", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belFaktBTils: BigDecimal? = null

    @XmlElement(name = "belopBTrygd", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belopBTrygd: BigDecimal? = null

    @XmlElement(name = "belopSmaBTil", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belopSmaBTil: BigDecimal? = null

    @XmlElement(name = "belBerSumU", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belBerSumU: BigDecimal? = null

    @XmlElement(name = "belJustSumU", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belJustSumU: BigDecimal? = null

    @XmlElement(name = "fnr", nillable = true)
    var fodselsnummer: String? = null

    @XmlElement(name = "bidrRolle", nillable = true)
    var rolle: String? = null

    @XmlElement(name = "belopForplei", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belopForplei: BigDecimal? = null
}

@Suppress("unused")
@XmlRootElement(name = "perBidrEvne")
@XmlAccessorType(XmlAccessType.FIELD)
class BidragEvnePeriode {
    @XmlElement(name = "fomDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var fomDato: LocalDate? = null

    @XmlElement(name = "tomDato", nillable = true)
    @XmlJavaTypeAdapter(PeriodDateAdapter::class)
    var tomDato: LocalDate? = null

    @XmlElement(name = "skattekl", nillable = true)
    var skatteklasse: String? = null

    @XmlElement(name = "bostatus", nillable = true)
    var bostatus: String? = null

    @XmlElement(name = "antBarn", nillable = true)
    @XmlJavaTypeAdapter(NumberAdapter::class)
    var antallBarn: Int? = null

    @XmlElement(name = "antBarnDelt", nillable = true)
    @XmlJavaTypeAdapter(NumberAdapter::class)
    var antallBarnDelt: Int? = null

    @XmlElement(name = "flBarnSakJN", nillable = true)
    @XmlJavaTypeAdapter(BooleanAdapter::class)
    var flBarnSakJN: Boolean? = null

    @XmlElement(name = "fullBiEvneJN", nillable = true)
    @XmlJavaTypeAdapter(BooleanAdapter::class)
    var fullBiEvneJN: Boolean? = null

    @XmlElement(name = "biEvneBeskr", nillable = true)
    var biEvneBeskr: String? = null

    @XmlElement(name = "belInntGrlag", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belInntGrlag: BigDecimal? = null

    @XmlElement(name = "belTrygdeAvg", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belTrygdeAvg: BigDecimal? = null

    @XmlElement(name = "belSkatt", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belSkatt: BigDecimal? = null

    @XmlElement(name = "belMinFradrg", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belMinFradrg: BigDecimal? = null

    @XmlElement(name = "belPerFradrg", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belPerFradrg: BigDecimal? = null

    @XmlElement(name = "belBoutgift", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belBoutgift: BigDecimal? = null

    @XmlElement(name = "belEgetUhold", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belEgetUhold: BigDecimal? = null

    @XmlElement(name = "belUholdBhus", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belUholdBhus: BigDecimal? = null

    @XmlElement(name = "belAarEvne", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belAarEvne: BigDecimal? = null

    @XmlElement(name = "belMndEvne", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belMndEvne: BigDecimal? = null

    @XmlElement(name = "belSumBidrag", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belSumBidrag: BigDecimal? = null

    @XmlElement(name = "belBerBidrag", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belBerBidrag: BigDecimal? = null

    @XmlElement(name = "belJustBidr", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belJustBidr: BigDecimal? = null
}

@XmlRootElement(name = "perSamvaer")
@XmlAccessorType(XmlAccessType.FIELD)
class SamvarPeriode {
    @XmlElement(name = "fomDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var fomDato: LocalDate? = null

    @XmlElement(name = "tomDato", nillable = true)
    @XmlJavaTypeAdapter(PeriodDateAdapter::class)
    var tomDato: LocalDate? = null

    @XmlElement(name = "samvKode", nillable = true)
    var samvarKode: String? = null

    @XmlElement(name = "aldersgrp", nillable = true)
    var aldersGruppe: String? = null

    @XmlElement(name = "belSamvFradr", nillable = true)
    @XmlJavaTypeAdapter(BelopAdapter::class)
    var belSamvFradr: BigDecimal? = null

    @XmlElement(name = "samvBeskr", nillable = true)
    var samvBeskr: String? = null

    @XmlElement(name = "fnr", nillable = true)
    var fodselsnummer: String? = null
}
