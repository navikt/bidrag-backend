import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { DataViewTableData } from "~/components/DataViewTable";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import { DataViewTableV2 } from "~/components/DataViewTableV2";

export const AldersjusteringDetaljer = () => {
  const {
    samværsfradrag,
    bpsAndel,
    delberegningUnderholdskostnad,
    deltBosted,
    sluttberegning,
    periode,
  } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <h4>{`Aldersjustering`}</h4>
      <div>
        <div className={"flex flex-row gap-5 mb-4"}>
          <DataViewTableV2
            gap={"5px"}
            title={"Underholdskostnad"}
            data={
              [
                {
                  label: "Forbruksutgift",
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.forbruksutgift, true)}`,
                },
                {
                  label: "Boutgift",
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.boutgift, true)}`,
                },
                {
                  label: "Netto tilsynsutgift",
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.nettoTilsynsutgift, true)}`,
                },
                {
                  label: "Barnetrygd",
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.barnetrygd, true)}`,
                },
                {
                  label: "Barnetilsyn med stønad",
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.barnetilsynMedStønad, true)}`,
                },
              ].filter((d) => d != null) as DataViewTableData[]
            }
          />
          <DataViewTableV2
            gap={"5px"}
            title={"Samværsfradrag"}
            data={
              [
                {
                  label: "Samværsklasse",
                  value: `${samværsfradrag.samværsklasseVisningsnavn}`,
                },
                {
                  label: "Samværsfradrag",
                  value: `${formatterBeløpForBeregning(samværsfradrag?.samværsfradrag, true)}`,
                },
              ].filter((d) => d != null) as DataViewTableData[]
            }
          />
        </div>
        <DataViewTableV2
          gap={"5px"}
          data={
            [
              {
                label: "Underholdskostnad",
                value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad, true)}`,
              },
              {
                label: "Andel underholdskostnad",
                value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad, true)} x ${formatterProsent(periode.bpsAndelU)}`,
                result: formatterBeløpForBeregning(periode.bpsAndelBeløp),
              },
              deltBosted && {
                label: "Etter fratrekk delt bosted",
                value: `${formatterBeløpForBeregning(bpsAndel.andelBeløp, true)} - ${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad, true)} x ${formatterProsent(0.5)}`,
                result: formatterBeløpForBeregning(
                  sluttberegning.bpAndelAvUVedDeltBostedBeløp,
                  true,
                ),
              },
            ].filter((d) => d != null) as DataViewTableData[]
          }
        />
      </div>

      {bpsAndel.endeligAndelFaktor !== bpsAndel.beregnetAndelFaktor && (
        <div className="text-red-500" style={{ width: "200px" }}>
          Andel begrenset til {formatterProsent(bpsAndel.endeligAndelFaktor)}
        </div>
      )}
    </div>
  );
};
