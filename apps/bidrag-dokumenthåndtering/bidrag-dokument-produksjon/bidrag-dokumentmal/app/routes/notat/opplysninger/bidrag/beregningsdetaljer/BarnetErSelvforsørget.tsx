import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";
import { FeltTabell, JusterteFeltrader } from "~/components/FeltTabell";

const t = bidragOpplysningerTekster.beregningsdetaljer.barnetErSelvforsørget;

export const BarnetErSelvforsørget = () => {
  const { inntekter, forskuddssats } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <div>
        {t.tittel && <h4>{t.tittel}</h4>}
        <FeltTabell>
          <JusterteFeltrader
            rows={[
              {
                label: t.basInntekt,
                labelBold: true,
                result: formatterBeløpForBeregning(
                  inntekter!.inntektBarn ?? 0,
                  true,
                ),
              },
              {
                label: t.grenseForSelvforsørget,
                labelBold: true,
                value: `100 x ${formatterBeløpForBeregning(forskuddssats)}`,
                result: formatterBeløpForBeregning(forskuddssats * 100, true),
              },
            ]}
          />
        </FeltTabell>
      </div>
    </div>
  );
};
