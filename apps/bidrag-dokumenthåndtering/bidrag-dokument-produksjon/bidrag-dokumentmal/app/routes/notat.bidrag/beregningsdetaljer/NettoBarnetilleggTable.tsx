import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import {
  formatterBeløpForBeregning,
  rolleTilVisningsnavn,
  formatterProsent,
} from "~/utils/visningsnavn";
import {
  DelberegningUnderholdskostnad,
  DokumentmalDelberegningBarnetilleggDto,
  Rolletype,
  DokumentmalSluttberegningBarnebidragDetaljer,
} from "~/types/Api";
import {
  CommonTable,
  TableHeader,
  TableColumn,
} from "~/components/CommonTable";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import { barnetilleggTiltakspengerVisningsnavn } from "~/constants/beregning";

export const NettoBarnetilleggTable = ({ rolle }: { rolle: Rolletype }) => {
  const {
    sluttberegning,
    delberegningUnderholdskostnad: underholdskostnad,
    barnetilleggBP,
    barnetilleggBM,
  } = useBeregningDetaljer();

  const barnetillegg = rolle == Rolletype.BP ? barnetilleggBP : barnetilleggBM;
  const harBareTiltakspenger = barnetillegg.barnetillegg.every(
    (bt) => bt.visningsnavn === barnetilleggTiltakspengerVisningsnavn,
  );
  return (
    <div style={{ width: "600px" }}>
      <h4>{`Netto barnetillegg (${rolleTilVisningsnavn(rolle)})`}</h4>

      <CommonTable
        layoutAuto
        data={{
          headers: [
            {
              name: "Type barnetillegg",
              width: "100px",
            },
            !harBareTiltakspenger && {
              name: "Brutto",
              width: "50px",
            },
            {
              name: "Skattesats",
              width: "50px",
            },
            {
              name: "Netto",
              width: "50px",
            },
          ].filter((h) => h != null) as TableHeader[],
          rows: barnetillegg.barnetillegg
            .map((bt) => ({
              columns: [
                {
                  content: bt.visningsnavn,
                  colSpan: 1,
                },
                !harBareTiltakspenger && {
                  content:
                    bt.visningsnavn === barnetilleggTiltakspengerVisningsnavn
                      ? "Ikke relevant"
                      : formatterBeløpForBeregning(bt.bruttoBeløp, true),
                },
                {
                  content: formatterProsent(bt.skattefaktor),
                },
                {
                  content: formatterBeløpForBeregning(bt.nettoBeløp, true),
                },
              ].filter((d) => d != null) as TableColumn[],
            }))
            .concat([
              {
                columns: [
                  {
                    content: "Resultat" as string,
                  },
                  !harBareTiltakspenger && {
                    content: formatterBeløpForBeregning(
                      barnetillegg!.sumBruttoBeløp,
                    ),
                  },
                  {
                    content: "",
                  },
                  {
                    content: formatterBeløpForBeregning(
                      barnetillegg!.sumNettoBeløp,
                    ),
                  },

                ] as TableColumn[],
              },
            ]),
        }}
      />
      {rolle == Rolletype.BP ? (
        <BeregningJusterBPsBarnetillegg sluttberegning={sluttberegning!} />
      ) : (
        <BeregningJusterBMsBarnetillegg
          sluttberegning={sluttberegning!}
          underholdskostnad={underholdskostnad!}
          barnetilleggBM={barnetillegg}
        />
      )}
    </div>
  );
};
export const BeregningJusterBPsBarnetillegg = ({
  sluttberegning,
}: {
  sluttberegning: DokumentmalSluttberegningBarnebidragDetaljer;
}) => {
  function renderResult() {
    if (sluttberegning.bidragJustertForNettoBarnetilleggBP) {
      return ` (justert opp til BPs netto barnetillegg)`;
    }
    return "";
  }
  function hentForeløpigBidrag() {
    if (sluttberegning.bidragJustertForNettoBarnetilleggBP)
      return formatterBeløpForBeregning(
        sluttberegning.bruttoBidragEtterBarnetilleggBP,
      );
    return formatterBeløpForBeregning(
      sluttberegning.bruttoBidragJustertForEvneOg25Prosent,
    );
  }
  return (
    <DataViewTable
      gap={"5px"}
      data={
        [
          {
            label: "Foreløpig bidrag",
            value: `${hentForeløpigBidrag()}${renderResult()}`,
          },
        ].filter((d) => d != null) as DataViewTableData[]
      }
    />
  );
};
export const BeregningJusterBMsBarnetillegg = ({
  barnetilleggBM,
  sluttberegning,
  underholdskostnad,
}: {
  underholdskostnad: DelberegningUnderholdskostnad;
  sluttberegning: DokumentmalSluttberegningBarnebidragDetaljer;
  barnetilleggBM: DokumentmalDelberegningBarnetilleggDto;
}) => {
  function renderResult() {
    if (sluttberegning.bidragJustertForNettoBarnetilleggBM) {
      return ` (justert til U - BMs netto barnetillegg + samværsfradrag)`;
    }
    return "";
  }

  return (
    <DataViewTable
      gap={"5px"}
      data={
        [
          {
            label: "U - BMs netto barnetillegg",
            value: `${formatterBeløpForBeregning(underholdskostnad.underholdskostnad)} - ${formatterBeløpForBeregning(barnetilleggBM.sumNettoBeløp)} = ${formatterBeløpForBeregning(sluttberegning.uminusNettoBarnetilleggBM)}`,
          },
          {
            label: "Foreløpig bidrag",
            textRight: false,
            value: `${formatterBeløpForBeregning(sluttberegning.bruttoBidragEtterBarnetilleggBM)}${renderResult()}`,
          },
        ].filter((d) => d != null) as DataViewTableData[]
      }
    />
  );
};
