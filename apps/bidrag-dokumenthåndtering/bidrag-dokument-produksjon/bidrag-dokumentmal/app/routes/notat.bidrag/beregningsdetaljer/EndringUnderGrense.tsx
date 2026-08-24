import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";

export const EndringUnderGrense = () => {
  const {
    endringUnderGrense,
    sluttberegning,
    erEndringUnderGrense,
    endeligBeløp,
  } = useBeregningDetaljer();

  if (!erEndringUnderGrense) return null;

  return (
    <div className={"pt-2"}>
      <h4>{`Endring under grense`}</h4>
      <DataViewTable
        gap={"5px"}
        data={
          [
            {
              label: "Løpende bidragsbeløp",
              textRight: false,
              value: `${formatterBeløpForBeregning(endeligBeløp)}`,
            },
            {
              label: "Endring i prosent",
              textRight: false,
              value: `(${formatterBeløpForBeregning(endeligBeløp)} - ${formatterBeløpForBeregning(sluttberegning.beregnetBeløp)}) / ${formatterBeløpForBeregning(endeligBeløp)} = ${formatterProsent(endringUnderGrense.faktiskEndringFaktor)}`,
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />
    </div>
  );
};
