import {
  NotatResultatSaerbidragsberegningDto,
  Resultatkode,
} from "~/types/Api";
import {
  formatterBeløp,
  formatterProsent,
  formatterBeløpForBeregning,
} from "~/utils/visningsnavn";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { DataViewTableData } from "~/components/DataViewTable";
import { VedtakFattetDetaljer } from "~/components/notat_felles/components/VedtakFattetDetaljer";
import tekster from "~/tekster";
import { dateToDDMMYYYY } from "~/utils/date-utils";
import elementIds from "~/utils/elementIds";
import { erResutlatMedBeregning } from "~/routes/notat.særbidrag/SærbidragHelpers";
import {
  CommonTable,
  TableHeader,
  TableColumn,
  TableRow,
} from "~/components/CommonTable";
import { DataViewTableV2 } from "~/components/DataViewTableV2";
import { VedleggProps } from "~/types/commonTypes";

export default function Vedtak({ vedleggNummer = 3 }: VedleggProps) {
  const { erAvslag, data } = useNotatFelles();
  return (
    <>
      <div
        style={{
          pageBreakBefore: erAvslag ? "auto" : "always",
          display: "inline-block",
        }}
      >
        <div className={"elements_inline section-title mb-2"}>
          <h2>Vedtak</h2>
          {erResutlatMedBeregning(
            (data.vedtak?.resultat as NotatResultatSaerbidragsberegningDto[]) ??
              [],
          ) && (
            <a href={`#${elementIds.vedleggBeregningsdetaljer}`}>
              se vedlegg nr. {vedleggNummer} for beregningsdetaljer
            </a>
          )}
        </div>
        <VedtakTable
          data={data.vedtak.resultat as NotatResultatSaerbidragsberegningDto[]}
        />
      </div>
      <VedtakFattetDetaljer data={data.vedtak} />
    </>
  );
}

function VedtakTable({
  data,
}: {
  data: NotatResultatSaerbidragsberegningDto[];
}) {
  const {
    data: { medInnkreving },
  } = useNotatFelles();
  if (data.length == 0) return <div>Mangler resultat</div>;
  const resultat = data[0]!;
  const erDirekteAvslag = resultat?.erDirekteAvslag;
  const erAvslagSomInneholderUtgifter = [
    Resultatkode.GODKJENTBELOPERLAVEREENNFORSKUDDSSATS,
    Resultatkode.ALLE_UTGIFTER_ER_FORELDET,
  ].includes(resultat.resultatKode);
  const erBeregningeAvslag =
    resultat?.resultatKode !== Resultatkode.SAeRBIDRAGINNVILGET;
  if (erDirekteAvslag) {
    return (
      <div>
        <h3 style={{ marginTop: 0 }}>Avslag</h3>
        <p>
          <dl className="bd_datadisplay">
            <dt>Årsak: </dt>
            <dd>{resultat.resultatVisningsnavn}</dd>
          </dl>
        </p>
      </div>
    );
  }
  if (erAvslagSomInneholderUtgifter) {
    return (
      <div>
        <h3 style={{ marginTop: 0 }}>Avslag</h3>
        <UtgifsposterTabell />
        <DataViewTableV2
          labelColWidth={"90px"}
          key={"inntekter"}
          data={
            [
              {
                label: "Årsak",
                value: resultat.resultatVisningsnavn,
              },
              resultat.resultatKode ==
              Resultatkode.GODKJENTBELOPERLAVEREENNFORSKUDDSSATS
                ? {
                    label: "Forskuddssaats",
                    value: formatterBeløp(resultat.forskuddssats, true),
                  }
                : null,
              {
                label: "Kravbeløp",
                value: formatterBeløp(resultat.beregning?.totalKravbeløp, true),
              },
              {
                label: "Godkjent beløp",
                value: formatterBeløp(
                  resultat.beregning?.totalGodkjentBeløp,
                  true,
                ),
              },
            ].filter((d) => d != null) as DataViewTableData[]
          }
        />
      </div>
    );
  }
  return (
    <div>
      {erBeregningeAvslag ? (
        <h3 style={{ marginTop: 0 }}>
          Avslag: {resultat.resultatVisningsnavn}
        </h3>
      ) : (
        <h3 style={{ marginTop: 0 }}>Særbidrag innvilget</h3>
      )}
      <div>
        <DataViewTableV2
          title="Inntekter"
          className={"two_column_view_v2"}
          labelColWidth={"140px"}
          width={"35%"}
          key={"inntekter"}
          data={[
            {
              label: `Inntekt ${tekster.begreper.bidragsmottaker}`,
              value: formatterBeløp(resultat.inntekter!.inntektBM, true),
            },
            {
              label: `Inntekt ${tekster.begreper.bidragspliktig}`,
              value: formatterBeløp(resultat.inntekter!.inntektBP, true),
            },
            {
              label: `Inntekt ${tekster.begreper.barn}`,
              value: formatterBeløp(resultat.inntekter!.inntektBarn, true),
            },
          ]}
        />

        <DataViewTableV2
          title="Boforhold"
          className={"two_column_view_v2"}
          width={"45%"}
          labelColWidth={"145px"}
          key={"Boforhold"}
          data={[
            {
              label: "Antall barn i husstanden",
              value: resultat.antallBarnIHusstanden,
            },
            {
              label: "Voksne i husstanden",
              value: resultat.voksenIHusstanden
                ? resultat.enesteVoksenIHusstandenErEgetBarn
                  ? "Ja (barn over 18 år)"
                  : "Ja"
                : "Nei",
            },
          ]}
        />
        <DataViewTableV2
          title="Beregning"
          className={"two_column_view_v2 mt-medium"}
          labelColWidth={"130px"}
          width={"100%"}
          key={"Beregning"}
          data={[
            {
              label: "Kravbeløp",
              value: formatterBeløp(resultat.beregning?.totalKravbeløp, true),
            },
            {
              label: "Godkjent beløp",
              value: formatterBeløp(
                resultat.beregning?.totalGodkjentBeløp,
                true,
              ),
            },
            {
              label: "Maks godkjent beløp",
              value: formatterBeløp(resultat.maksGodkjentBeløp, true),
            },
            {
              label: `${tekster.begreper.bidragspliktiges} andel`,
              value: formatterProsent(resultat.bpsAndel?.endeligAndelFaktor),
            },
            {
              label: `${tekster.begreper.bidragspliktig} har evne`,
              value: !resultat.bpHarEvne ? "Nei" : "Ja",
            },
            {
              label: "Resultat",
              value: erBeregningeAvslag
                ? "Avslag"
                : formatterBeløp(resultat.resultat, true),
            },
            {
              label: `Betalt av ${tekster.begreper.bidragspliktig}`,
              value: formatterBeløp(
                resultat.beregning?.totalBeløpBetaltAvBp,
                true,
              ),
            },
            {
              label: medInnkreving
                ? "Beløp som innkreves"
                : "Fastsatt beløp å betale",
              value: erBeregningeAvslag
                ? "Avslag"
                : formatterBeløp(resultat.beløpSomInnkreves, true),
            },
          ]}
        />
      </div>
      <UtgifsposterTabell />
    </div>
  );
}
function UtgifsposterTabell() {
  const { data } = useNotatFelles();
  const utgifstposter = data.utgift?.utgifter ?? [];
  const beregnetSærbidrag = data.utgift?.beregning!;
  return (
    <div className={"mt-4"}>
      <h4>{"Utgiftene lagt til grunn"}</h4>
      <CommonTable
        layoutAuto
        width={"630px"}
        data={{
          headers: [
            { name: tekster.tabell.utgifter.betaltAvBp, width: "100px" },
            { name: tekster.tabell.utgifter.dato, width: "70px" },
            { name: tekster.tabell.utgifter.utgift, width: "120px" },
            { name: tekster.tabell.utgifter.kravbeløp, width: "80px" },
            { name: tekster.tabell.utgifter.godkjentBeløp, width: "110px" },
          ].filter((d) => typeof d != "boolean") as TableHeader[],
          rows: utgifstposter
            .map((d) => {
              return {
                columns: [
                  { content: d.betaltAvBp ? "Ja" : "Nei" },
                  { content: dateToDDMMYYYY(d.dato) },
                  { content: d.utgiftstypeVisningsnavn },
                  { content: formatterBeløp(d.kravbeløp) },
                  { content: formatterBeløp(d.godkjentBeløp) },
                ].filter((d) => typeof d != "boolean") as TableColumn[],
              };
            })
            .concat([
              {
                columns: [
                  { content: "Sum", colSpan: 3, labelBold: true },
                  {
                    content: formatterBeløpForBeregning(
                      beregnetSærbidrag.totalKravbeløp,
                      true,
                    ),
                  },
                  {
                    content: formatterBeløpForBeregning(
                      beregnetSærbidrag.totalGodkjentBeløp,
                      true,
                    ),
                  },
                ] as TableColumn[],
              } as TableRow,
            ]),
        }}
      />
    </div>
  );
}
