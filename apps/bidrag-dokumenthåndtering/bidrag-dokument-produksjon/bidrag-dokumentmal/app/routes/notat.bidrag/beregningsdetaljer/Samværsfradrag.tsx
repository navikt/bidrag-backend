import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";

export const Samværsfradrag = () => {
  const { samværsfradrag } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <h4>{`Samværsfradrag`}</h4>
      <DataViewTable
        gap={"5px"}
        data={
          [
            {
              label: "Samværsklasse",
              value: `Samværsklasse ${samværsfradrag.samværsklasseVisningsnavn} (samvær per måned: ${samværsfradrag.gjennomsnittligSamværPerMåned})`,
            },
            {
              label: "Samværsfradrag",
              value: formatterBeløpForBeregning(samværsfradrag.samværsfradrag),
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />
    </div>
  );
};
