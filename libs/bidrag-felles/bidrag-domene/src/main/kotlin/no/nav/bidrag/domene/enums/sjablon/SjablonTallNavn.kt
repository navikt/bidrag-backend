package no.nav.bidrag.domene.enums.sjablon

enum class SjablonTallNavn(
    val navn: String,
    val id: String,
    val bidragsevne: Boolean = false,
    val nettoBarnetilsyn: Boolean = false,
    val underholdskostnad: Boolean = false,
    val bpAndelUnderholdskostnad: Boolean = false,
    val barnetilleggSkattesats: Boolean = false,
    val barnebidrag: Boolean = false,
    val forskudd: Boolean = false,
    val bpAndelSærbidrag: Boolean = false,
    val særbidrag: Boolean = false,
    val endringSjekkGrense: Boolean = false,
    val indeksregulering: Boolean = false,
    val aldersjustering: Boolean = false,
) {
    ORDINÆR_BARNETRYGD_BELØP(
        navn = "OrdinærBarnetrygdBeløp",
        id = "0001",
        underholdskostnad = true,
        aldersjustering = true,
    ),
    ORDINÆR_SMÅBARNSTILLEGG_BELØP(
        navn = "OrdinærSmåbarnstilleggBeløp",
        id = "0002",
    ),
    BOUTGIFTER_BIDRAGSBARN_BELØP(
        navn = "BoutgifterBidragsbarnBeløp",
        id = "0003",
        underholdskostnad = true,
        aldersjustering = true,
    ),
    FORDEL_SKATTEKLASSE2_BELØP(
        navn = "FordelSkatteklasse2Beløp",
        id = "0004",
        bpAndelSærbidrag = true,
    ),
    FORSKUDDSSATS_BELØP(
        navn = "ForskuddssatsBeløp",
        id = "0005",
        bpAndelUnderholdskostnad = true,
        forskudd = true,
        bpAndelSærbidrag = true,
    ),
    INNSLAG_KAPITALINNTEKT_BELØP(
        navn = "InnslagKapitalInntektBeløp",
        id = "0006",
        bidragsevne = true,
        bpAndelUnderholdskostnad = true,
        bpAndelSærbidrag = true,
    ),
    INNTEKTSINTERVALL_TILLEGGSBIDRAG_BELØP(
        navn = "InntektsintervallTilleggsbidragBeløp",
        id = "0007",
    ),
    MAKS_INNTEKT_BP_PROSENT(
        navn = "MaksInntektBPProsent",
        id = "0008",
    ),
    HØY_INNTEKT_BP_MULTIPLIKATOR(
        navn = "HøyInntektBPMultiplikator",
        id = "0009",
    ),
    INNTEKT_BB_MULTIPLIKATOR(
        navn = "InntektBBMultiplikator",
        id = "0010",
    ),
    MAKS_BIDRAG_MULTIPLIKATOR(
        navn = "MaksBidragMultiplikator",
        id = "0011",
    ),
    MAKS_INNTEKT_BB_MULTIPLIKATOR(
        navn = "MaksInntektBBMultiplikator",
        id = "0012",
    ),
    MAKS_INNTEKT_FORSKUDD_MOTTAKER_MULTIPLIKATOR(
        navn = "MaksInntektForskuddMottakerMultiplikator",
        id = "0013",
        forskudd = true,
    ),
    NEDRE_INNTEKTSGRENSE_GEBYR_BELØP(
        navn = "NedreInntektsgrenseGebyrBeløp",
        id = "0014",
    ),
    SKATT_ALMINNELIG_INNTEKT_PROSENT(
        navn = "SkattAlminneligInntektProsent",
        id = "0015",
        nettoBarnetilsyn = true,
    ),
    TILLEGGSBIDRAG_PROSENT(
        navn = "TilleggsbidragProsent",
        id = "0016",
    ),
    TRYGDEAVGIFT_PROSENT(
        navn = "TrygdeavgiftProsent",
        id = "0017",
        bidragsevne = true,
        barnetilleggSkattesats = true,
    ),
    BARNETILLEGG_SKATT_PROSENT(
        navn = "BarneTilleggSkattProsent",
        id = "0018",
    ),
    UNDERHOLD_EGNE_BARN_I_HUSSTAND_BELØP(
        navn = "UnderholdEgneBarnIHusstandBeløp",
        id = "0019",
        bidragsevne = true,
    ),
    ENDRING_BIDRAG_GRENSE_PROSENT(
        navn = "EndringBidragGrenseProsent",
        id = "0020",
        endringSjekkGrense = true,
    ),
    BARNETILLEGG_FORSVARET_FØRSTE_BARN_BELØP(
        navn = "BarnetilleggForsvaretFørsteBarnBeløp",
        id = "0021",
        barnebidrag = true,
    ),
    BARNETILLEGG_FORSVARET_ØVRIGE_BARN_BELØP(
        navn = "BarnetilleggForsvaretØvrigeBarnBeløp",
        id = "0022",
        barnebidrag = true,
    ),
    MINSTEFRADRAG_INNTEKT_BELØP(
        navn = "MinstefradragInntektBeløp",
        id = "0023",
        bidragsevne = true,
        barnetilleggSkattesats = true,
    ),
    GJENNOMSNITT_VIRKEDAGER_PR_MÅNED_ANTALL(
        navn = "GjennomsnittVirkedagerPrMånedAntall",
        id = "0024",
    ),
    MINSTEFRADRAG_INNTEKT_PROSENT(
        navn = "MinstefradragInntektProsent",
        id = "0025",
        bidragsevne = true,
        barnetilleggSkattesats = true,
    ),
    DAGLIG_SATS_BARNETILLEGG_BELØP(
        navn = "DagligSatsBarnetilleggBeløp",
        id = "0026",
    ),
    PERSONFRADRAG_KLASSE1_BELØP(
        navn = "PersonfradragKlasse1Beløp",
        id = "0027",
        bidragsevne = true,
        barnetilleggSkattesats = true,
    ),
    PERSONFRADRAG_KLASSE2_BELØP(
        navn = "PersonfradragKlasse2Beløp",
        id = "0028",
    ),
    KONTANTSTOTTE_BELØP(
        navn = "KontantstøtteBeløp",
        id = "0029",
    ),
    ØVRE_INNTEKTSGRENSE_IKKE_I_SKATTEPOSISJON_BELØP(
        navn = "ØvreInntektsgrenseIkkeISkatteposisjonBeløp",
        id = "0030",
    ),
    NEDRE_INNTEKTSGRENSE_FULL_SKATTEPOSISJON_BELØP(
        navn = "NedreInntektsgrenseFullSkatteposisjonBeløp",
        id = "0031",
    ),
    EKSTRA_SMÅBARNSTILLEGG_BELØP(
        navn = "EkstraSmåbarnstilleggBeløp",
        id = "0032",
    ),
    ØVRE_INNTEKTSGRENSE_FULLT_FORSKUDD_BELØP(
        navn = "ØvreInntektsgrenseFulltForskuddBeløp",
        id = "0033",
        forskudd = true,
    ),
    ØVRE_INNTEKTSGRENSE_75PROSENT_FORSKUDD_EN_BELØP(
        navn = "ØvreInntektsgrense75ProsentForskuddEnBeløp",
        id = "0034",
        forskudd = true,
    ),
    ØVRE_INNTEKTSGRENSE_75PROSENT_FORSKUDD_GS_BELØP(
        navn = "ØvreInntektsgrense75ProsentForskuddGSBeløp",
        id = "0035",
        forskudd = true,
    ),
    INNTEKTSINTERVALL_FORSKUDD_BELØP(
        navn = "InntektsintervallForskuddBeløp",
        id = "0036",
        forskudd = true,
    ),
    ØVRE_GRENSE_SÆRBIDRAG_BELØP(
        navn = "ØvreGrenseSærbidragBeløp",
        id = "0037",
    ),
    FORSKUDDSSATS_75PROSENT_BELØP(
        navn = "Forskuddssats75ProsentBeløp",
        id = "0038",
        forskudd = true,
    ),
    FORDEL_SÆRFRADRAG_BELØP(
        navn = "FordelSærfradragBeløp",
        id = "0039",
        bpAndelSærbidrag = true,
    ),
    SKATTESATS_ALMINNELIG_INNTEKT_PROSENT(
        navn = "SkattesatsAlminneligInntektProsent",
        id = "0040",
        bidragsevne = true,
        barnetilleggSkattesats = true,
    ),
    FORHØYET_BARNETRYGD_BELØP(
        navn = "ForhøyetBarnetrygdBeløp",
        id = "0041",
        underholdskostnad = true,
        aldersjustering = true,
    ),
    INDEKSREGULRERING_FAKTOR(
        navn = "IndeksreguleringFaktor",
        id = "0050",
        indeksregulering = true,
    ),
    FASTSETTELSESGEBYR_BELØP(
        navn = "FastsettelsesgebyrBeløp",
        id = "0100",
    ),
    DUMMY(
        navn = "Dummy",
        id = "9999",
    ),
    ;

    companion object {
        fun from(search: String): SjablonTallNavn = requireNotNull(entries.find { it.navn == search }) { DUMMY }

        fun fromId(search: String): SjablonTallNavn = requireNotNull(entries.find { it.id == search }) { DUMMY }
    }
}
