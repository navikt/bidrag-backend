import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/bidrag/opplysninger.json";

const t = bidragOpplysningerTekster.beregningsdetaljer.fordeltBidragEvne;

export const BeregningFordeltBidragEvne = () => {
  const {
    sluttberegning,
    delberegningBidragsevne: evne,
    forholdsmessigFordelingBeregningsdetaljer: forholdsmessigFordeling,
  } = useBeregningDetaljer();
  function renderResult() {
    if (sluttberegning.bidragJustertNedTilEvne) {
      return t.redusertNedTilEvne;
    } else if (sluttberegning.bidragJustertNedTil25ProsentAvInntekt) {
      return t.redusertNedTil25ProsentAvInntekt;
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
            label: t.tjuefemProsentAvInntekt,
            textRight: false,
            value: formatterBeløpForBeregning(evne.sumInntekt25Prosent),
          },
          !erForholdsmessigFordelt && {
            label: t.foreløpigBidrag,
            textRight: false,
            value: `${formatterBeløpForBeregning(sluttberegning.bruttoBidragJustertForEvneOg25Prosent)}${renderResult()}`,
          },
        ].filter((d) => d != null) as DataViewTableData[]
      }
    />
  );
};
