package no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import java.math.BigDecimal
import java.time.LocalDate

@Suppress("unused")
@XmlRootElement(name = "sertilsk")
@XmlAccessorType(XmlAccessType.FIELD)
class Særbidrag {
    @XmlElement(name = "antTermin", nillable = true)
    @XmlJavaTypeAdapter(NumberAdapter::class)
    var antTermin: Int? = null

    @XmlElement(name = "belopSokt", nillable = true)
    @XmlJavaTypeAdapter(BelopNoDecimalAdapter::class)
    var beløpSøkt: BigDecimal? = null

    @XmlElement(name = "belopGodkj", nillable = true)
    @XmlJavaTypeAdapter(BelopNoDecimalAdapter::class)
    var beløpGodkjent: BigDecimal? = null

    @XmlElement(name = "fratrekk", nillable = true)
    @XmlJavaTypeAdapter(BelopNoDecimalAdapter::class)
    var fratrekk: BigDecimal? = null

    @XmlElement(name = "belSertilsk", nillable = true)
    @XmlJavaTypeAdapter(BelopNoDecimalAdapter::class)
    var beløpSærbidrag: BigDecimal? = null

    @XmlElement(name = "bidrEvneSiVt", nillable = true)
    @XmlJavaTypeAdapter(BooleanAdapter::class)
    var bidrEvneSiVt: Boolean? = false

    @XmlElement(name = "belopForsk", nillable = true)
    @XmlJavaTypeAdapter(BelopNoDecimalAdapter::class)
    var beløpForskudd: BigDecimal? = null

    @XmlElement(name = "belopInntgr", nillable = true)
    @XmlJavaTypeAdapter(BelopNoDecimalAdapter::class)
    var beløpInntektsgrense: BigDecimal? = null

    @XmlElement(name = "bpInntekt", nillable = true)
    @XmlJavaTypeAdapter(BelopNoDecimalAdapter::class)
    var bpInntekt: BigDecimal? = null

    @XmlElement(name = "bmInntekt", nillable = true)
    @XmlJavaTypeAdapter(BelopNoDecimalAdapter::class)
    var bmInntekt: BigDecimal? = null

    @XmlElement(name = "bbInntekt", nillable = true)
    @XmlJavaTypeAdapter(BelopNoDecimalAdapter::class)
    var bbInntekt: BigDecimal? = null

    @XmlElement(name = "sumInntekt", nillable = true)
    @XmlJavaTypeAdapter(BelopNoDecimalAdapter::class)
    var sumInntekt: BigDecimal? = null

    @XmlElement(name = "fordNokkel", nillable = true)
    @XmlJavaTypeAdapter(PercentageAdapter::class)
    var fordNokkel: BigDecimal? = null

    @XmlElement(name = "andelTeller", nillable = true)
    @XmlJavaTypeAdapter(NumberAdapter::class)
    var andelTeller: Int? = null

    @XmlElement(name = "andelNevner", nillable = true)
    @XmlJavaTypeAdapter(NumberAdapter::class)
    var andelNevner: Int? = null

    @XmlElement(name = "fnr", nillable = true)
    var fnr: String? = null

    @XmlElement(name = "belopTyp", nillable = true)
    var belopTyp: String? = null

    @XmlElement(name = "skatteKlII", nillable = true)
    var skatteKlII: String? = null
}

@Suppress("unused")
@XmlRootElement(name = "perSertilsk")
@XmlAccessorType(XmlAccessType.FIELD)
class SærbidragPeriode {
    @XmlElement(name = "fomDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var fomDato: LocalDate? = null

    @XmlElement(name = "tomDato", nillable = true)
    @XmlJavaTypeAdapter(DateAdapter::class)
    var tomDato: LocalDate? = null

    @XmlElement(name = "belopSerPer", nillable = true)
    @XmlJavaTypeAdapter(BelopNoDecimalAdapter::class)
    var beløp: BigDecimal? = null

    @XmlElement(name = "fnr", nillable = true)
    var fnr: String? = null
}
