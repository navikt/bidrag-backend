import { FeltTabell, Feltrader } from "~/components/FeltTabell";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";

const t = bidragOpplysningerTekster.beregningsdetaljer.begrensetRevurdering;

export const BeregningBegrensetRevurdering = () => {
  const { sluttberegning, samværsfradrag } = useBeregningDetaljer();
  if (!sluttberegning.begrensetRevurderingUtført) return null;

  return (
    <div className={"mb-2"}>
      <h4>{t.tittel}</h4>
      <div>
        <FeltTabell>
          <Feltrader
            rows={[
              {
                label: t.løpendeForskudd,
                value: `${formatterBeløpForBeregning(sluttberegning.løpendeForskudd ?? 0)}`,
              },
              {
                label: t.løpendeBidrag,
                value: `${formatterBeløpForBeregning(sluttberegning.løpendeBidrag ?? 0)}`,
              },
              {
                label: t.foreløpigBidrag,
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
