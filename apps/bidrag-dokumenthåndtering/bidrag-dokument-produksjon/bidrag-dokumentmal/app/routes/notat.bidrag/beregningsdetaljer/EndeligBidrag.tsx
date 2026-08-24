import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";

export const EndeligBidragTable = () => {
  const {
    deltBosted,
    sluttberegning,
    sluttberegningAldersjustering,
    samværsfradrag: beregning,
  } = useBeregningDetaljer();

  return (
    <div style={{ width: "600px" }}>
      <h4>{`Endelig bidrag`}</h4>
      <DataViewTable
        gap={"5px"}
        data={
          [
            !deltBosted
              ? {
                  label: "Etter samværsfradraget",
                  textRight: false,
                  value: `${formatterBeløpForBeregning(sluttberegningAldersjustering?.bpAndelBeløp ?? sluttberegning?.bruttoBidragEtterBarnetilleggBP)} - ${formatterBeløpForBeregning(beregning.samværsfradrag)} = ${formatterBeløpForBeregning(sluttberegningAldersjustering?.beregnetBeløp ?? sluttberegning?.beregnetBeløp)}`,
                }
              : null,
            {
              label: "Avrundet beløp",
              textRight: false,
              value: `${formatterBeløpForBeregning(sluttberegningAldersjustering?.resultatBeløp ?? sluttberegning?.resultatBeløp)}`,
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />
    </div>
  );
};
