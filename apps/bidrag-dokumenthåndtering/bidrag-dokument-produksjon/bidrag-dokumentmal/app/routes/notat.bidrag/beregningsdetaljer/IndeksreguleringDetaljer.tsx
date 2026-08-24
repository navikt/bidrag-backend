import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { DataViewTableData } from "~/components/DataViewTable";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import { DataViewTableV2 } from "~/components/DataViewTableV2";

export const IndeksreguleringDetaljer = () => {
  const { indeksreguleringDetaljer } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <h4>{`Indeksregulering`}</h4>
      <div>
        <DataViewTableV2
          gap={"5px"}
          data={
            [
              {
                label: "Indeksregulert beløp",
                result: `${formatterBeløpForBeregning(indeksreguleringDetaljer.sluttberegning?.originaltBeløp?.verdi, true)}`,
              },
              {
                label: "Indeksprosent",
                result: `${formatterProsent(indeksreguleringDetaljer.faktor)}`,
              },
              {
                label: "Beregning",
                value: `${formatterBeløpForBeregning(indeksreguleringDetaljer.sluttberegning?.originaltBeløp?.verdi, true)} x ${1 + indeksreguleringDetaljer.faktor / 100}`,
                result: formatterBeløpForBeregning(
                  indeksreguleringDetaljer.sluttberegning?.beløp?.verdi,
                ),
              },
            ].filter((d) => d != null) as DataViewTableData[]
          }
        />
      </div>
    </div>
  );
};
