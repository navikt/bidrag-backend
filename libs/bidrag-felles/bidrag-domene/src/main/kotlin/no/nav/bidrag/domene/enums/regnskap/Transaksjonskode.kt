package no.nav.bidrag.domene.enums.regnskap

import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype

enum class Transaksjonskode(
    val korreksjonskode: String?,
    val negativtBeløp: Boolean,
    val beskrivelse: String,
) {
    A1("A3", false, "Bidragsforskudd"),
    A2(null, false, "Forskudd korrigering"),
    A3(null, true, "Korreksjonskode for A1"),
    A4(null, false, "Forskudd utbetaling"),
    A5(null, false, "Forskudd feilutbetaling"),
    A6(null, false, "Forskudd erstatningsutbetaling"),
    A7(null, false, "Forskudd returføring utbetaling"),
    A10(null, false, "Midlertidig forskuddsats"),
    B1("B3", false, "Underholdsbidrag (m/u tilleggsbidrag)"),
    B2(null, false, "Privat bidrag korrigering"),
    B3(null, true, "Korreksjonskode for B1"),
    B4(null, false, "Privat oppgjør bidrag privat"),
    B10(null, false, "Privat bidrag utbetaling"),
    C1(null, false, "Offentlig bidrag"),
    C2(null, false, "Offentlig bidrag korrigering"),
    C4(null, false, "Off bidrag BP tilbakebetalt forskudd"),
    C5(null, false, "Off bidrag tilbakeført innkrevingssak"),
    D1("D3", false, "18årsbidrag"),
    D2(null, false, "Bidrag privat 18 år korrigering"),
    D3(null, true, "Korreksjonskode for D1"),
    D4(null, false, "Privat oppgjør bidrag 18 år"),
    D10(null, false, "Bidrag privat 18 år utbetaling"),
    E1("E3", false, "Bidrag til særlige utgifter (særtilskudd)"),
    E2(null, false, "Særtilskudd korrigering"),
    E3(null, true, "Korreksjonskode for E1"),
    E4(null, false, "Privat oppgjør særtilskudd"),
    E10(null, false, "Særtilskudd utbetaling"),
    F1("F3", false, "Ektefellebidrag"),
    F2(null, false, "Ektefellebidrag korrigering"),
    F3(null, true, "Korreksjonskode for F1"),
    F4(null, false, "Privat oppgjør ektefellebidrag"),
    F10(null, false, "Utbetaling ektefellebidrag"),
    G1("G3", false, "Gebyr"),
    G2(null, false, "Fastsettelsesgebyr korrigering"),
    G3(null, true, "Korreksjonskode for G1"),
    G4(null, false, "Fastsettelsesgebyr tilbakebetaling"),
    G5(null, false, "Fastsettelsesgebyr tilb.ført innkr.sak"),
    H1("H3", false, "Tilbakekrevd forskudd"),
    H2(null, false, "Tilbakekrevd forskudd korrigering"),
    H3(null, true, "Korreksjonskode for H1"),
    H5(null, false, "Tilbakekrevd forskudd tilb.ført innkr"),
    I1(null, false, "Motregning"),
    I2(null, false, "Motregning korrigering"),
    J1(null, false, "Kommunale forskuddskrav"),
    J2(null, false, "Kommunale stønadskrav"),
    J3(null, false, "Folketrygdens stønadskrav"),
    J10(null, false, "Gamle kommunale krav utbetaling"),
    K1(null, false, "Ettergivelse"),
    K2(null, false, "Direkte oppgjør (innbetalt beløp)"),
    K3(null, false, "Tilbakekreving ettergivelse"),
    K10(null, false, "Ettergivelse korrigering"),
    M1("M3", false, "Tilbakekreving bidrag"),
    M3(null, true, "Korreksjonskode for M1"),
    T301(null, false, "OCR innbetaling"),
    T302(null, false, "Trygdetrekk"),
    T303(null, false, "Trekk i utbetaling"),
    T304(null, false, "Aetat innbetaling"),
    T305(null, false, "Innbetaling Adra"),
    T307(null, false, "Tilbakeført fra reskontro"),
    T309(null, false, "Manuelt ført innbetaling"),
    T371(null, false, "Tilbakebetaling"),
    T390(null, false, "Annullert innbet"),
    T400(null, false, "Avskrivning"),
    T401(null, false, "Innbetaling/avskriving"),
    ;

    companion object {
        fun hentAlle(): List<Transaksjonskode> = entries.toList()

        fun hentTransaksjonskodeForType(type: String): Transaksjonskode =
            when (type) {
                Stønadstype.FORSKUDD.name -> A1
                Stønadstype.BIDRAG.name -> B1
                Stønadstype.OPPFOSTRINGSBIDRAG.name -> B1
                Stønadstype.BIDRAG18AAR.name -> D1
                Stønadstype.EKTEFELLEBIDRAG.name -> F1
                Stønadstype.MOTREGNING.name -> I1
                Engangsbeløptype.SAERTILSKUDD.name -> E1
                Engangsbeløptype.SÆRTILSKUDD.name -> E1
                Engangsbeløptype.SÆRBIDRAG.name -> E1
                Engangsbeløptype.GEBYR_MOTTAKER.name -> G1
                Engangsbeløptype.GEBYR_SKYLDNER.name -> G1
                Engangsbeløptype.TILBAKEKREVING.name -> H1
                Engangsbeløptype.ETTERGIVELSE.name -> K1
                Engangsbeløptype.DIREKTE_OPPGJOR.name -> K2
                Engangsbeløptype.DIREKTE_OPPGJØR.name -> K2
                Engangsbeløptype.ETTERGIVELSE_TILBAKEKREVING.name -> K3
                Engangsbeløptype.TILBAKEKREVING_BIDRAG.name -> M1
                else -> throw IllegalStateException("Ugyldig type for transaksjonskode funnet!")
            }
    }
}
