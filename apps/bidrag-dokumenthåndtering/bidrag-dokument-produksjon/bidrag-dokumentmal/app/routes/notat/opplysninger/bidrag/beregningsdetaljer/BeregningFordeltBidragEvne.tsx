import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { FeltTabell, Feltrader } from "~/components/FeltTabell";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import { tekst } from "~/utils/tekst";
export const BeregningFordeltBidragEvne = () => {
  const {
    sluttberegning,
    delberegningBidragsevne: evne,
    forholdsmessigFordelingBeregningsdetaljer: forholdsmessigFordeling,
  } = useBeregningDetaljer();
  function renderResult() {
    if (sluttberegning.bidragJustertNedTilEvne) {
      return tekst(
        "notat.opplysninger.bidrag.beregningsdetaljer.fordeltBidragEvne.redusertNedTilEvne",
        " (redusert ned til evne)",
      );
    } else if (sluttberegning.bidragJustertNedTil25ProsentAvInntekt) {
      return tekst(
        "notat.opplysninger.bidrag.beregningsdetaljer.fordeltBidragEvne.redusertNedTil25ProsentAvInntekt",
        " (redusert ned til 25% av inntekt)",
      );
    }
    return "";
  }
  const erForholdsmessigFordelt =
    forholdsmessigFordeling != null &&
    forholdsmessigFordeling.erForholdsmessigFordelt;
  return (
    <div>
      <FeltTabell>
        <Feltrader
          rows={[
            {
              label: tekst(
                "notat.opplysninger.bidrag.beregningsdetaljer.fordeltBidragEvne.tjuefemProsentAvInntekt",
                "25% av inntekt",
              ),
              value: formatterBeløpForBeregning(evne.sumInntekt25Prosent),
            },
            !erForholdsmessigFordelt
              ? {
                  label: tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.fordeltBidragEvne.foreløpigBidrag",
                    "Foreløpig bidrag",
                  ),
                  value: `${formatterBeløpForBeregning(sluttberegning.bruttoBidragJustertForEvneOg25Prosent)}${renderResult()}`,
                }
              : {},
          ]}
        />
      </FeltTabell>
    </div>
  );
};
