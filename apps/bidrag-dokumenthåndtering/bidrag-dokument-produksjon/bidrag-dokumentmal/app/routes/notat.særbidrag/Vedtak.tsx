import {
  NotatResultatSaerbidragsberegningDto,
  Resultatkode,
} from "~/types/Api";
import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import { VedtakFattetDetaljer } from "~/components/notat_felles/components/VedtakFattetDetaljer";
import elementIds from "~/utils/elementIds";
import { erResutlatMedBeregning } from "~/routes/notat.særbidrag/SærbidragHelpers";
import { KolonneTabell, Tabelloppsett } from "~/components/KolonneTabell";
import { Feltliste, Feltlisteoppsett } from "~/components/Feltliste";
import { RowContent } from "~/components/CommonTable";
import { formater } from "~/utils/visning";
import ord from "~/tekster/ord.yaml";
import særbidragTekster from "~/tekster/notat/særbidrag/saksbehandler.yaml";

const t = særbidragTekster.vedtak;

const avslagMedUtgifter: Feltlisteoppsett = {
  ledetekstBredde: "90px",
  felter: [
    { felt: "resultatVisningsnavn", tittel: ord.årsak },
    {
      felt: "forskuddssats",
      tittel: t.avslagMedUtgifter.forskuddssats,
      format: "beløp",
    },
    {
      felt: "beregning.totalKravbeløp",
      tittel: ord.kravbeløp,
      format: "beløp",
    },
    {
      felt: "beregning.totalGodkjentBeløp",
      tittel: ord.godkjentBeløp,
      format: "beløp",
    },
  ],
};

const inntekter: Feltlisteoppsett = {
  tittel: t.inntekter.tittel,
  bredde: "35%",
  ledetekstBredde: "140px",
  felter: [
    {
      felt: "inntekter.inntektBM",
      tittel: t.inntekter.inntektBidragsmottaker,
      format: "beløp",
    },
    {
      felt: "inntekter.inntektBP",
      tittel: t.inntekter.inntektBidragspliktig,
      format: "beløp",
    },
    {
      felt: "inntekter.inntektBarn",
      tittel: t.inntekter.inntektBarn,
      format: "beløp",
    },
  ],
};

const boforhold: Feltlisteoppsett = {
  tittel: t.boforhold.tittel,
  bredde: "45%",
  ledetekstBredde: "145px",
  felter: [
    {
      felt: "antallBarnIHusstanden",
      tittel: t.boforhold.antallBarnIHusstanden,
    },
    { felt: "voksenIHusstanden", tittel: t.boforhold.voksneIHusstanden },
  ],
};

function beregning(medInnkreving: boolean): Feltlisteoppsett {
  const tb = t.beregning;
  return {
    tittel: tb.tittel,
    bredde: "100%",
    ledetekstBredde: "130px",
    felter: [
      {
        felt: "beregning.totalKravbeløp",
        tittel: ord.kravbeløp,
        format: "beløp",
      },
      {
        felt: "beregning.totalGodkjentBeløp",
        tittel: ord.godkjentBeløp,
        format: "beløp",
      },
      {
        felt: "maksGodkjentBeløp",
        tittel: tb.maksGodkjentBeløp,
        format: "beløp",
      },
      {
        felt: "bpsAndel.endeligAndelFaktor",
        tittel: tb.bidragspliktigesAndel,
        format: "prosent",
      },
      { felt: "bpHarEvne", tittel: tb.bidragspliktigHarEvne },
      { felt: "resultat", tittel: ord.resultat, format: "beløp" },
      {
        felt: "beregning.totalBeløpBetaltAvBp",
        tittel: tb.betaltAvBidragspliktig,
        format: "beløp",
      },
      {
        felt: "beløpSomInnkreves",
        tittel: medInnkreving
          ? tb.beløpSomInnkreves
          : tb.beløpSomInnkrevesUtenInnkreving,
        format: "beløp",
      },
    ],
  };
}

const utgifter: Tabelloppsett = {
  bredde: "630px",
  kolonner: [
    {
      felt: "betaltAvBp",
      tittel: t.utgifter.betaltAvBidragspliktig,
      bredde: "100px",
    },
    {
      felt: "dato",
      tittel: t.utgifter.forfallsdato,
      bredde: "70px",
      format: "dato",
    },
    {
      felt: "utgiftstypeVisningsnavn",
      tittel: t.utgifter.utgift,
      bredde: "120px",
    },
    {
      felt: "kravbeløp",
      tittel: ord.kravbeløp,
      bredde: "80px",
      format: "beløp",
    },
    {
      felt: "godkjentBeløp",
      tittel: ord.godkjentBeløp,
      bredde: "110px",
      format: "beløp",
    },
  ],
};

export default function Vedtak() {
  const vedleggNummer = useVedleggNummer("beregningsdetaljer");
  const { erAvslag, data } = useNotatFelles();
  const resultat =
    (data.vedtak?.resultat as NotatResultatSaerbidragsberegningDto[]) ?? [];
  return (
    <>
      <div
        style={{
          pageBreakBefore: erAvslag ? "auto" : "always",
          display: "inline-block",
        }}
      >
        <div className={"elements_inline section-title mb-2"}>
          <h2>{t.tittel}</h2>
          {erResutlatMedBeregning(resultat) && (
            <a href={`#${elementIds.vedleggBeregningsdetaljer}`}>
              {ord.seVedleggForBeregningsdetaljer.replace(
                "{}",
                vedleggNummer.toString(),
              )}
            </a>
          )}
        </div>
        {resultat.length == 0 ? (
          <div>{ord.manglerResultat}</div>
        ) : (
          <VedtakResultat resultat={resultat[0]} />
        )}
      </div>
      <VedtakFattetDetaljer data={data.vedtak} />
    </>
  );
}

function VedtakResultat({
  resultat,
}: {
  resultat: NotatResultatSaerbidragsberegningDto;
}) {
  const {
    data: { medInnkreving },
  } = useNotatFelles();

  if (resultat.erDirekteAvslag) {
    return (
      <div>
        <h3 style={{ marginTop: 0 }}>{ord.avslag}</h3>
        <p>
          <dl className="bd_datadisplay">
            <dt>{t.årsak}</dt>
            <dd>{resultat.resultatVisningsnavn}</dd>
          </dl>
        </p>
      </div>
    );
  }

  if (resultat.erAvslagSomInneholderUtgifter) {
    const visForskuddssats =
      resultat.resultatKode ==
      Resultatkode.GODKJENTBELOPERLAVEREENNFORSKUDDSSATS;
    return (
      <div>
        <h3 style={{ marginTop: 0 }}>{ord.avslag}</h3>
        <UtgiftsposterTabell />
        <Feltliste
          oppsett={avslagMedUtgifter}
          kilde={resultat}
          skjul={visForskuddssats ? [] : ["forskuddssats"]}
        />
      </div>
    );
  }

  const avslagEllerBeløp = (_: unknown, beløp: RowContent) =>
    resultat.erBeregningAvslag ? ord.avslag : beløp;
  const voksne = t.boforhold.svarVoksneIHusstanden;
  return (
    <div>
      <h3 style={{ marginTop: 0 }}>
        {resultat.erBeregningAvslag
          ? t.avslagMedÅrsak.replace("{}", resultat.resultatVisningsnavn)
          : t.innvilget}
      </h3>
      <div>
        <Feltliste
          oppsett={inntekter}
          kilde={resultat}
          className={"two_column_view_v2"}
        />
        <Feltliste
          oppsett={boforhold}
          kilde={resultat}
          className={"two_column_view_v2"}
          verdier={{
            voksenIHusstanden: (r) =>
              !r.voksenIHusstanden
                ? ord.nei
                : r.enesteVoksenIHusstandenErEgetBarn
                  ? voksne.jaEgetBarn
                  : ord.ja,
          }}
        />
        <Feltliste
          oppsett={beregning(medInnkreving)}
          kilde={resultat}
          className={"two_column_view_v2 mt-medium"}
          verdier={{
            resultat: avslagEllerBeløp,
            beløpSomInnkreves: avslagEllerBeløp,
          }}
        />
      </div>
      <UtgiftsposterTabell />
    </div>
  );
}

function UtgiftsposterTabell() {
  const { data } = useNotatFelles();
  const beregning = data.utgift?.beregning;
  return (
    <div className={"mt-4"}>
      <h4>{t.utgifter.tittel}</h4>
      <KolonneTabell
        layoutAuto
        oppsett={utgifter}
        rader={data.utgift?.utgifter ?? []}
        ekstraRader={[
          {
            columns: [
              { content: ord.sum, colSpan: 3, labelBold: true },
              {
                content: formater(beregning?.totalKravbeløp, "beløpAvrundet"),
              },
              {
                content: formater(
                  beregning?.totalGodkjentBeløp,
                  "beløpAvrundet",
                ),
              },
            ],
          },
        ]}
      />
    </div>
  );
}
