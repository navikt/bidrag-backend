import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";

export const BeregningFordeltBidragEvne = () => {
  const {
    sluttberegning,
    delberegningBidragsevne: evne,
    forholdsmessigFordelingBeregningsdetaljer: forholdsmessigFordeling,
  } = useBeregningDetaljer();
  function renderResult() {
    if (sluttberegning.bidragJustertNedTilEvne) {
      return ` (redusert ned til evne)`;
    } else if (sluttberegning.bidragJustertNedTil25ProsentAvInntekt) {
      return ` (redusert ned til 25% av inntekt)`;
    }
    return "";
  }

  const erForholdsmessigFordelt =
    forholdsmessigFordeling != null &&
    forholdsmessigFordeling.erForholdsmessigFordelt;
  return (
    <DataViewTable
      gap={"5px"}
      data={
        [
          {
            label: "25% av inntekt",
            textRight: false,
            value: formatterBeløpForBeregning(evne.sumInntekt25Prosent),
          },
          !erForholdsmessigFordelt && {
            label: "Foreløpig bidrag",
            textRight: false,
            value: `${formatterBeløpForBeregning(sluttberegning.bruttoBidragJustertForEvneOg25Prosent)}${renderResult()}`,
          },
        ].filter((d) => d != null) as DataViewTableData[]
      }
    />
  );
};
