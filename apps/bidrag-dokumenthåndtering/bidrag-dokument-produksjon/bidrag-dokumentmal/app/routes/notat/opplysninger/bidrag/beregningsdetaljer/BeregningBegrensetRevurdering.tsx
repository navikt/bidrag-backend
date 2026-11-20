import { FeltTabell, Feltrader } from "~/components/FeltTabell";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import { tekst } from "~/utils/tekst";
export const BeregningBegrensetRevurdering = () => {
  const { sluttberegning, samværsfradrag } = useBeregningDetaljer();
  if (!sluttberegning.begrensetRevurderingUtført) return null;
  return (
    <div className={"mb-2"}>
      <h4>
        {tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.begrensetRevurdering.tittel",
          "Begrenset revurdering",
        )}
      </h4>
      <div>
        <FeltTabell>
          <Feltrader
            rows={[
              {
                label: tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.begrensetRevurdering.løpendeForskudd",
                  "Løpende forskudd",
                ),
                value: `${formatterBeløpForBeregning(sluttberegning.løpendeForskudd ?? 0)}`,
              },
              {
                label: tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.begrensetRevurdering.løpendeBidrag",
                  "Løpende bidrag",
                ),
                value: `${formatterBeløpForBeregning(sluttberegning.løpendeBidrag ?? 0)}`,
              },
              {
                label: tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.begrensetRevurdering.foreløpigBidrag",
                  "Foreløpig bidrag",
                ),
                value: `${formatterBeløpForBeregning(sluttberegning.løpendeForskudd ?? 0)} + ${formatterBeløpForBeregning(samværsfradrag.samværsfradrag)}`,
                result: `${formatterBeløpForBeregning(sluttberegning.bruttoBidragEtterBegrensetRevurdering)}`,
              },
            ]}
          />
        </FeltTabell>
      </div>
    </div>
  );
};
