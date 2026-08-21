/* eslint-disable */
/* tslint:disable */
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

type UtilRequiredKeys<T, K extends keyof T> = Omit<T, K> & Required<Pick<T, K>>;

export interface Arbeidsforhold {
  periode: TypeArManedsperiode;
  arbeidsgiver: string;
  stillingProsent?: string | null;
  /** @format date */
  lønnsendringDato?: string | null;
}

export enum Behandlingstema {
  AVSKRIVNING = "AVSKRIVNING",
  BIDRAG = "BIDRAG",
  BIDRAG_PLUSS_TILLEGGSBIDRAG = "BIDRAG_PLUSS_TILLEGGSBIDRAG",
  DIREKTEOPPGJOR = "DIREKTE_OPPGJØR",
  EKTEFELLEBIDRAG = "EKTEFELLEBIDRAG",
  ETTERGIVELSE = "ETTERGIVELSE",
  ERSTATNING = "ERSTATNING",
  FARSSKAP = "FARSSKAP",
  KUNNSKAP_OM_BIOLOGISK_FAR = "KUNNSKAP_OM_BIOLOGISK_FAR",
  FORSKUDD = "FORSKUDD",
  GEBYR = "GEBYR",
  INNKREVING = "INNKREVING",
  MORSSKAP = "MORSSKAP",
  MOTREGNING = "MOTREGNING",
  OPPFOSTRINGSBIDRAG = "OPPFOSTRINGSBIDRAG",
  REFUSJON_BIDRAG = "REFUSJON_BIDRAG",
  SAKSOMKOSTNINGER = "SAKSOMKOSTNINGER",
  SAeRBIDRAG = "SÆRBIDRAG",
  TILLEGGSBIDRAG = "TILLEGGSBIDRAG",
  TILBAKEKREVING_ETTERGIVELSE = "TILBAKEKREVING_ETTERGIVELSE",
  TILBAKEKREVING = "TILBAKEKREVING",
  TILBAKEKREVING_BIDRAG = "TILBAKEKREVING_BIDRAG",
  BIDRAG18ARPLUSSTILLEGGSBIDRAG = "BIDRAG_18_ÅR_PLUSS_TILLEGGSBIDRAG",
  BIDRAG18AR = "BIDRAG_18_ÅR",
  REISEKOSTNADER = "REISEKOSTNADER",
}

export enum Behandlingstype {
  ENDRING = "ENDRING",
  EGET_TILTAK = "EGET_TILTAK",
  SOKNAD = "SØKNAD",
  INNKREVINGSGRUNNLAG = "INNKREVINGSGRUNNLAG",
  FORHOLDSMESSIG_FORDELING = "FORHOLDSMESSIG_FORDELING",
  FORHOLDSMESSIG_FORDELING_KLAGE = "FORHOLDSMESSIG_FORDELING_KLAGE",
  ALDERSJUSTERING = "ALDERSJUSTERING",
  INDEKSREGULERING = "INDEKSREGULERING",
  KLAGE_BEGRENSET_SATS = "KLAGE_BEGRENSET_SATS",
  KLAGE = "KLAGE",
  FOLGERKLAGE = "FØLGER_KLAGE",
  KORRIGERING = "KORRIGERING",
  KONVERTERING = "KONVERTERING",
  OPPHOR = "OPPHØR",
  PRIVAT_AVTALE = "PRIVAT_AVTALE",
  BEGRENSET_REVURDERING = "BEGRENSET_REVURDERING",
  REVURDERING = "REVURDERING",
  OPPJUSTERT_FORSKUDD = "OPPJUSTERT_FORSKUDD",
  OMGJORING = "OMGJØRING",
  OMGJORINGBEGRENSETSATS = "OMGJØRING_BEGRENSET_SATS",
  PARAGRAF35C = "PARAGRAF_35_C",
  PARAGRAF35CBEGRENSETSATS = "PARAGRAF_35_C_BEGRENSET_SATS",
  MANEDLIGPALOP = "MÅNEDLIG_PÅLOP",
}

export interface Belop {
  verdi: number;
  valutakode?: Valutakode | null;
}

export enum BeregnTil {
  OPPRINNELIG_VEDTAKSTIDSPUNKT = "OPPRINNELIG_VEDTAKSTIDSPUNKT",
  INNEVAeRENDEMANED = "INNEVÆRENDE_MÅNED",
  ETTERFOLGENDEMANUELLVEDTAK = "ETTERFØLGENDE_MANUELL_VEDTAK",
}

export interface BeregnetBidragBarnDto {
  saksnummer: string;
  løpendeBeløp: number;
  valutakode: Valutakode;
  valutakurs: number;
  samværsklasse: Samvaersklasse;
  samværsfradrag: number;
  beregnetBeløp: number;
  indeksreguleringFaktor?: number | null;
  faktiskBeløp: number;
  reduksjonUnderholdskostnad: number;
  beregnetBidrag: number;
  /** @format int32 */
  vedtaksid?: number | null;
  bidragJustertForNettoBarnetilleggBP?: boolean | null;
  bruttoBidragEtterBarnetilleggBM?: number | null;
  bruttoBidragEtterBarnetilleggBP?: number | null;
  erVedtakKildeBBM: boolean;
}

export interface BeregnetBidragPerBarn {
  gjelderBarn: string;
  saksnummer: string;
  løpendeBeløp: number;
  valutakode: string;
  samværsklasse: Samvaersklasse;
  samværsfradrag: number;
  beregnetBeløp: number;
  faktiskBeløp: number;
  reduksjonUnderholdskostnad: number;
  beregnetBidrag: number;
  /** @format int32 */
  vedtaksid?: number | null;
  bidragJustertForNettoBarnetilleggBP?: boolean | null;
  bruttoBidragEtterBarnetilleggBM?: number | null;
  bruttoBidragEtterBarnetilleggBP?: number | null;
  erVedtakKildeBBM: boolean;
}

export interface BidragPeriodeBeregningsdetaljer {
  bpHarEvne: boolean;
  /** @format double */
  antallBarnIHusstanden?: number | null;
  forskuddssats: number;
  barnetilleggBM?: DokumentmalDelberegningBarnetilleggDto | null;
  barnetilleggBP?: DokumentmalDelberegningBarnetilleggDto | null;
  voksenIHusstanden?: boolean | null;
  enesteVoksenIHusstandenErEgetBarn?: boolean | null;
  bpsAndel?: DelberegningBidragspliktigesAndel | null;
  inntekter?: DokumentmalResultatBeregningInntekterDto | null;
  delberegningBidragsevne?: DokumentmalDelberegningBidragsevneDto | null;
  samværsfradrag?: NotatBeregningsdetaljerSamvaersfradrag | null;
  endringUnderGrense?: DelberegningEndringSjekkGrensePeriode | null;
  sluttberegning?: DokumentmalSluttberegningBarnebidragDetaljer | null;
  delberegningUnderholdskostnad?: DelberegningUnderholdskostnad | null;
  indeksreguleringDetaljer?: IndeksreguleringDetaljer | null;
  sluttberegningAldersjustering?: SluttberegningBarnebidragAldersjustering | null;
  delberegningBidragspliktigesBeregnedeTotalBidrag?: DokumentmalDelberegningBidragspliktigesBeregnedeTotalbidragDto | null;
  forholdsmessigFordelingBeregningsdetaljer?: DokumentmalForholdsmessigFordelingBeregningsdetaljer | null;
  deltBosted: boolean;
}

export interface BoforholdBarn {
  gjelder: DokumentmalPersonDto;
  medIBehandling: boolean;
  kilde: Kilde;
  opplysningerFraFolkeregisteret: OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeUnit[];
  opplysningerBruktTilBeregning: OpplysningerBruktTilBeregningBostatuskode[];
}

export enum Bostatuskode {
  MED_FORELDER = "MED_FORELDER",
  DOKUMENTERT_SKOLEGANG = "DOKUMENTERT_SKOLEGANG",
  IKKE_MED_FORELDER = "IKKE_MED_FORELDER",
  DELT_BOSTED = "DELT_BOSTED",
  REGNES_IKKE_SOM_BARN = "REGNES_IKKE_SOM_BARN",
  BOR_MED_ANDRE_VOKSNE = "BOR_MED_ANDRE_VOKSNE",
  BOR_IKKE_MED_ANDRE_VOKSNE = "BOR_IKKE_MED_ANDRE_VOKSNE",
  UNNTAK_HOS_ANDRE = "UNNTAK_HOS_ANDRE",
  UNNTAK_ALENE = "UNNTAK_ALENE",
  UNNTAKENSLIGASYLSOKER = "UNNTAK_ENSLIG_ASYLSØKER",
  MED_VERGE = "MED_VERGE",
  ALENE = "ALENE",
}

export interface DatoperiodeDto {
  /** @format date */
  fom: string;
  /** @format date */
  tom?: string | null;
}

export interface DelberegningBarnetilleggSkattesats {
  periode: TypeArManedsperiode;
  skattFaktor: number;
  minstefradrag: number;
  skattAlminneligInntekt: number;
  trygdeavgift: number;
  trinnskatt: number;
  sumSkatt: number;
  sumInntekt: number;
}

export interface DelberegningBidragspliktigesAndel {
  periode: TypeArManedsperiode;
  endeligAndelFaktor: number;
  andelBeløp: number;
  beregnetAndelFaktor: number;
  barnEndeligInntekt: number;
  barnetErSelvforsørget: boolean;
}

export interface DelberegningBoforhold {
  periode: TypeArManedsperiode;
  /** @format double */
  antallBarn: number;
  borMedAndreVoksne: boolean;
}

export interface DelberegningEndringSjekkGrensePeriode {
  periode: TypeArManedsperiode;
  løpendeBidragBeløp?: number | null;
  løpendeBidragFraPrivatAvtale: boolean;
  beregnetBidragBeløp?: number | null;
  faktiskEndringFaktor?: number | null;
  endringErOverGrense: boolean;
}

export interface DelberegningSumInntekt {
  periode: TypeArManedsperiode;
  totalinntekt: number;
  kontantstøtte?: number | null;
  skattepliktigInntekt?: number | null;
  barnetillegg?: number | null;
  utvidetBarnetrygd?: number | null;
  småbarnstillegg?: number | null;
}

export interface DelberegningUnderholdskostnad {
  periode: TypeArManedsperiode;
  forbruksutgift: number;
  boutgift: number;
  barnetilsynMedStønad?: number | null;
  nettoTilsynsutgift?: number | null;
  barnetrygd: number;
  underholdskostnad: number;
  forpleining?: number | null;
}

export interface DelberegningUtgift {
  periode: TypeArManedsperiode;
  sumBetaltAvBp: number;
  sumGodkjent: number;
}

export interface DokumentmalBarnetilleggDetaljerDto {
  bruttoBeløp: number;
  nettoBeløp: number;
  skattefaktor?: number | null;
  visningsnavn: string;
}

export interface DokumentmalBidragsevneUtgifterBolig {
  borMedAndreVoksne: boolean;
  boutgiftBeløp: number;
  underholdBeløp: number;
}

export interface DokumentmalDelberegningBarnetilleggDto {
  barnetillegg: DokumentmalBarnetilleggDetaljerDto[];
  skattFaktor: number;
  delberegningSkattesats?: DelberegningBarnetilleggSkattesats | null;
  sumBruttoBeløp: number;
  sumNettoBeløp: number;
}

export interface DokumentmalDelberegningBidragsevneDto {
  sumInntekt25Prosent: number;
  bidragsevne: number;
  skatt: DokumentmalSkattBeregning;
  underholdEgneBarnIHusstand: DokumentmalUnderholdEgneBarnIHusstand;
  utgifter: DokumentmalBidragsevneUtgifterBolig;
}

export interface DokumentmalDelberegningBidragspliktigesBeregnedeTotalbidragDto {
  beregnetBidragPerBarnListe: NotatBeregnetBidragPerBarnDto[];
  bidragspliktigesBeregnedeTotalbidrag: number;
  periode: TypeArManedsperiode;
}

export interface DokumentmalForholdsmessigFordelingBeregningsdetaljer {
  beregningFordelingAvBidragSjekkEvnesprekk?: DokumentmalForholdsmessigFordelingBidragTilFordeling | null;
  beregningFordelingAvBidrag: DokumentmalForholdsmessigFordelingBidragTilFordeling;
  sumBidragTilFordelingJustertForPrioriterteBidrag: number;
  evneJustertForPrioriterteBidrag: number;
  bidragTilFordelingForBarnet: number;
  andelAvSumBidragTilFordelingFaktor: number;
  andelAvEvneBeløp: number;
  bidragEtterFordeling: number;
  harBPFullEvne: boolean;
  erKompletteGrunnlagForAlleLøpendeBidrag: boolean;
  erForholdsmessigFordelt: boolean;
  bidragTilFordelingAlle: DokumentmalForholdsmessigFordelingBidragTilFordelingBarn[];
}

export interface DokumentmalForholdsmessigFordelingBidragTilFordeling {
  sumBidragTilFordeling: number;
  sumBidragTilFordelingSøknadsbarn: number;
  sumBidragTilFordelingIkkeSøknadsbarn: number;
  sumBidragTilFordelingPrivatAvtale: number;
  sumBidragSomIkkeKanFordeles: number;
  sumPrioriterteBidragTilFordeling: number;
  finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn: boolean;
  bidragTilFordelingAlle: DokumentmalForholdsmessigFordelingBidragTilFordelingBarn[];
}

export interface DokumentmalForholdsmessigFordelingBidragTilFordelingBarn {
  utenlandskbidrag: boolean;
  oppfostringsbidrag: boolean;
  privatAvtale: boolean;
  erSøknadsbarn: boolean;
  beregnetBidrag?: BeregnetBidragBarnDto | null;
  bidragTilFordeling: number;
  barn: DokumentmalPersonDto;
  erBidragSomIkkeKanFordeles: boolean;
}

export interface DokumentmalManuellVedtak {
  valgt: boolean;
  /** @format date-time */
  fattetTidspunkt: string;
  /** @format date */
  virkningsDato: string;
  vedtakstype: Vedtakstype;
  resultatSistePeriode: string;
  privatAvtale: boolean;
  begrensetRevurdering: boolean;
  søknadstype: string;
}

export interface DokumentmalPersonDto {
  rolle?: Rolletype | null;
  navn?: string | null;
  /** @format date */
  fødselsdato?: string | null;
  ident?: string | null;
  erBeskyttet: boolean;
  innbetaltBeløp?: number | null;
  /** @format date */
  opphørsdato?: string | null;
  /** @format date */
  virkningstidspunkt?: string | null;
  saksnummer?: string | null;
  bidragsmottakerIdent?: string | null;
  revurdering: boolean;
  harLøpendeForskudd?: boolean | null;
  harLøpendeBidrag?: boolean | null;
}

export interface DokumentmalResultatBeregningInntekterDto {
  inntektBM?: number | null;
  inntektBP?: number | null;
  inntektBarn?: number | null;
  barnEndeligInntekt?: number | null;
  inntektBarnMånedlig?: number | null;
  totalEndeligInntekt: number;
  inntektBPMånedlig?: number | null;
  inntektBMMånedlig?: number | null;
}

export type DokumentmalResultatBidragsberegningBarnDto = UtilRequiredKeys<VedtakResultatInnhold, "type"> & {
  barn: DokumentmalPersonDto;
  /** @format int32 */
  indeksår?: number | null;
  innkrevesFraDato?: string | null;
  erAvvistRevurdering: boolean;
  erAvvisning: boolean;
  minstEnPeriodeHarSlåttUtTilFF: boolean;
  perioderSlåttUtTilFF: PeriodeSlattUtTilFF[];
  orkestrertVedtak?: EndeligOrkestrertVedtak | null;
  perioder: ResultatBarnebidragsberegningPeriodeDto[];
};

export interface DokumentmalSkattBeregning {
  sumSkatt: number;
  skattAlminneligInntekt: number;
  trinnskatt: number;
  trygdeavgift: number;
  skattAlminneligInntektMånedsbeløp: number;
  trinnskattMånedsbeløp: number;
  trygdeavgiftMånedsbeløp: number;
  skattMånedsbeløp: number;
}

export interface DokumentmalSluttberegningBarnebidragDetaljer {
  beregnetBeløp?: number | null;
  resultatBeløp?: number | null;
  uMinusNettoBarnetilleggBM?: number;
  bruttoBidragEtterBarnetilleggBM: number;
  nettoBidragEtterBarnetilleggBM: number;
  bruttoBidragJustertForEvneOg25Prosent: number;
  bruttoBidragEtterBegrensetRevurdering: number;
  bruttoBidragEtterBarnetilleggBP: number;
  nettoBidragEtterSamværsfradrag: number;
  bpAndelAvUVedDeltBostedFaktor: number;
  bpAndelAvUVedDeltBostedBeløp: number;
  løpendeForskudd?: number | null;
  løpendeBidrag?: number | null;
  barnetErSelvforsørget: boolean;
  bidragJustertForDeltBosted: boolean;
  bidragJustertForNettoBarnetilleggBP: boolean;
  bidragJustertForNettoBarnetilleggBM: boolean;
  bidragJustertNedTilEvne: boolean;
  bidragJustertNedTil25ProsentAvInntekt: boolean;
  bidragJustertTilForskuddssats: boolean;
  bidragJustertManueltTilForskuddssats: boolean;
  begrensetRevurderingUtført: boolean;
  ikkeOmsorgForBarnet: boolean;
  bpEvneVedForholdsmessigFordeling?: number | null;
  bpAndelAvUVedForholdsmessigFordelingFaktor?: number | null;
  bpSumAndelAvU?: number | null;
  resultat?: Resultatkode | null;
  resultatVisningsnavn?: Visningsnavn | null;
  uminusNettoBarnetilleggBM: number;
}

export interface DokumentmalUnderholdEgneBarnIHusstand {
  getårsbeløp: number;
  sjablon: number;
  /** @format double */
  antallBarnIHusstanden: number;
  måndesbeløp: number;
}

export interface EndeligOrkestrertVedtak {
  type?: Vedtakstype | null;
  perioder: ResultatBarnebidragsberegningPeriodeDto[];
}

export interface IndeksreguleringDetaljer {
  sluttberegning?: SluttberegningIndeksregulering | null;
  faktor: number;
}

export enum InntektBelopstype {
  ValueARSBELOP = "ÅRSBELØP",
  MANEDSBELOP = "MÅNEDSBELØP",
  MANEDSBELOP11MANEDER = "MÅNEDSBELØP_11_MÅNEDER",
  DAGSATS = "DAGSATS",
}

export interface InntekterPerRolle {
  gjelder: DokumentmalPersonDto;
  arbeidsforhold: Arbeidsforhold[];
  årsinntekter: NotatInntektDto[];
  barnetillegg: NotatInntektDto[];
  utvidetBarnetrygd: NotatInntektDto[];
  småbarnstillegg: NotatInntektDto[];
  kontantstøtte: NotatInntektDto[];
  beregnetInntekter: NotatBeregnetInntektDto[];
  harInntekter: boolean;
}

export enum Inntektsrapportering {
  AINNTEKT = "AINNTEKT",
  AINNTEKTBEREGNET3MND = "AINNTEKT_BEREGNET_3MND",
  AINNTEKTBEREGNET12MND = "AINNTEKT_BEREGNET_12MND",
  AINNTEKTBEREGNETLAVESTEMANEDSINNTEKTSISTE3MNDGANGETTOLV = "AINNTEKT_BEREGNET_LAVESTE_MÅNEDSINNTEKT_SISTE_3MND_GANGET_TOLV",
  AINNTEKTBEREGNET3MNDFRAOPPRINNELIGVEDTAKSTIDSPUNKT = "AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAKSTIDSPUNKT",
  AINNTEKTBEREGNET12MNDFRAOPPRINNELIGVEDTAKSTIDSPUNKT = "AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAKSTIDSPUNKT",
  AINNTEKTBEREGNET3MNDFRAOPPRINNELIGVEDTAK = "AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAK",
  AINNTEKTBEREGNET12MNDFRAOPPRINNELIGVEDTAK = "AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAK",
  KAPITALINNTEKT = "KAPITALINNTEKT",
  LIGNINGSINNTEKT = "LIGNINGSINNTEKT",
  KONTANTSTOTTE = "KONTANTSTØTTE",
  SMABARNSTILLEGG = "SMÅBARNSTILLEGG",
  UTVIDET_BARNETRYGD = "UTVIDET_BARNETRYGD",
  AAP = "AAP",
  DAGPENGER = "DAGPENGER",
  FORELDREPENGER = "FORELDREPENGER",
  INTRODUKSJONSSTONAD = "INTRODUKSJONSSTØNAD",
  KVALIFISERINGSSTONAD = "KVALIFISERINGSSTØNAD",
  OVERGANGSSTONAD = "OVERGANGSSTØNAD",
  PENSJON = "PENSJON",
  SYKEPENGER = "SYKEPENGER",
  BARNETILLEGG = "BARNETILLEGG",
  BARNETILSYN = "BARNETILSYN",
  PERSONINNTEKT_EGNE_OPPLYSNINGER = "PERSONINNTEKT_EGNE_OPPLYSNINGER",
  KAPITALINNTEKT_EGNE_OPPLYSNINGER = "KAPITALINNTEKT_EGNE_OPPLYSNINGER",
  SAKSBEHANDLER_BEREGNET_INNTEKT = "SAKSBEHANDLER_BEREGNET_INNTEKT",
  LONNMANUELTBEREGNET = "LØNN_MANUELT_BEREGNET",
  NAeRINGSINNTEKTMANUELTBEREGNET = "NÆRINGSINNTEKT_MANUELT_BEREGNET",
  YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET = "YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET",
  AINNTEKT_KORRIGERT_FOR_BARNETILLEGG = "AINNTEKT_KORRIGERT_FOR_BARNETILLEGG",
  BARNETRYGD_MANUELL_VURDERING = "BARNETRYGD_MANUELL_VURDERING",
  BARNS_SYKDOM = "BARNS_SYKDOM",
  SKJONNMANGLERDOKUMENTASJON = "SKJØNN_MANGLER_DOKUMENTASJON",
  FORDELSAeRFRADRAGENSLIGFORSORGER = "FORDEL_SÆRFRADRAG_ENSLIG_FORSØRGER",
  FODSELADOPSJON = "FØDSEL_ADOPSJON",
  INNTEKTSOPPLYSNINGER_FRA_ARBEIDSGIVER = "INNTEKTSOPPLYSNINGER_FRA_ARBEIDSGIVER",
  LIGNINGSOPPLYSNINGER_MANGLER = "LIGNINGSOPPLYSNINGER_MANGLER",
  LIGNING_FRA_SKATTEETATEN = "LIGNING_FRA_SKATTEETATEN",
  LONNSOPPGAVEFRASKATTEETATEN = "LØNNSOPPGAVE_FRA_SKATTEETATEN",
  LONNSOPPGAVEFRASKATTEETATENKORRIGERTFORBARNETILLEGG = "LØNNSOPPGAVE_FRA_SKATTEETATEN_KORRIGERT_FOR_BARNETILLEGG",
  SKJONNMANGLENDEBRUKAVEVNE = "SKJØNN_MANGLENDE_BRUK_AV_EVNE",
  NETTO_KAPITALINNTEKT = "NETTO_KAPITALINNTEKT",
  PENSJON_KORRIGERT_FOR_BARNETILLEGG = "PENSJON_KORRIGERT_FOR_BARNETILLEGG",
  REHABILITERINGSPENGER = "REHABILITERINGSPENGER",
  SKATTEGRUNNLAG_KORRIGERT_FOR_BARNETILLEGG = "SKATTEGRUNNLAG_KORRIGERT_FOR_BARNETILLEGG",
}

export enum Inntektstype {
  AAP = "AAP",
  DAGPENGER = "DAGPENGER",
  FORELDREPENGER = "FORELDREPENGER",
  INTRODUKSJONSSTONAD = "INTRODUKSJONSSTØNAD",
  KVALIFISERINGSSTONAD = "KVALIFISERINGSSTØNAD",
  OVERGANGSSTONAD = "OVERGANGSSTØNAD",
  PENSJON = "PENSJON",
  SYKEPENGER = "SYKEPENGER",
  KONTANTSTOTTE = "KONTANTSTØTTE",
  SMABARNSTILLEGG = "SMÅBARNSTILLEGG",
  UTVIDET_BARNETRYGD = "UTVIDET_BARNETRYGD",
  KAPITALINNTEKT = "KAPITALINNTEKT",
  LONNSINNTEKT = "LØNNSINNTEKT",
  NAeRINGSINNTEKT = "NÆRINGSINNTEKT",
  BARNETILSYN = "BARNETILSYN",
  BARNETILLEGG_PENSJON = "BARNETILLEGG_PENSJON",
  BARNETILLEGGUFORETRYGD = "BARNETILLEGG_UFØRETRYGD",
  BARNETILLEGG_DAGPENGER = "BARNETILLEGG_DAGPENGER",
  BARNETILLEGGKVALIFISERINGSSTONAD = "BARNETILLEGG_KVALIFISERINGSSTØNAD",
  BARNETILLEGG_AAP = "BARNETILLEGG_AAP",
  BARNETILLEGG_DNB = "BARNETILLEGG_DNB",
  BARNETILLEGG_NORDEA = "BARNETILLEGG_NORDEA",
  BARNETILLEGG_STOREBRAND = "BARNETILLEGG_STOREBRAND",
  BARNETILLEGG_KLP = "BARNETILLEGG_KLP",
  BARNETILLEGG_SPK = "BARNETILLEGG_SPK",
  BARNETILLEGG_TILTAKSPENGER = "BARNETILLEGG_TILTAKSPENGER",
  BARNETILLEGG_SUMMERT = "BARNETILLEGG_SUMMERT",
}

export enum Kilde {
  MANUELL = "MANUELL",
  OFFENTLIG = "OFFENTLIG",
}

export interface KlageOmgjoringDetaljer {
  /** @format date-time */
  resultatFraVedtakVedtakstidspunkt?: string | null;
  beregnTilDato?: string | null;
  manuellAldersjustering: boolean;
  delAvVedtaket: boolean;
  kanOpprette35c: boolean;
  skalOpprette35c: boolean;
}

export interface NotatAndreVoksneIHusstanden {
  opplysningerFraFolkeregisteret: OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeNotatAndreVoksneIHusstandenDetaljerDto[];
  opplysningerBruktTilBeregning: OpplysningerBruktTilBeregningBostatuskode[];
}

export interface NotatAndreVoksneIHusstandenDetaljerDto {
  /** @format int32 */
  totalAntallHusstandsmedlemmer: number;
  husstandsmedlemmer: NotatVoksenIHusstandenDetaljerDto[];
}

export interface NotatBarnetilsynOffentligeOpplysninger {
  periode: TypeArManedsperiode;
  tilsynstype?: "HELTID" | "DELTID" | "IKKE_ANGITT";
  skolealder?: "OVER" | "UNDER" | "IKKE_ANGITT";
}

/** Notat begrunnelse skrevet av saksbehandler */
export interface NotatBegrunnelseDto {
  innhold?: string | null;
  innholdFraOmgjortVedtak?: string | null;
  /** @deprecated */
  intern?: string | null;
  gjelder?: DokumentmalPersonDto | null;
}

export interface NotatBehandlingDetaljerDto {
  /** @format date */
  klageMottattDato?: string | null;
  vedtakstype?: Vedtakstype | null;
  opprinneligVedtakstype?: Vedtakstype | null;
  kategori?: NotatSaerbidragKategoriDto | null;
  søktAv?: SoktAvType | null;
  innkreving: boolean;
  /** @format date */
  mottattDato?: string | null;
  søktFraDato?: string | null;
  søknadstype?: string | null;
  /**
   * Hent informasjon fra virkningstidspunkt
   * @deprecated
   * @format date
   */
  virkningstidspunkt?: string | null;
  avslag?: Resultatkode | null;
  avslagVisningsnavnUtenPrefiks?: string | null;
  vedtakstypeVisningsnavn?: string | null;
  kategoriVisningsnavn?: string | null;
  avslagVisningsnavn?: string | null;
  erAvvisning: boolean;
}

export interface NotatBeregnetBidragPerBarnDto {
  beregnetBidragPerBarn: BeregnetBidragPerBarn;
  personidentBarn: string;
}

export interface NotatBeregnetInntektDto {
  gjelderBarn: DokumentmalPersonDto;
  summertInntektListe: DelberegningSumInntekt[];
}

export interface NotatBeregnetPrivatAvtalePeriodeDto {
  periode: DatoperiodeDto;
  indeksfaktor: number;
  beløp: number;
}

export interface NotatBeregningsdetaljerSamvaersfradrag {
  samværsfradrag: number;
  samværsklasse: Samvaersklasse;
  gjennomsnittligSamværPerMåned: number;
  samværsklasseVisningsnavn: string;
}

export interface NotatBoforholdDto {
  barn: BoforholdBarn[];
  andreVoksneIHusstanden?: NotatAndreVoksneIHusstanden | null;
  boforholdBMSøknadsbarn: NotatBoforholdTilBMMedSoknadsbarn[];
  sivilstand: NotatSivilstand;
  /** Notat begrunnelse skrevet av saksbehandler */
  begrunnelse: NotatBegrunnelseDto;
  /**
   * Bruk begrunnelse
   * @deprecated
   */
  notat: NotatBegrunnelseDto;
  beregnetBoforhold: DelberegningBoforhold[];
}

export interface NotatBoforholdTilBMMedSoknadsbarn {
  gjelderBarn: DokumentmalPersonDto;
  perioder: OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeUnit[];
}

export interface NotatFaktiskTilsynsutgiftDto {
  periode: DatoperiodeDto;
  utgift: number;
  kostpenger?: number | null;
  kommentar?: string | null;
  total: number;
}

export interface NotatGebyrDetaljerDto {
  søknad?: NotatGebyrSoknadDetaljerDto | null;
  inntekt: NotatGebyrInntektDto;
  manueltOverstyrtGebyr?: NotatManueltOverstyrGebyrDto | null;
  beregnetIlagtGebyr: boolean;
  endeligIlagtGebyr: boolean;
  begrunnelse?: string | null;
  beløpGebyrsats: number;
  /** @deprecated */
  rolle: DokumentmalPersonDto;
  gebyrResultatVisningsnavn: string;
  erManueltOverstyrt: boolean;
}

export interface NotatGebyrInntektDto {
  skattepliktigInntekt: number;
  maksBarnetillegg?: number | null;
  totalInntekt: number;
}

export interface NotatGebyrRolleV2Dto {
  gebyrDetaljer: NotatGebyrDetaljerDto[];
  rolle: DokumentmalPersonDto;
}

export interface NotatGebyrSakDto {
  saksnummer: string;
  gebyrRoller: NotatGebyrDetaljerDto[];
  gebyr18År: NotatGebyrDetaljerDto[];
}

export interface NotatGebyrSoknadDetaljerDto {
  saksnummer: string;
  /** @format int64 */
  søknadsid: number;
  /** @format date */
  mottattDato: string;
  /** @format date */
  søknadFomDato?: string | null;
  søktAvType: SoktAvType;
  behandlingstype?: Behandlingstype | null;
  behandlingstema?: Behandlingstema | null;
  behandlingstypeVisningsnavn?: string | null;
  søktAvTypeVisningsnavn?: string | null;
  behandlingstemaVisningsnavn?: string | null;
}

export interface NotatGebyrV2Dto {
  gebyrRoller: NotatGebyrRolleV2Dto[];
}

export interface NotatGebyrV3Dto {
  saker: NotatGebyrSakDto[];
}

export interface NotatInntektDto {
  periode?: TypeArManedsperiode | null;
  opprinneligPeriode?: TypeArManedsperiode | null;
  beløp: number;
  kilde: Kilde;
  type: Inntektsrapportering;
  medIBeregning: boolean;
  gjelderBarn?: DokumentmalPersonDto | null;
  historisk: boolean;
  inntektsposter: NotatInntektspostDto[];
  beløpstypeVisningsnavn: string;
  visningsnavn: string;
  /** Avrundet dagsats for barnetillegg */
  dagsats?: number | null;
  beløpstype?: InntektBelopstype | null;
  skattefaktor?: number | null;
  /** Avrundet månedsbeløp for barnetillegg */
  beløpMånedDagsats?: number | null;
  /** Avrundet månedsbeløp for barnetillegg */
  månedsbeløp?: number | null;
}

export interface NotatInntekterDto {
  inntekterPerRolle: InntekterPerRolle[];
  offentligeInntekterPerRolle: InntekterPerRolle[];
  /** Notat begrunnelse skrevet av saksbehandler */
  notat: NotatBegrunnelseDto;
  /** @uniqueItems true */
  notatPerRolle: NotatBegrunnelseDto[];
  /** @uniqueItems true */
  begrunnelsePerRolle: NotatBegrunnelseDto[];
}

export interface NotatInntektspostDto {
  kode?: string | null;
  inntektstype?: Inntektstype | null;
  beløp: number;
  beløpstype: InntektBelopstype;
  skattefaktor?: number | null;
  visningsnavn?: string | null;
}

export interface NotatMaksGodkjentBelopDto {
  taMed: boolean;
  beløp?: number | null;
  begrunnelse?: string | null;
}

export enum NotatMalType {
  FORSKUDD = "FORSKUDD",
  SAeRBIDRAG = "SÆRBIDRAG",
  BIDRAG = "BIDRAG",
}

export interface NotatManueltOverstyrGebyrDto {
  begrunnelse?: string | null;
  /** Skal bare settes hvis det er avslag */
  ilagtGebyr?: boolean | null;
}

export interface NotatOffentligeOpplysningerUnderhold {
  offentligeOpplysningerBarn: NotatOffentligeOpplysningerUnderholdBarn[];
  andreBarnTilBidragsmottaker: DokumentmalPersonDto[];
  bidragsmottakerHarInnvilgetTilleggsstønad: boolean;
}

export interface NotatOffentligeOpplysningerUnderholdBarn {
  gjelder: DokumentmalPersonDto;
  gjelderBarn?: DokumentmalPersonDto | null;
  barnetilsyn: NotatBarnetilsynOffentligeOpplysninger[];
  harTilleggsstønad: boolean;
}

export interface NotatPrivatAvtaleDto {
  gjelderBarn: DokumentmalPersonDto;
  /** @format date */
  avtaleDato?: string | null;
  avtaleType?: PrivatAvtaleType | null;
  skalIndeksreguleres: boolean;
  utlandsbidrag: boolean;
  begrunnelse?: NotatBegrunnelseDto | null;
  perioder: NotatPrivatAvtalePeriodeDto[];
  vedtakslisteUtenInnkreving: DokumentmalManuellVedtak[];
  beregnetPrivatAvtalePerioder: NotatBeregnetPrivatAvtalePeriodeDto[];
  avtaleTypeVisningsnavn?: string | null;
}

export interface NotatPrivatAvtalePeriodeDto {
  periode: DatoperiodeDto;
  beløp: number;
  samværsklasse?: Samvaersklasse | null;
  valutakode?: Valutakode | null;
}

export type NotatResultatForskuddBeregningBarnDto = UtilRequiredKeys<VedtakResultatInnhold, "type"> & {
  barn: DokumentmalPersonDto;
  perioder: NotatResultatPeriodeDto[];
};

export interface NotatResultatPeriodeDto {
  periode: TypeArManedsperiode;
  beløp: number;
  resultatKode: Resultatkode;
  regel: string;
  sivilstand?: Sivilstandskode | null;
  inntekt: number;
  vedtakstype?: Vedtakstype | null;
  /** @format int32 */
  antallBarnIHusstanden: number;
  resultatKodeVisningsnavn: string;
  sivilstandVisningsnavn?: string | null;
}

export type NotatResultatSaerbidragsberegningDto = UtilRequiredKeys<VedtakResultatInnhold, "type"> & {
  periode: TypeArManedsperiode;
  bpsAndel?: DelberegningBidragspliktigesAndel | null;
  beregning?: UtgiftBeregningDto | null;
  forskuddssats?: number | null;
  maksGodkjentBeløp?: number | null;
  inntekter?: DokumentmalResultatBeregningInntekterDto | null;
  delberegningBidragspliktigesBeregnedeTotalbidrag?: DokumentmalDelberegningBidragspliktigesBeregnedeTotalbidragDto | null;
  delberegningBidragsevne?: DokumentmalDelberegningBidragsevneDto | null;
  delberegningUtgift?: DelberegningUtgift | null;
  resultat: number;
  resultatKode: Resultatkode;
  /** @format double */
  antallBarnIHusstanden?: number | null;
  voksenIHusstanden?: boolean | null;
  enesteVoksenIHusstandenErEgetBarn?: boolean | null;
  erDirekteAvslag: boolean;
  bpHarEvne: boolean;
  resultatVisningsnavn: string;
  beløpSomInnkreves: number;
};

export interface NotatSamvaerBarnDto {
  gjelderBarn: DokumentmalPersonDto;
  begrunnelse?: NotatBegrunnelseDto | null;
  perioder: NotatSamvaersperiodeDto[];
}

export interface NotatSamvaerDto {
  erSammeForAlle: boolean;
  barn: NotatSamvaerBarnDto[];
}

export interface NotatSamvaersperiodeDto {
  periode: DatoperiodeDto;
  samværsklasse: Samvaersklasse;
  gjennomsnittligSamværPerMåned: number;
  beregning?: SamvaerskalkulatorDetaljer | null;
  samværsklasseVisningsnavn: string;
  ferieVisningsnavnMap: Record<string, string>;
  frekvensVisningsnavnMap: Record<string, string>;
}

export interface NotatSivilstand {
  opplysningerFraFolkeregisteret: OpplysningerFraFolkeregisteretMedDetaljerSivilstandskodePDLUnit[];
  opplysningerBruktTilBeregning: OpplysningerBruktTilBeregningSivilstandskode[];
}

export interface NotatStonadTilBarnetilsynDto {
  periode: DatoperiodeDto;
  skolealder: "OVER" | "UNDER" | "IKKE_ANGITT";
  tilsynstype: "HELTID" | "DELTID" | "IKKE_ANGITT";
  kilde: Kilde;
  skoleaderVisningsnavn: string;
  tilsynstypeVisningsnavn: string;
}

export interface NotatSaerbidragKategoriDto {
  kategori: Saerbidragskategori;
  beskrivelse?: string | null;
}

export interface NotatSaerbidragUtgifterDto {
  beregning?: NotatUtgiftBeregningDto | null;
  maksGodkjentBeløp?: NotatMaksGodkjentBelopDto | null;
  /** Notat begrunnelse skrevet av saksbehandler */
  begrunnelse: NotatBegrunnelseDto;
  /**
   * Bruk begrunnelse
   * @deprecated
   */
  notat: NotatBegrunnelseDto;
  utgifter: NotatUtgiftspostDto[];
  totalBeregning: NotatTotalBeregningUtgifterDto[];
}

export interface NotatTilleggsstonadDto {
  periode: DatoperiodeDto;
  dagsats?: number | null;
  beløp?: number | null;
  beløpstype: InntektBelopstype;
  total: number;
  beløpstypeVisningsnavn: string;
}

export interface NotatTilsynsutgiftBarn {
  gjelderBarn: DokumentmalPersonDto;
  totalTilsynsutgift: number;
  faktiskUtgiftBeregnet: number;
  beløp: number;
  tilleggsstønadDagsats?: number | null;
  tilleggsstønadBeløp?: number | null;
  beløpstype?: InntektBelopstype | null;
  kostpenger?: number | null;
  tilleggsstønad?: number | null;
}

export interface NotatTotalBeregningUtgifterDto {
  betaltAvBp: boolean;
  utgiftstype: string;
  totalKravbeløp: number;
  totalGodkjentBeløp: number;
  utgiftstypeVisningsnavn: string;
}

export interface NotatUnderholdBarnDto {
  gjelderBarn: DokumentmalPersonDto;
  harTilsynsordning?: boolean | null;
  stønadTilBarnetilsyn: NotatStonadTilBarnetilsynDto[];
  faktiskTilsynsutgift: NotatFaktiskTilsynsutgiftDto[];
  tilleggsstønad: NotatTilleggsstonadDto[];
  underholdskostnad: NotatUnderholdskostnadBeregningDto[];
  begrunnelse?: NotatBegrunnelseDto | null;
}

export interface NotatUnderholdDto {
  underholdskostnaderBarn: NotatUnderholdBarnDto[];
  offentligeOpplysninger: NotatOffentligeOpplysningerUnderholdBarn[];
  offentligeOpplysningerV2: NotatOffentligeOpplysningerUnderhold;
}

export interface NotatUnderholdskostnadBeregningDto {
  periode: DatoperiodeDto;
  forbruk: number;
  boutgifter: number;
  stønadTilBarnetilsyn: number;
  tilsynsutgifter: number;
  barnetrygd: number;
  total: number;
  beregningsdetaljer?: NotatUnderholdskostnadPeriodeBeregningsdetaljer | null;
}

export interface NotatUnderholdskostnadPeriodeBeregningsdetaljer {
  tilsynsutgifterBarn: NotatTilsynsutgiftBarn[];
  sjablonMaksTilsynsutgift: number;
  sjablonMaksFradrag: number;
  /** @format int32 */
  antallBarnBMUnderTolvÅr: number;
  /** @format int32 */
  antallBarnBMBeregnet: number;
  /** @format int32 */
  antallBarnMedTilsynsutgifter: number;
  skattesatsFaktor: number;
  totalTilsynsutgift: number;
  sumTilsynsutgifter: number;
  bruttoTilsynsutgift: number;
  justertBruttoTilsynsutgift: number;
  nettoTilsynsutgift: number;
  erBegrensetAvMaksTilsyn: boolean;
  fordelingFaktor: number;
  skattefradragPerBarn: number;
  maksfradragAndel: number;
  skattefradrag: number;
  skattefradragMaksFradrag: number;
  skattefradragTotalTilsynsutgift: number;
}

export interface NotatUtgiftBeregningDto {
  /** Beløp som er direkte betalt av BP */
  beløpDirekteBetaltAvBp: number;
  /** Summen av godkjente beløp som brukes for beregningen */
  totalGodkjentBeløp: number;
  /** Summen av kravbeløp */
  totalKravbeløp: number;
  /** Summen av godkjente beløp som brukes for beregningen */
  totalGodkjentBeløpBp?: number | null;
  /** Summen av godkjent beløp for utgifter BP har betalt plus beløp som er direkte betalt av BP */
  totalBeløpBetaltAvBp: number;
}

export interface NotatUtgiftspostDto {
  /**
   * Når utgifter gjelder. Kan være feks dato på kvittering
   * @format date
   */
  dato: string;
  /** Type utgift. Kan feks være hva som ble kjøpt for kravbeløp (bugnad, klær, sko, etc) */
  type: Utgiftstype | string;
  /** Beløp som er betalt for utgiften det gjelder */
  kravbeløp: number;
  /** Beløp som er godkjent for beregningen */
  godkjentBeløp: number;
  /** Begrunnelse for hvorfor godkjent beløp avviker fra kravbeløp. Må settes hvis godkjent beløp er ulik kravbeløp */
  begrunnelse?: string | null;
  /** Om utgiften er betalt av BP */
  betaltAvBp: boolean;
  utgiftstypeVisningsnavn: string;
}

export interface NotatVedtakDetaljerDto {
  erFattet: boolean;
  fattetAvSaksbehandler?: string | null;
  /** @format date-time */
  fattetTidspunkt?: string | null;
  kanFatteVedtakForRevurderingsbarn: boolean;
  skalFatteVedtakForRevurderingsbarn: boolean;
  manueltOverstyrtFatteVedtakRevurderingsbarnBegrunnelse?: string | null;
  resultat: (
    | DokumentmalResultatBidragsberegningBarnDto
    | NotatResultatForskuddBeregningBarnDto
    | NotatResultatSaerbidragsberegningDto
  )[];
}

export interface NotatVirkningstidspunktBarnDto {
  rolle: DokumentmalPersonDto;
  stønadstype?: Stonadstype | null;
  behandlingstype?: Behandlingstype | null;
  /**
   * Bruk behandlingstype
   * @deprecated
   */
  søknadstype?: string | null;
  vedtakstype?: Vedtakstype | null;
  innkreving: boolean;
  søktAv?: SoktAvType | null;
  /**
   * @format date
   * @example "01.12.2025"
   */
  mottattDato?: string | null;
  /**
   * @format date
   * @example "01.12.2025"
   */
  søktFraDato?: string | null;
  beregnTilDato?: string | null;
  opphørsdato?: string | null;
  beregnTil?: BeregnTil | null;
  etterfølgendeVedtakVirkningstidspunkt?: string | null;
  /**
   * @format date
   * @example "01.12.2025"
   */
  virkningstidspunkt?: string | null;
  avslag?: Resultatkode | null;
  årsak?: TypeArsakstype | null;
  /** Notat begrunnelse skrevet av saksbehandler */
  begrunnelse: NotatBegrunnelseDto;
  begrunnelseVurderingAvSkolegang?: NotatBegrunnelseDto | null;
  /**
   * Bruk begrunnelse
   * @deprecated
   */
  notat: NotatBegrunnelseDto;
  behandlingstypeVisningsnavn?: string | null;
  avslagVisningsnavnUtenPrefiks?: string | null;
  avslagVisningsnavn?: string | null;
  erAvvisning: boolean;
  årsakVisningsnavn?: string | null;
}

export interface NotatVirkningstidspunktDto {
  /** Hvis det er likt for alle bruk avslag/årsak fra ett av barna */
  erLikForAlle: boolean;
  erVirkningstidspunktLikForAlle: boolean;
  erAvslagForAlle: boolean;
  eldsteVirkningstidspunkt: string;
  barn: NotatVirkningstidspunktBarnDto[];
}

export interface NotatVoksenIHusstandenDetaljerDto {
  navn: string;
  /** @format date */
  fødselsdato?: string | null;
  erBeskyttet: boolean;
  harRelasjonTilBp: boolean;
}

export interface OpplysningerBruktTilBeregningBostatuskode {
  periode: TypeArManedsperiode;
  status: Bostatuskode;
  kilde: Kilde;
  statusVisningsnavn?: string | null;
}

export interface OpplysningerBruktTilBeregningSivilstandskode {
  periode: TypeArManedsperiode;
  status: Sivilstandskode;
  kilde: Kilde;
  statusVisningsnavn?: string | null;
}

export interface OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeNotatAndreVoksneIHusstandenDetaljerDto {
  periode: TypeArManedsperiode;
  status?: Bostatuskode | null;
  detaljer?: NotatAndreVoksneIHusstandenDetaljerDto | null;
  statusVisningsnavn?: string | null;
}

export interface OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeUnit {
  periode: TypeArManedsperiode;
  status?: Bostatuskode | null;
  statusVisningsnavn?: string | null;
}

export interface OpplysningerFraFolkeregisteretMedDetaljerSivilstandskodePDLUnit {
  periode: TypeArManedsperiode;
  status?: SivilstandskodePDL | null;
  statusVisningsnavn?: string | null;
}

export interface PeriodeSlattUtTilFF {
  periode: TypeArManedsperiode;
  erEvneJustertNedTil25ProsentAvInntekt: boolean;
}

export enum PrivatAvtaleType {
  PRIVAT_AVTALE = "PRIVAT_AVTALE",
  DOM_RETTSFORLIK = "DOM_RETTSFORLIK",
  VEDTAK_FRA_NAV = "VEDTAK_FRA_NAV",
}

export interface ResultatBarnebidragsberegningPeriodeDto {
  periode: TypeArManedsperiode;
  erSistePeriode: boolean;
  underholdskostnad: number;
  bpsAndelU: number;
  bpsAndelBeløp: number;
  samværsfradrag: number;
  beregnetBidrag: number;
  faktiskBidrag: number;
  resultatKode?: Resultatkode | null;
  erOpphør: boolean;
  erDirekteAvslag: boolean;
  vedtakstype: Vedtakstype;
  beregningsdetaljer?: BidragPeriodeBeregningsdetaljer | null;
  klageOmgjøringDetaljer?: KlageOmgjoringDetaljer | null;
  delvedtakstypeVisningsnavn: string;
  resultatkodeVisningsnavn: string;
  resultatFraVedtak?: ResultatFraVedtakGrunnlag | null;
}

export interface ResultatFraVedtakGrunnlag {
  /** @format int32 */
  vedtaksid?: number | null;
  omgjøringsvedtak: boolean;
  beregnet: boolean;
  opprettParagraf35c: boolean;
  /** @format date-time */
  vedtakstidspunkt?: string | null;
  vedtakstype?: Vedtakstype | null;
}

export enum Resultatkode {
  OPPHOR = "OPPHØR",
  GEBYR_FRITATT = "GEBYR_FRITATT",
  GEBYR_ILAGT = "GEBYR_ILAGT",
  BARNETERSELVFORSORGET = "BARNET_ER_SELVFORSØRGET",
  DIREKTEOPPGJOR = "DIREKTE_OPPGJØR",
  IKKE_DOKUMENTERT_SKOLEGANG = "IKKE_DOKUMENTERT_SKOLEGANG",
  AVSLUTTET_SKOLEGANG = "AVSLUTTET_SKOLEGANG",
  IKKESTERKNOKGRUNNOGBIDRAGETHAROPPHORT = "IKKE_STERK_NOK_GRUNN_OG_BIDRAGET_HAR_OPPHØRT",
  IKKE_OMSORG_FOR_BARNET = "IKKE_OMSORG_FOR_BARNET",
  PARTENE_BOR_SAMMEN = "PARTENE_BOR_SAMMEN",
  BARNETERDODT = "BARNET_ER_DØDT",
  BIDRAGSMOTTAKER_HAR_OMSORG_FOR_BARNET = "BIDRAGSMOTTAKER_HAR_OMSORG_FOR_BARNET",
  BIDRAGSPLIKTIGERDOD = "BIDRAGSPLIKTIG_ER_DØD",
  BEREGNET_BIDRAG = "BEREGNET_BIDRAG",
  REDUSERTFORSKUDD50PROSENT = "REDUSERT_FORSKUDD_50_PROSENT",
  ORDINAeRTFORSKUDD75PROSENT = "ORDINÆRT_FORSKUDD_75_PROSENT",
  FORHOYETFORSKUDD100PROSENT = "FORHØYET_FORSKUDD_100_PROSENT",
  FORHOYETFORSKUDD11AR125PROSENT = "FORHØYET_FORSKUDD_11_ÅR_125_PROSENT",
  SAeRTILSKUDDINNVILGET = "SÆRTILSKUDD_INNVILGET",
  SAeRBIDRAGINNVILGET = "SÆRBIDRAG_INNVILGET",
  SAeRTILSKUDDIKKEFULLBIDRAGSEVNE = "SÆRTILSKUDD_IKKE_FULL_BIDRAGSEVNE",
  SAeRBIDRAGIKKEFULLBIDRAGSEVNE = "SÆRBIDRAG_IKKE_FULL_BIDRAGSEVNE",
  SAeRBIDRAGMANGLERBIDRAGSEVNE = "SÆRBIDRAG_MANGLER_BIDRAGSEVNE",
  AVSLAG = "AVSLAG",
  AVSLAG2 = "AVSLAG2",
  PARTENBEROMOPPHOR = "PARTEN_BER_OM_OPPHØR",
  AVSLAGOVER18AR = "AVSLAG_OVER_18_ÅR",
  AVSLAGIKKEREGISTRERTPAADRESSE = "AVSLAG_IKKE_REGISTRERT_PÅ_ADRESSE",
  AVSLAGHOYINNTEKT = "AVSLAG_HØY_INNTEKT",
  PAGRUNNAVBARNEPENSJON = "PÅ_GRUNN_AV_BARNEPENSJON",
  IKKE_OMSORG = "IKKE_OMSORG",
  BARNETS_EKTESKAP = "BARNETS_EKTESKAP",
  BARNETS_INNTEKT = "BARNETS_INNTEKT",
  PAGRUNNAVYTELSEFRAFOLKETRYGDEN = "PÅ_GRUNN_AV_YTELSE_FRA_FOLKETRYGDEN",
  FULLT_UNDERHOLDT_AV_OFFENTLIG = "FULLT_UNDERHOLDT_AV_OFFENTLIG",
  IKKE_OPPHOLD_I_RIKET = "IKKE_OPPHOLD_I_RIKET",
  MANGLENDE_DOKUMENTASJON = "MANGLENDE_DOKUMENTASJON",
  BARNETANSESABOSAMMENMEDBEGGEFORELDRE = "BARNET_ANSES_Å_BO_SAMMEN_MED_BEGGE_FORELDRE",
  OPPHOLD_I_UTLANDET = "OPPHOLD_I_UTLANDET",
  UTENLANDSK_YTELSE = "UTENLANDSK_YTELSE",
  AVSLAG_PRIVAT_AVTALE_BIDRAG = "AVSLAG_PRIVAT_AVTALE_BIDRAG",
  IKKESOKTOMINNKREVINGAVBIDRAG = "IKKE_SØKT_OM_INNKREVING_AV_BIDRAG",
  IKKE_INNKREVING_AV_BIDRAG = "IKKE_INNKREVING_AV_BIDRAG",
  UTGIFTER_DEKKES_AV_BARNEBIDRAGET = "UTGIFTER_DEKKES_AV_BARNEBIDRAGET",
  IKKENODVENDIGEUTGIFTER = "IKKE_NØDVENDIGE_UTGIFTER",
  PRIVAT_AVTALE = "PRIVAT_AVTALE",
  AVSLAGPRIVATAVTALEOMSAeRBIDRAG = "AVSLAG_PRIVAT_AVTALE_OM_SÆRBIDRAG",
  ALLE_UTGIFTER_ER_FORELDET = "ALLE_UTGIFTER_ER_FORELDET",
  GODKJENTBELOPERLAVEREENNFORSKUDDSSATS = "GODKJENT_BELØP_ER_LAVERE_ENN_FORSKUDDSSATS",
  INGEN_ENDRING_UNDER_GRENSE = "INGEN_ENDRING_UNDER_GRENSE",
  INNVILGET_VEDTAK = "INNVILGET_VEDTAK",
  SKJONNUTLANDET = "SKJØNN_UTLANDET",
  LAVERE_ENN_INNTEKTSEVNE_BEGGE_PARTER = "LAVERE_ENN_INNTEKTSEVNE_BEGGE_PARTER",
  LAVERE_ENN_INNTEKTSEVNE_BIDRAGSPLIKTIG = "LAVERE_ENN_INNTEKTSEVNE_BIDRAGSPLIKTIG",
  LAVERE_ENN_INNTEKTSEVNE_BIDRAGSMOTTAKER = "LAVERE_ENN_INNTEKTSEVNE_BIDRAGSMOTTAKER",
  MANGLER_DOKUMENTASJON_AV_INNTEKT_BEGGE_PARTER = "MANGLER_DOKUMENTASJON_AV_INNTEKT_BEGGE_PARTER",
  MANGLER_DOKUMENTASJON_AV_INNTEKT_BIDRAGSPLIKTIG = "MANGLER_DOKUMENTASJON_AV_INNTEKT_BIDRAGSPLIKTIG",
  MANGLER_DOKUMENTASJON_AV_INNTEKT_BIDRAGSMOTTAKER = "MANGLER_DOKUMENTASJON_AV_INNTEKT_BIDRAGSMOTTAKER",
  INNTIL1ARTILBAKE = "INNTIL_1_ÅR_TILBAKE",
  MAKS25PROSENTAVINNTEKT = "MAKS_25_PROSENT_AV_INNTEKT",
  MANGLER_BIDRAGSEVNE = "MANGLER_BIDRAGSEVNE",
  KOSTNADSBEREGNET_BIDRAG = "KOSTNADSBEREGNET_BIDRAG",
  INNKREVINGSGRUNNLAG = "INNKREVINGSGRUNNLAG",
  INDEKSREGULERING = "INDEKSREGULERING",
  BIDRAG_JUSTERT_FOR_DELT_BOSTED = "BIDRAG_JUSTERT_FOR_DELT_BOSTED",
  BIDRAG_JUSTERT_FOR_NETTO_BARNETILLEGG_BP = "BIDRAG_JUSTERT_FOR_NETTO_BARNETILLEGG_BP",
  BIDRAG_JUSTERT_FOR_NETTO_BARNETILLEGG_BM = "BIDRAG_JUSTERT_FOR_NETTO_BARNETILLEGG_BM",
  BIDRAG_JUSTERT_TIL_FORSKUDDSSATS = "BIDRAG_JUSTERT_TIL_FORSKUDDSSATS",
  BIDRAG_JUSTERT_MANUELT_TIL_FORSKUDDSSATS = "BIDRAG_JUSTERT_MANUELT_TIL_FORSKUDDSSATS",
}

export enum Rolletype {
  BA = "BA",
  BM = "BM",
  BP = "BP",
  FR = "FR",
  RM = "RM",
}

export interface SamvaerskalkulatorDetaljer {
  ferier: SamvaerskalkulatorFerie[];
  regelmessigSamværNetter: number;
}

export interface SamvaerskalkulatorFerie {
  type: SamvaerskalkulatorFerietype;
  bidragsmottakerNetter: number;
  bidragspliktigNetter: number;
  frekvens: SamvaerskalkulatorNetterFrekvens;
}

export enum SamvaerskalkulatorFerietype {
  JULNYTTAR = "JUL_NYTTÅR",
  VINTERFERIE = "VINTERFERIE",
  PASKE = "PÅSKE",
  SOMMERFERIE = "SOMMERFERIE",
  HOSTFERIE = "HØSTFERIE",
  ANNET = "ANNET",
}

export enum SamvaerskalkulatorNetterFrekvens {
  HVERTAR = "HVERT_ÅR",
  ANNETHVERTAR = "ANNET_HVERT_ÅR",
}

export enum Samvaersklasse {
  SAMVAeRSKLASSE0 = "SAMVÆRSKLASSE_0",
  SAMVAeRSKLASSE1 = "SAMVÆRSKLASSE_1",
  SAMVAeRSKLASSE2 = "SAMVÆRSKLASSE_2",
  SAMVAeRSKLASSE3 = "SAMVÆRSKLASSE_3",
  SAMVAeRSKLASSE4 = "SAMVÆRSKLASSE_4",
  DELT_BOSTED = "DELT_BOSTED",
}

export enum Sivilstandskode {
  GIFT_SAMBOER = "GIFT_SAMBOER",
  BOR_ALENE_MED_BARN = "BOR_ALENE_MED_BARN",
  ENSLIG = "ENSLIG",
  SAMBOER = "SAMBOER",
  UKJENT = "UKJENT",
}

export enum SivilstandskodePDL {
  GIFT = "GIFT",
  UGIFT = "UGIFT",
  UOPPGITT = "UOPPGITT",
  ENKE_ELLER_ENKEMANN = "ENKE_ELLER_ENKEMANN",
  SKILT = "SKILT",
  SEPARERT = "SEPARERT",
  REGISTRERT_PARTNER = "REGISTRERT_PARTNER",
  SEPARERT_PARTNER = "SEPARERT_PARTNER",
  SKILT_PARTNER = "SKILT_PARTNER",
  GJENLEVENDE_PARTNER = "GJENLEVENDE_PARTNER",
}

export interface SluttberegningBarnebidragAldersjustering {
  periode: TypeArManedsperiode;
  beregnetBeløp: number;
  resultatBeløp: number;
  bpAndelBeløp: number;
  bpAndelFaktorVedDeltBosted?: number | null;
  deltBosted: boolean;
}

export interface SluttberegningIndeksregulering {
  periode: TypeArManedsperiode;
  beløp: Belop;
  originaltBeløp: Belop;
  nesteIndeksreguleringsår?: string | null;
}

export enum Stonadstype {
  BIDRAG = "BIDRAG",
  FORSKUDD = "FORSKUDD",
  BIDRAG18AAR = "BIDRAG18AAR",
  EKTEFELLEBIDRAG = "EKTEFELLEBIDRAG",
  MOTREGNING = "MOTREGNING",
  OPPFOSTRINGSBIDRAG = "OPPFOSTRINGSBIDRAG",
}

export enum Saerbidragskategori {
  KONFIRMASJON = "KONFIRMASJON",
  TANNREGULERING = "TANNREGULERING",
  OPTIKK = "OPTIKK",
  ANNET = "ANNET",
}

export enum SoktAvType {
  BIDRAGSMOTTAKER = "BIDRAGSMOTTAKER",
  BIDRAGSPLIKTIG = "BIDRAGSPLIKTIG",
  BARN18AR = "BARN_18_ÅR",
  BM_I_ANNEN_SAK = "BM_I_ANNEN_SAK",
  NAV_BIDRAG = "NAV_BIDRAG",
  FYLKESNEMDA = "FYLKESNEMDA",
  NAV_INTERNASJONALT = "NAV_INTERNASJONALT",
  KOMMUNE = "KOMMUNE",
  NORSKE_MYNDIGHET = "NORSKE_MYNDIGHET",
  UTENLANDSKE_MYNDIGHET = "UTENLANDSKE_MYNDIGHET",
  VERGE = "VERGE",
  TRYGDEETATEN_INNKREVING = "TRYGDEETATEN_INNKREVING",
  KLAGE_ANKE = "KLAGE_ANKE",
  KONVERTERING = "KONVERTERING",
}

export interface UtgiftBeregningDto {
  /** Beløp som er direkte betalt av BP */
  beløpDirekteBetaltAvBp: number;
  /** Summen av godkjente beløp som brukes for beregningen */
  totalGodkjentBeløp: number;
  /** Summen av kravbeløp */
  totalKravbeløp: number;
  /** Summen av godkjente beløp som brukes for beregningen */
  totalGodkjentBeløpBp?: number | null;
  /** Summen av godkjent beløp for utgifter BP har betalt plus beløp som er direkte betalt av BP */
  totalBeløpBetaltAvBp: number;
}

export enum Utgiftstype {
  KONFIRMASJONSAVGIFT = "KONFIRMASJONSAVGIFT",
  KONFIRMASJONSLEIR = "KONFIRMASJONSLEIR",
  SELSKAP = "SELSKAP",
  KLAeR = "KLÆR",
  REISEUTGIFT = "REISEUTGIFT",
  TANNREGULERING = "TANNREGULERING",
  OPTIKK = "OPTIKK",
  ANNET = "ANNET",
}

export enum Valutakode {
  ALL = "ALL",
  ANG = "ANG",
  AUD = "AUD",
  BAM = "BAM",
  BGN = "BGN",
  BRL = "BRL",
  CAD = "CAD",
  CHF = "CHF",
  CNY = "CNY",
  CZK = "CZK",
  DKK = "DKK",
  EEK = "EEK",
  EUR = "EUR",
  GBP = "GBP",
  HKD = "HKD",
  HRK = "HRK",
  HUF = "HUF",
  INR = "INR",
  ISK = "ISK",
  JPY = "JPY",
  LTL = "LTL",
  LVL = "LVL",
  MAD = "MAD",
  NOK = "NOK",
  NZD = "NZD",
  PKR = "PKR",
  PLN = "PLN",
  RON = "RON",
  RSD = "RSD",
  SEK = "SEK",
  THB = "THB",
  TND = "TND",
  TRY = "TRY",
  UAH = "UAH",
  USD = "USD",
  VND = "VND",
  ZAR = "ZAR",
  PHP = "PHP",
}

export interface VedtakNotatDto {
  type: NotatMalType;
  erOrkestrertVedtak: boolean;
  stønadstype?: Stonadstype | null;
  medInnkreving: boolean;
  saksnummer: string;
  behandling: NotatBehandlingDetaljerDto;
  saksbehandlerNavn?: string | null;
  virkningstidspunkt: NotatVirkningstidspunktDto;
  utgift?: NotatSaerbidragUtgifterDto | null;
  boforhold: NotatBoforholdDto;
  samvær: NotatSamvaerBarnDto[];
  samværV2?: NotatSamvaerDto | null;
  gebyr?: any[] | null;
  gebyrV2?: NotatGebyrV2Dto | null;
  gebyrV3?: NotatGebyrV3Dto | null;
  underholdskostnader?: NotatUnderholdDto | null;
  personer: DokumentmalPersonDto[];
  privatavtale: NotatPrivatAvtaleDto[];
  roller: DokumentmalPersonDto[];
  inntekter: NotatInntekterDto;
  vedtak: NotatVedtakDetaljerDto;
}

export interface VedtakResultatInnhold {
  type: NotatMalType;
}

export enum Vedtakstype {
  INDEKSREGULERING = "INDEKSREGULERING",
  ALDERSJUSTERING = "ALDERSJUSTERING",
  OPPHOR = "OPPHØR",
  ALDERSOPPHOR = "ALDERSOPPHØR",
  REVURDERING = "REVURDERING",
  FASTSETTELSE = "FASTSETTELSE",
  INNKREVING = "INNKREVING",
  KLAGE = "KLAGE",
  ENDRING = "ENDRING",
  ENDRING_MOTTAKER = "ENDRING_MOTTAKER",
}

export interface Visningsnavn {
  intern: string;
  bruker: Record<string, string>;
}

export interface TypeArManedsperiode {
  /**
   * @pattern YYYY-MM
   * @example "2023-01"
   */
  fom: string;
  /**
   * @pattern YYYY-MM
   * @example "2023-01"
   */
  til?: string | null;
}

export enum TypeArsakstype {
  FRABARNETSFODSEL = "FRA_BARNETS_FØDSEL",
  FRA_SAMLIVSBRUDD = "FRA_SAMLIVSBRUDD",
  FRABARNETSFLYTTEMANED = "FRA_BARNETS_FLYTTEMÅNED",
  FRAMANEDENETTERFYLTE18AR = "FRA_MÅNEDEN_ETTER_FYLTE_18_ÅR",
  FRA_KRAVFREMSETTELSE = "FRA_KRAVFREMSETTELSE",
  TREMANEDERTILBAKE = "TRE_MÅNEDER_TILBAKE",
  FRASOKNADSTIDSPUNKT = "FRA_SØKNADSTIDSPUNKT",
  TREARSREGELEN = "TRE_ÅRS_REGELEN",
  FRA_OPPHOLDSTILLATELSE = "FRA_OPPHOLDSTILLATELSE",
  AUTOMATISK_JUSTERING = "AUTOMATISK_JUSTERING",
  FRASAMMEMANEDSOMINNTEKTENBLEREDUSERT = "FRA_SAMME_MÅNED_SOM_INNTEKTEN_BLE_REDUSERT",
  FRAMANEDETTERENDRETSOKNAD = "FRA_MÅNED_ETTER_ENDRET_SØKNAD",
  FORHOYELSETILBAKEITID = "FORHØYELSE_TILBAKE_I_TID",
  FRAMANEDETTERINNTEKTENOKTE = "FRA_MÅNED_ETTER_INNTEKTEN_ØKTE",
  SOKNADSTIDSPUNKTENDRING = "SØKNADSTIDSPUNKT_ENDRING",
  NEDSETTELSE_TILBAKE_I_TID = "NEDSETTELSE_TILBAKE_I_TID",
  ENDRING3MANEDERTILBAKE = "ENDRING_3_MÅNEDER_TILBAKE",
  AVSLAGFORHOYELSETILBAKE = "AVSLAG_FORHØYELSE_TILBAKE",
  ENDRING3ARSREGELEN = "ENDRING_3_ÅRS_REGELEN",
  AVSLAG_NEDSETTELSE_TILBAKE = "AVSLAG_NEDSETTELSE_TILBAKE",
  TIDLIGERE_FEILAKTIG_AVSLAG = "TIDLIGERE_FEILAKTIG_AVSLAG",
  REVURDERINGMANEDENETTER = "REVURDERING_MÅNEDEN_ETTER",
  ANNET = "ANNET",
  OMREGNING = "OMREGNING",
  PRIVAT_AVTALE = "PRIVAT_AVTALE",
  FRAMANEDENETTERIPAVENTEAVBIDRAGSSAK = "FRA_MÅNEDEN_ETTER_I_PÅVENTE_AV_BIDRAGSSAK",
  FRAMANEDENETTERPRIVATAVTALE = "FRA_MÅNEDEN_ETTER_PRIVAT_AVTALE",
  FRA_ENDRINGSTIDSPUNKT = "FRA_ENDRINGSTIDSPUNKT",
  BIDRAGSPLIKTIGHARIKKEBIDRATTTILFORSORGELSE = "BIDRAGSPLIKTIG_HAR_IKKE_BIDRATT_TIL_FORSØRGELSE",
  MANEDETTERBETALTFORFALTBIDRAG = "MÅNED_ETTER_BETALT_FORFALT_BIDRAG",
}

export interface Adresse {
  adresselinje1: string;
  adresselinje2?: string | null;
  adresselinje3?: string | null;
  adresselinje4?: string | null;
  bruksenhetsnummer?: string | null;
  postnummer?: string | null;
  poststed?: string | null;
  landkode?: string | null;
  landkode3?: string | null;
  land?: string | null;
}

export interface AndelUnderholdskostnadPeriode {
  periode: TypeArManedsperiode;
  inntektBM?: number | null;
  inntektBP?: number | null;
  inntektBarn?: number | null;
  barnEndeligInntekt?: number | null;
  andelFaktor?: number | null;
  beløpUnderholdskostnad?: number | null;
  beløpBpsAndel: number;
  totalEndeligInntekt: number;
}

export type Barn = UtilRequiredKeys<Rolle, "rolle" | "navn" | "type"> & {
  fodselsnummer?: string | null;
  /** @format date */
  fodselsdato?: string | null;
  fornavn?: string | null;
  /** @format int32 */
  bidragsbelop?: number | null;
  /** @format int32 */
  forskuddsbelop?: number | null;
  /** @format int32 */
  gebyrRm?: number | null;
  fodselsnummerRm?: string | null;
};

export interface BarnIHusstandPeriode {
  periode: TypeArManedsperiode;
  /** @format double */
  antall: number;
}

export interface BidragsevnePeriode {
  periode: TypeArManedsperiode;
  sjabloner: BidragsevneSjabloner;
  bidragsevne: number;
  beløpBidrag: number;
  harFullEvne: boolean;
  harDelvisEvne: boolean;
  inntektBP: number;
  underholdEgneBarnIHusstand: UnderholdEgneBarnIHusstand;
  skatt: Skatt;
  borMedAndreVoksne: boolean;
}

export interface BidragsevneSjabloner {
  beløpMinstefradrag: number;
  beløpKlassfradrag: number;
  beløpUnderholdEgneBarnIHusstanden: number;
  boutgiftBeløp: number;
  underholdBeløp: number;
}

/** Bostatus for person */
export interface BostatusPeriode {
  periode: TypeArManedsperiode;
  bostatus: Bostatuskode;
  /** Referanse til BM eller BP som bostatus for personen gjelder for */
  relatertTilPart: string;
  /** Om grunnlaget er manuelt registrert av saksbehandler eller om det er innhentet fra ekstern kilde (skatt/folkregisteret...) */
  manueltRegistrert: boolean;
}

export interface BrevSjablonVerdier {
  forskuddSats: number;
  inntektsgrense: number;
}

export interface Datoperiode {
  /** @format date */
  fom: string;
  /** @format date */
  til?: string | null;
}

export interface DokumentBestilling {
  bestillBatch: boolean;
  mottaker?: Mottaker | null;
  gjelder?: Gjelder | null;
  kontaktInfo?: EnhetKontaktInfo | null;
  saksbehandler?: Saksbehandler | null;
  dokumentreferanse?: string | null;
  tittel?: string | null;
  enhet?: string | null;
  saksnummer?: string | null;
  /** @format date */
  datoSakOpprettet?: string | null;
  spraak?: string | null;
  roller: {
    barn: Barn[];
    bidragsmottaker?: PartInfo | null;
    bidragspliktig?: PartInfo | null;
    isEmpty: boolean;
    /** @format int32 */
    size: number;
    first?: Barn;
    last?: Barn;
  };
  rollerV2: DokumentmalPersonDto[];
  rmISak?: boolean | null;
  vedtakDetaljer?: VedtakDetaljer | null;
  sjablonDetaljer: SjablonDetaljer;
  sakDetaljer: SakDetaljer;
}

export enum Engangsbeloptype {
  DIREKTE_OPPGJOR = "DIREKTE_OPPGJOR",
  DIREKTEOPPGJOR = "DIREKTE_OPPGJØR",
  ETTERGIVELSE = "ETTERGIVELSE",
  ETTERGIVELSE_TILBAKEKREVING = "ETTERGIVELSE_TILBAKEKREVING",
  GEBYR_MOTTAKER = "GEBYR_MOTTAKER",
  GEBYR_SKYLDNER = "GEBYR_SKYLDNER",
  INNKREVING_GJELD = "INNKREVING_GJELD",
  TILBAKEKREVING = "TILBAKEKREVING",
  TILBAKEKREVING_BIDRAG = "TILBAKEKREVING_BIDRAG",
  SAERTILSKUDD = "SAERTILSKUDD",
  SAeRTILSKUDD = "SÆRTILSKUDD",
  SAeRBIDRAG = "SÆRBIDRAG",
}

export interface EnhetKontaktInfo {
  navn: string;
  telefonnummer: string;
  postadresse: Adresse;
  enhetId: string;
}

export interface ForskuddInntektgrensePeriode {
  /** @format date */
  fomDato: string;
  /** @format date */
  tomDato?: string | null;
  forsorgerType: Sivilstandskode;
  /** @format int32 */
  antallBarn: number;
  beløp50Prosent: PairBigDecimalBigDecimal;
  beløp75Prosent: PairBigDecimalBigDecimal;
}

export interface GebyrInfoDto {
  bmGebyr?: number | null;
  bpGebyr?: number | null;
}

export interface Gjelder {
  fodselsnummer: string;
  navn?: string | null;
  adresse?: Adresse | null;
  rolle?: Rolletype | null;
}

export interface Inntekt {
  bmInntekt: number;
  bpInntekt: number;
  barnInntekt: number;
  totalInntekt: number;
}

export interface InntektPeriode {
  /** @uniqueItems true */
  inntektPerioder: TypeArManedsperiode[];
  /** @uniqueItems true */
  inntektOpprinneligPerioder: TypeArManedsperiode[];
  periode: TypeArManedsperiode;
  /** @uniqueItems true */
  typer: Inntektsrapportering[];
  periodeTotalinntekt?: boolean | null;
  nettoKapitalInntekt?: boolean | null;
  /** @format int32 */
  beløpÅr?: number | null;
  fødselsnummer?: string | null;
  beløp: number;
  rolle: Rolletype;
  innteksgrense: number;
  type?: Inntektsrapportering | null;
}

export interface Mottaker {
  fodselsnummer: string;
  navn: string;
  spraak: string;
  adresse?: Adresse | null;
  rolle?: Rolletype | null;
  /** @format date */
  fodselsdato?: string | null;
}

export interface PairBigDecimalBigDecimal {
  first: number;
  second: number;
}

export interface PairIntegerInteger {
  /** @format int32 */
  first: number;
  /** @format int32 */
  second: number;
}

export interface PartInfo {
  type?: Rolletype | null;
  rolle: Rolletype;
  fodselsnummer?: string | null;
  navn: string;
  /** @format date */
  fodselsdato?: string | null;
  /** @format date */
  doedsdato?: string | null;
  landkode?: string | null;
  landkode3?: string | null;
  /** @format date */
  datoDod?: string | null;
  gebyr?: number | null;
  kravFremAv?: string | null;
}

export interface Rolle {
  rolle: Rolletype;
  navn: string;
  fodselsnummer?: string;
  /** @format date */
  fodselsdato?: string;
  type?: Rolletype;
}

export interface SakDetaljer {
  harUkjentPart: boolean;
  levdeAdskilt: boolean;
}

export interface Saksbehandler {
  ident?: string | null;
  navn?: string | null;
  fornavnEtternavn: string;
}

export interface Samvaersperiode {
  periode: TypeArManedsperiode;
  samværsklasse: Samvaersklasse;
  aldersgruppe?: PairIntegerInteger | null;
  samværsfradragBeløp: number;
}

/** Sivilstand for person */
export interface SivilstandPeriode {
  periode: TypeArManedsperiode;
  sivilstand: Sivilstandskode;
  /** Om grunnlaget er manuelt registrert av saksbehandler eller om det er innhentet fra ekstern kilde (skatt/folkregisteret...) */
  manueltRegistrert: boolean;
}

export interface SjablonDetaljer {
  multiplikatorInntekstgrenseForskudd: number;
  fastsettelseGebyr: number;
  forskuddInntektIntervall: number;
  forskuddSats: number;
  inntektsintervallTillegsbidrag: number;
  multiplikatorHøyInntektBp: number;
  multiplikatorMaksBidrag: number;
  multiplikatorInnteksinslagBarn: number;
  multiplikatorMaksInntekBarn: number;
  nedreInntekstgrenseGebyr: number;
  prosentsatsTilleggsbidrag: number;
  maksProsentAvInntektBp: number;
  forskuddInntektgrensePerioder: ForskuddInntektgrensePeriode[];
  maksgrenseHøyInntekt: number;
  maksBidragsgrense: number;
  maksInntektsgrense: number;
  maksForskuddsgrense: number;
  maksInntektsgebyr: number;
}

export interface Skatt {
  sumSkattFaktor: number;
  sumSkatt: number;
  skattAlminneligInntekt: number;
  trinnskatt: number;
  trygdeavgift: number;
  skattAlminneligInntektMånedsbeløp: number;
  trinnskattMånedsbeløp: number;
  trygdeavgiftMånedsbeløp: number;
  skattMånedsbeløp: number;
}

export interface SaerbidragBeregning {
  kravbeløp: number;
  godkjentbeløp: number;
  resultat: number;
  resultatKode: Resultatkode;
  beløpDirekteBetaltAvBp: number;
  andelProsent: number;
  inntekt: Inntekt;
}

export enum TypeBehandling {
  FORSKUDD = "FORSKUDD",
  SAeRBIDRAG = "SÆRBIDRAG",
  BIDRAG = "BIDRAG",
  BIDRAG18AR = "BIDRAG_18_ÅR",
}

export interface UnderholdEgneBarnIHusstand {
  getårsbeløp: number;
  sjablon: number;
  /** @format double */
  antallBarnIHusstanden: number;
  /** @format int32 */
  antallBarnDeltBossted: number;
  måndesbeløp: number;
}

export interface UnderholdskostnaderPeriode {
  periode: TypeArManedsperiode;
  tilsynstype?: "HELTID" | "DELTID" | "IKKE_ANGITT";
  skolealder?: "OVER" | "UNDER" | "IKKE_ANGITT";
  harBarnetilsyn: boolean;
  delberegning: DelberegningUnderholdskostnad;
  gjelderIdent: string;
  rolletype?: Rolletype | null;
}

export interface VedtakBarn {
  fødselsnummer: string;
  navn?: string | null;
  sumAvregning: number;
  løpendeBidrag?: number | null;
  bostatusPerioder: BostatusPeriode[];
  stønadsendringer: VedtakBarnStonad[];
  engangsbeløper: VedtakBarnEngangsbelop[];
  erDirekteAvslag: boolean;
}

export interface VedtakBarnEngangsbelop {
  type: Engangsbeloptype;
  sjablon: BrevSjablonVerdier;
  periode: Datoperiode;
  medInnkreving: boolean;
  erDirekteAvslag: boolean;
  særbidragBeregning?: SaerbidragBeregning | null;
  inntekter: InntektPeriode[];
}

export interface VedtakBarnStonad {
  type: Stonadstype;
  innkreving: boolean;
  direkteAvslag: boolean;
  vedtakPerioder: VedtakPeriode[];
  forskuddInntektgrensePerioder: ForskuddInntektgrensePeriode[];
}

export interface VedtakDetaljer {
  getårsakKode?: TypeArsakstype;
  avslagsKode?: Resultatkode | null;
  type: TypeBehandling;
  gebyr?: GebyrInfoDto | null;
  /** @format date */
  virkningstidspunkt?: string | null;
  /** @format date */
  mottattDato?: string | null;
  /** @format date */
  soktFraDato?: string | null;
  /** @format date */
  vedtattDato?: string | null;
  saksbehandlerInfo: VedtakSaksbehandlerInfo;
  vedtakstype: Vedtakstype;
  stønadstype?: Stonadstype | null;
  engangsbeløptype?: Engangsbeloptype | null;
  søknadFra?: SoktAvType | null;
  kilde: "MANUELT" | "AUTOMATISK";
  vedtakBarn: VedtakBarn[];
  resultat: (
    | DokumentmalResultatBidragsberegningBarnDto
    | NotatResultatForskuddBeregningBarnDto
    | NotatResultatSaerbidragsberegningDto
  )[];
  barnIHusstandPerioder: BarnIHusstandPeriode[];
  sivilstandPerioder: SivilstandPeriode[];
  erDirekteAvslagForAlleBarn: boolean;
}

export interface VedtakPeriode {
  /** @format date */
  fomDato: string;
  /** @format date */
  tomDato?: string | null;
  beløp: number;
  innkreving?: string | null;
  resultatKode: string;
  inntektGrense: number;
  maksInntekt: number;
  inntekter: InntektPeriode[];
  samvær?: Samvaersperiode | null;
  bidragsevne?: BidragsevnePeriode | null;
  underhold?: UnderholdskostnaderPeriode | null;
  andelUnderhold?: AndelUnderholdskostnadPeriode | null;
}

export interface VedtakSaksbehandlerInfo {
  navn: string;
  ident: string;
}

export type JsonNode = any;
