import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { FeltTabell, Feltrader } from "~/components/FeltTabell";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";

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
    <div>
      <FeltTabell>
        <Feltrader
          rows={[
            {
              label: t.tjuefemProsentAvInntekt,
              value: formatterBeløpForBeregning(evne.sumInntekt25Prosent),
            },
            !erForholdsmessigFordelt
              ? {
                  label: t.foreløpigBidrag,
                  value: `${formatterBeløpForBeregning(sluttberegning.bruttoBidragJustertForEvneOg25Prosent)}${renderResult()}`,
                }
              : {},
          ]}
        />
      </FeltTabell>
    </div>
  );
};
