package no.nav.bidrag.transport.reskontro.response.transaksjoner

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.Datoperiode
import java.math.BigDecimal
import java.time.LocalDate

@Schema(
    name = "Transaksjon",
    description = "Transaksjon på bidragssak.",
)
data class TransaksjonDto(
    @field:Schema(
        description = "Id på transaksjonen.",
    )
    val transaksjonsid: Long?,
    @field:Schema(
        description =
            "Transaksjonskoden for transaksjonen.\nGyldige transaksjonskoder er:\n" +
                "| Kode  | Korreksjonskode | Beskrivelse                                |\n" +
                "|-------|-----------------|--------------------------------------------|\n" +
                "| A1    | A3              | Bidragsforskudd                            |\n" +
                "| A10   |                 | Midlertidig forskuddsats                   |\n" +
                "| A2    |                 | Forskudd korrigering                       |\n" +
                "| A4    |                 | Forskudd utbetaling                        |\n" +
                "| A5    |                 | Forskudd feilutbetaling                    |\n" +
                "| A6    |                 | Forskudd erstatningsutbetaling             |\n" +
                "| A7    |                 | Forskudd returføring utbetalin             |\n" +
                "| B1    | B3              | Underholdsbidrag (m/u tilleggsbidrag)      |\n" +
                "| B10   |                 | Privat bidrag utbetaling                   |\n" +
                "| B2    |                 | Privat bidrag korrigering                  |\n" +
                "| B4    |                 | Privat oppgjør bidrag privat               |\n" +
                "| C1    |                 | Offentlig bidrag                           |\n" +
                "| C2    |                 | Offentlig bidrag korrigering               |\n" +
                "| C4    |                 | Off bidrag BP tilbakebetalt forskudd       |\n" +
                "| C5    |                 | Off bidrag tilbakeført innkrevingssak      |\n" +
                "| D1    | D3              | 18årsbidrag                                |\n" +
                "| D10   |                 | Bidrag privat 18 år utbetaling             |\n" +
                "| D2    |                 | Bidrag privat 18 år korrigering            |\n" +
                "| D4    |                 | Privat oppgjør bidrag 18 år                |\n" +
                "| E1    | E3              | Bidrag til særlige utgifter (særtilskudd)  |\n" +
                "| E10   |                 | Særtilskudd utbetaling                     |\n" +
                "| E2    |                 | Særtilskudd korrigering                    |\n" +
                "| E4    |                 | Privat oppgjør særtilskudd                 |\n" +
                "| F1    | F3              | Ektefellebidrag                            |\n" +
                "| F10   |                 | Utbetaling ektefellebidrag                 |\n" +
                "| F2    |                 | Ektefellebidrag korrigering                |\n" +
                "| F4    |                 | Privat oppgjør ektefellebidrag             |\n" +
                "| G1    | G3              | Gebyr                                      |\n" +
                "| G2    |                 | Fastsettelsesgebyr korrigering             |\n" +
                "| G4    |                 | Fastsettelsesgebyr tilbakebetaling         |\n" +
                "| G5    |                 | Fastsettelsesgebyr tilb.ført innkr.sak     |\n" +
                "| H1    | H3              | Tilbakekrevd forskudd                      |\n" +
                "| H2    |                 | Tilbakekrevd forskudd korrigering          |\n" +
                "| H5    |                 | Tilbakekrevd forskudd tilb.ført innkr      |\n" +
                "| I1    |                 | Motregning                                 |\n" +
                "| I2    |                 | Motregning korrigering                     |\n" +
                "| J1    |                 | Kommunale forskuddskrav                    |\n" +
                "| J10   |                 | Gamle kommunale krav utbetaling            |\n" +
                "| J2    |                 | Kommunale stønadskrav                      |\n" +
                "| J3    |                 | Folketrygdens stønadskrav                  |\n" +
                "| K1    |                 | Ettergivelse                               |\n" +
                "| K10   |                 | Ettergivelse korrigering                   |\n" +
                "| K2    |                 | Direkte oppgjør (innbetalt beløp)          |\n" +
                "| K3    |                 | Tilbakekreving ettergivelse                |\n" +
                "| 301   |                 | OCR innbetaling                            |\n" +
                "| 302   |                 | Trygdetrekk                                |\n" +
                "| 303   |                 | Trekk i utbetaling                         |\n" +
                "| 304   |                 | Aetat innbetaling                          |\n" +
                "| 305   |                 | Innbetaling Adra                           |\n" +
                "| 307   |                 | Tilbakeført fra reskontro                  |\n" +
                "| 309   |                 | Manuelt ført innbetaling                   |\n" +
                "| 371   |                 | Tilbakebetaling                            |\n" +
                "| 390   |                 | Annullert innbet                           |\n" +
                "| 400   |                 | Avskrivning                                |\n" +
                "| 401   |                 | Innbetaling/avskriving                     |\n",
    )
    val transaksjonskode: String?,
    @field:Schema(
        description = "Beskrivelse av transaksjonen.",
    )
    val beskrivelse: String?,
    @field:Schema(
        description = "Dato uvist over hva",
    )
    val dato: LocalDate?,
    @field:Schema(
        description = "Ident til skyldner.",
    )
    val skyldner: Personident?,
    @field:Schema(
        description = "Ident til mottaker.",
    )
    val mottaker: Personident?,
    @field:Schema(
        description = "Opprinnelig beløp på transaksjonen.",
    )
    val beløp: BigDecimal?,
    @field:Schema(
        description = "Resterende beløp.",
    )
    val restBeløp: BigDecimal?,
    @field:Schema(
        description = "Beløp i opprinnelig valuta.",
    )
    val beløpIOpprinneligValuta: BigDecimal?,
    @field:Schema(
        description = "Valutakode slik utlevert fra NAV.",
    )
    val valutakode: String?,
    @field:Schema(
        description = "Saksnummer for bidragssaken.",
    )
    val saksnummer: Saksnummer?,
    @field:Schema(
        description = "Periode for transaksjonen.",
    )
    val periode: Datoperiode?,
    @field:Schema(
        description = "Ident til barn.",
    )
    val barn: Personident?,
    @field:Schema(
        description = "Delytelsesid for transaksjonen.",
    )
    val delytelsesid: String?,
    @field:Schema(
        description =
            "Søknadstype. " +
                "Tom for A6, A7, B10, D10, E10, F10, G2, H2, I2, J10, K1, 301, 302, " +
                "303, 304, 305, 306, 307, 308, 309, 371, 372, 373, 374, 375, 376, 377, 378, 378, 379, 400.",
    )
    val søknadstype: String?,
)
