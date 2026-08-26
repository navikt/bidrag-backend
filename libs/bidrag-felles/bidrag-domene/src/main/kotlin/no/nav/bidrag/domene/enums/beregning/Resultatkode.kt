package no.nav.bidrag.domene.enums.beregning

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.util.fjernAvslagOpphørPrefiks
import no.nav.bidrag.domene.util.visningsnavn
import no.nav.bidrag.domene.util.visningsnavnIntern

val konverterteVerdier =
    mapOf(
        "DIREKTE_OPPJØR" to Resultatkode.DIREKTE_OPPGJØR,
        "PÅ_GRUNN_AV_SAMMENFLYTTING" to Resultatkode.BARNET_ANSES_Å_BO_SAMMEN_MED_BEGGE_FORELDRE,
    )

@Schema(enumAsRef = true, name = "Resultatkode")
enum class Resultatkode(
    val bisysKode: List<BisysResultatkode>,
    vararg val type: ResultatkodeType,
) {
    OPPHØR(
        listOf(BisysResultatkode("OH", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.BARNEBIDRAG,
        ResultatkodeType.OPPHØR,
    ),

    // Gebyr
    GEBYR_FRITATT(
        listOf(
            BisysResultatkode("GIFR", type = BisysResultatkodeType.INNVILGELSE),
            BisysResultatkode("GKFI", type = BisysResultatkodeType.KLAGE),
        ),
        ResultatkodeType.BARNEBIDRAG,
    ),
    GEBYR_ILAGT(
        listOf(
            BisysResultatkode("GIGI", type = BisysResultatkodeType.INNVILGELSE),
            BisysResultatkode("GKIF", type = BisysResultatkodeType.KLAGE),
        ),
        ResultatkodeType.BARNEBIDRAG,
    ),

    BARNET_ER_SELVFORSØRGET(
        listOf(BisysResultatkode("5SF", BisysResultatkodeType.AVSLAG)),
        ResultatkodeType.BARNEBIDRAG,
        ResultatkodeType.SÆRBIDRAG,
        ResultatkodeType.AVSLAG,
    ),

    @JsonAlias("DIREKTE_OPPJØR")
    DIREKTE_OPPGJØR(listOf(BisysResultatkode("V")), ResultatkodeType.BARNEBIDRAG),

    // Avslag barnebidrag 18 år
    IKKE_DOKUMENTERT_SKOLEGANG(
        listOf(BisysResultatkode("ADS", BisysResultatkodeType.AVSLAG)),
        ResultatkodeType.BARNEBIDRAG,
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
    ),
    AVSLUTTET_SKOLEGANG(
        listOf(BisysResultatkode("OSG", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.BARNEBIDRAG,
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
    ),

    // Avslag barnebidrag
    IKKE_STERK_NOK_GRUNN_OG_BIDRAGET_HAR_OPPHØRT(
        listOf(BisysResultatkode("OSB")),
        ResultatkodeType.BARNEBIDRAG,
        ResultatkodeType.OPPHØR,
        ResultatkodeType.AVSLAG,
        ResultatkodeType.AVVISNING,
        ResultatkodeType.DIREKTE_AVSLAG,
    ),
    IKKE_OMSORG_FOR_BARNET(
        listOf(BisysResultatkode("IOB")),
        ResultatkodeType.BARNEBIDRAG,
        ResultatkodeType.OPPHØR,
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
    ),
    PARTENE_BOR_SAMMEN(
        listOf(BisysResultatkode("APB", BisysResultatkodeType.AVSLAG), BisysResultatkode("OPS", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.BARNEBIDRAG,
        ResultatkodeType.OPPHØR,
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
    ),

    @JsonAlias("BARNET_ER_DØD")
    BARNET_ER_DØDT(
        listOf(BisysResultatkode("OBD")),
        ResultatkodeType.BARNEBIDRAG,
        ResultatkodeType.FORSKUDD,
        ResultatkodeType.OPPHØR,
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
    ),
    BIDRAGSMOTTAKER_HAR_OMSORG_FOR_BARNET(
        listOf(BisysResultatkode("AOB")),
        ResultatkodeType.AVVISNING,
    ),
    BIDRAGSPLIKTIG_ER_DØD(
        listOf(BisysResultatkode("BPD")),
        ResultatkodeType.BARNEBIDRAG,
        ResultatkodeType.OPPHØR,
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
    ),
    BEREGNET_BIDRAG(
        listOf(),
        ResultatkodeType.BARNEBIDRAG,
    ),

    REDUSERT_FORSKUDD_50_PROSENT(listOf(BisysResultatkode("50")), ResultatkodeType.FORSKUDD),
    ORDINÆRT_FORSKUDD_75_PROSENT(listOf(BisysResultatkode("75")), ResultatkodeType.FORSKUDD),
    FORHØYET_FORSKUDD_100_PROSENT(listOf(BisysResultatkode("100")), ResultatkodeType.FORSKUDD),
    FORHØYET_FORSKUDD_11_ÅR_125_PROSENT(listOf(BisysResultatkode("125")), ResultatkodeType.FORSKUDD),

    // Resultat av beregning av særbidrag
    @Deprecated("SÆRTILSKUDD er erstattet med SÆRBIDRAG", ReplaceWith("SÆRBIDRAG_INNVILGET"))
    SÆRTILSKUDD_INNVILGET(listOf(BisysResultatkode("VS")), ResultatkodeType.SÆRBIDRAG),
    SÆRBIDRAG_INNVILGET(listOf(BisysResultatkode("VS")), ResultatkodeType.SÆRBIDRAG),

    // Resultat av beregning av særbidrag
    @Deprecated("SÆRTILSKUDD er erstattet med SÆRBIDRAG", ReplaceWith("SÆRBIDRAG_IKKE_FULL_BIDRAGSEVNE"))
    SÆRTILSKUDD_IKKE_FULL_BIDRAGSEVNE(
        listOf(BisysResultatkode("ABS", BisysResultatkodeType.AVSLAG)),
        ResultatkodeType.SÆRBIDRAG,
        ResultatkodeType.AVSLAG,
    ),
    SÆRBIDRAG_IKKE_FULL_BIDRAGSEVNE(
        listOf(BisysResultatkode("ABS", BisysResultatkodeType.AVSLAG)),
        ResultatkodeType.SÆRBIDRAG,
        ResultatkodeType.AVSLAG,
    ),
    SÆRBIDRAG_MANGLER_BIDRAGSEVNE(
        listOf(BisysResultatkode("ABB", BisysResultatkodeType.AVSLAG)),
        ResultatkodeType.SÆRBIDRAG,
        ResultatkodeType.AVSLAG,
    ),

    AVSLAG(listOf(BisysResultatkode("A", BisysResultatkodeType.AVSLAG)), ResultatkodeType.AVSLAG),
    AVSLAG2(listOf(BisysResultatkode("AA", BisysResultatkodeType.AVSLAG)), ResultatkodeType.AVSLAG),

    PARTEN_BER_OM_OPPHØR(
        listOf(BisysResultatkode("OPB", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),

    AVSLAG_OVER_18_ÅR(
        listOf(BisysResultatkode("A18", BisysResultatkodeType.AVSLAG), BisysResultatkode("OH2", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),
    AVSLAG_IKKE_REGISTRERT_PÅ_ADRESSE(
        listOf(BisysResultatkode("ARA", BisysResultatkodeType.AVSLAG), BisysResultatkode("ORA", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
    ),

    AVSLAG_HØY_INNTEKT(
        listOf(BisysResultatkode("AHI", BisysResultatkodeType.AVSLAG), BisysResultatkode("OHI", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),

    PÅ_GRUNN_AV_BARNEPENSJON(
        listOf(BisysResultatkode("ABA", BisysResultatkodeType.AVSLAG), BisysResultatkode("OBA", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),
    IKKE_OMSORG(
        listOf(BisysResultatkode("AIO", BisysResultatkodeType.AVSLAG), BisysResultatkode("OIO", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),
    BARNETS_EKTESKAP(
        listOf(BisysResultatkode("ABE", BisysResultatkodeType.AVSLAG), BisysResultatkode("OBE", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),
    BARNETS_INNTEKT(
        listOf(BisysResultatkode("ABI", BisysResultatkodeType.AVSLAG), BisysResultatkode("OBI", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),
    PÅ_GRUNN_AV_YTELSE_FRA_FOLKETRYGDEN(
        listOf(BisysResultatkode("AFT", BisysResultatkodeType.AVSLAG), BisysResultatkode("OFT", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),
    FULLT_UNDERHOLDT_AV_OFFENTLIG(
        listOf(BisysResultatkode("AFU", BisysResultatkodeType.AVSLAG), BisysResultatkode("OFU", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),

    IKKE_OPPHOLD_I_RIKET(
        listOf(BisysResultatkode("AIR", BisysResultatkodeType.AVSLAG), BisysResultatkode("OIR", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),
    MANGLENDE_DOKUMENTASJON(
        listOf(BisysResultatkode("AMD", BisysResultatkodeType.AVSLAG), BisysResultatkode("OMD", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.SÆRBIDRAG,
        ResultatkodeType.FORSKUDD,
    ),

    @JsonAlias("PÅ_GRUNN_AV_SAMMENFLYTTING", "BARNET_BOR_SAMMEN_MED_BEGGE_FORELDRE")
    BARNET_ANSES_Å_BO_SAMMEN_MED_BEGGE_FORELDRE(
        listOf(BisysResultatkode("ASA", BisysResultatkodeType.AVSLAG), BisysResultatkode("OSA", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.OPPHØR,
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),
    OPPHOLD_I_UTLANDET(
        listOf(BisysResultatkode("AUT", BisysResultatkodeType.AVSLAG), BisysResultatkode("OUT", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),
    UTENLANDSK_YTELSE(
        listOf(BisysResultatkode("AUY", BisysResultatkodeType.AVSLAG), BisysResultatkode("OUY", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),

    // Også for 18 år
    AVSLAG_PRIVAT_AVTALE_BIDRAG(
        listOf(BisysResultatkode("APA", BisysResultatkodeType.AVSLAG), BisysResultatkode("OPA", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.OPPHØR,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
        ResultatkodeType.BARNEBIDRAG,
    ),

    // I tilfeller når BP bor i Lugano-land og BM søker kun om forskudd, så har vi ikke hjemmel til å ta bidragssaken opp av eget tiltak etter forskotteringsloven.
    // Da må BM søke om fastsettelse av bidrag før vi innvilger forskuddet. Hvis hen ikke søker, så avslår vi kravet på grunn av manglende samarbeid (§3 i forskotteringsloven).
    @Deprecated("Bruk IKKE_INNKREVING_AV_BIDRAG istedenfor", ReplaceWith("IKKE_INNKREVING_AV_BIDRAG"))
    IKKE_SØKT_OM_INNKREVING_AV_BIDRAG(
        listOf(BisysResultatkode("AIB", BisysResultatkodeType.AVSLAG), BisysResultatkode("OIB", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),
    IKKE_INNKREVING_AV_BIDRAG(
        listOf(BisysResultatkode("AIB", BisysResultatkodeType.AVSLAG), BisysResultatkode("OIB", BisysResultatkodeType.OPPHØR)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.FORSKUDD,
    ),

    // Særbidrag avslag
    UTGIFTER_DEKKES_AV_BARNEBIDRAGET(
        listOf(BisysResultatkode("ADB", BisysResultatkodeType.AVSLAG)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.SÆRBIDRAG,
    ),
    IKKE_NØDVENDIGE_UTGIFTER(
        listOf(BisysResultatkode("AIN", BisysResultatkodeType.AVSLAG)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.SÆRBIDRAG,
    ),
    PRIVAT_AVTALE(
        listOf(BisysResultatkode("VX")),
        ResultatkodeType.BARNEBIDRAG,
    ),
    AVSLAG_PRIVAT_AVTALE_OM_SÆRBIDRAG(
        listOf(BisysResultatkode("APS", BisysResultatkodeType.AVSLAG)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.DIREKTE_AVSLAG,
        ResultatkodeType.SÆRBIDRAG,
    ),
    ALLE_UTGIFTER_ER_FORELDET(
        listOf(BisysResultatkode("AUF", BisysResultatkodeType.AVSLAG)),
        ResultatkodeType.AVSLAG,
        ResultatkodeType.SÆRBIDRAG,
    ),
    GODKJENT_BELØP_ER_LAVERE_ENN_FORSKUDDSSATS(
        listOf(BisysResultatkode("AMF")),
        ResultatkodeType.SÆRBIDRAG,
        ResultatkodeType.AVSLAG,
    ),
    INGEN_ENDRING_UNDER_GRENSE(
        listOf(BisysResultatkode("VO")),
        ResultatkodeType.BARNEBIDRAG,
    ),

    INNVILGET_VEDTAK(
        listOf(BisysResultatkode("IV")),
        ResultatkodeType.BARNEBIDRAG,
    ),
    SKJØNN_UTLANDET(
        listOf(BisysResultatkode("11B")),
        ResultatkodeType.BARNEBIDRAG,
    ),

    // Koder som ikke brukes i ny løsning men bare i Bisys

    LAVERE_ENN_INNTEKTSEVNE_BEGGE_PARTER(
        listOf(BisysResultatkode("4E")),
        ResultatkodeType.BARNEBIDRAG,
    ),
    LAVERE_ENN_INNTEKTSEVNE_BIDRAGSPLIKTIG(
        listOf(BisysResultatkode("4E1")),
        ResultatkodeType.BARNEBIDRAG,
    ),
    LAVERE_ENN_INNTEKTSEVNE_BIDRAGSMOTTAKER(
        listOf(BisysResultatkode("4E2")),
        ResultatkodeType.BARNEBIDRAG,
    ),
    MANGLER_DOKUMENTASJON_AV_INNTEKT_BEGGE_PARTER(
        listOf(BisysResultatkode("4D")),
        ResultatkodeType.BARNEBIDRAG,
    ),
    MANGLER_DOKUMENTASJON_AV_INNTEKT_BIDRAGSPLIKTIG(
        listOf(BisysResultatkode("4D1")),
        ResultatkodeType.BARNEBIDRAG,
    ),
    MANGLER_DOKUMENTASJON_AV_INNTEKT_BIDRAGSMOTTAKER(
        listOf(BisysResultatkode("4D2")),
        ResultatkodeType.BARNEBIDRAG,
    ),
    INNTIL_1_ÅR_TILBAKE(
        listOf(BisysResultatkode("VT")),
        ResultatkodeType.BARNEBIDRAG,
    ),
    MAKS_25_PROSENT_AV_INNTEKT(
        listOf(BisysResultatkode("7M")),
        ResultatkodeType.BARNEBIDRAG,
    ),
    MANGLER_BIDRAGSEVNE(
        listOf(BisysResultatkode("6MB")),
        ResultatkodeType.BARNEBIDRAG,
    ),
    INGEN_BIDRAGSEVNE(
        listOf(BisysResultatkode("0MB")),
        ResultatkodeType.BARNEBIDRAG,
    ),
    KOSTNADSBEREGNET_BIDRAG(
        listOf(BisysResultatkode("KBB")),
        ResultatkodeType.BARNEBIDRAG,
    ),
    INNKREVINGSGRUNNLAG(
        listOf(BisysResultatkode("IKG")),
        ResultatkodeType.BARNEBIDRAG,
    ),

    INDEKSREGULERING(
        listOf(BisysResultatkode("IREG")),
        ResultatkodeType.BARNEBIDRAG,
        ResultatkodeType.FORSKUDD,
    ),

    // Bisys resultatkoder (ikke brukt i ny løsning)
    BIDRAG_JUSTERT_FOR_DELT_BOSTED(listOf(BisysResultatkode("8DN"))),
    BIDRAG_JUSTERT_FOR_NETTO_BARNETILLEGG_BP(listOf(BisysResultatkode("101"))),
    BIDRAG_JUSTERT_FOR_NETTO_BARNETILLEGG_BM(listOf(BisysResultatkode("102"))),
    BIDRAG_JUSTERT_TIL_FORSKUDDSSATS(listOf(BisysResultatkode("RFO"))),
    BIDRAG_JUSTERT_MANUELT_TIL_FORSKUDDSSATS(listOf(BisysResultatkode("RFO"))),
    ;

    val legacyKode get() = bisysKode.firstOrNull()?.resultatKode

    companion object {
        fun fraKode(kode: String): Resultatkode? = try {
            enumValues<Resultatkode>().find { res -> res.bisysKode.any { it.resultatKode == kode } } ?: Resultatkode.valueOf(kode)
        } catch (e: Exception) {
            konverterteVerdier[kode]
        }

        fun Resultatkode.tilBisysResultatkode() = tilBisysResultatkode(null)

        fun Resultatkode.tilBisysResultatkode(vedtakstype: Vedtakstype? = null) = tilBisysResultatkode(vedtakstype, false)

        fun Resultatkode.tilBisysResultatkode(
            vedtakstype: Vedtakstype? = null,
            løperBidragEllerForskudd: Boolean? = null,
        ) = if (vedtakstype == Vedtakstype.OPPHØR || (løperBidragEllerForskudd != null && løperBidragEllerForskudd)) {
            bisysKode.find { it.type == BisysResultatkodeType.OPPHØR }?.resultatKode ?: bisysKode.firstOrNull()?.resultatKode
        } else {
            bisysKode.firstOrNull()?.resultatKode
        }

        fun fraVisningsnavn(
            visningsnavn: String,
            vedtakstype: Vedtakstype? = null,
        ): Resultatkode? = try {
            enumValues<Resultatkode>().find {
                it.visningsnavnIntern(vedtakstype).equals(visningsnavn, ignoreCase = true) ||
                    it.visningsnavn.intern.equals(visningsnavn, ignoreCase = true) ||
                    it.visningsnavn.intern.equals(visningsnavn.fjernAvslagOpphørPrefiks(), ignoreCase = true)
            }
        } catch (e: Exception) {
            null
        }

        fun Resultatkode.erType(type: ResultatkodeType): Boolean = Resultatkode.alleMedType(type).contains(this)

        fun Resultatkode.erDirekteAvslag(): Boolean = erType(ResultatkodeType.DIREKTE_AVSLAG)

        fun Resultatkode.erAvvisning(): Boolean = erType(ResultatkodeType.AVVISNING)

        fun Resultatkode.erOpphør(): Boolean = erType(ResultatkodeType.OPPHØR)

        fun Resultatkode.erAvslag(): Boolean = erType(ResultatkodeType.AVSLAG) ||
            erType(ResultatkodeType.DIREKTE_AVSLAG)

        fun alleMedType(type: ResultatkodeType): List<Resultatkode> = try {
            enumValues<Resultatkode>().filter { it.type.contains(type) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    data class BisysResultatkode(
        val resultatKode: String,
        val type: BisysResultatkodeType = BisysResultatkodeType.INNVILGELSE,
    )

    enum class BisysResultatkodeType {
        AVSLAG,
        OPPHØR,
        KLAGE,
        INNVILGELSE,
    }

    enum class ResultatkodeType {
        OPPHØR,
        AVSLAG,
        AVVISNING,
        DIREKTE_AVSLAG,
        FORSKUDD,
        BARNEBIDRAG,
        SÆRBIDRAG,
    }
}
