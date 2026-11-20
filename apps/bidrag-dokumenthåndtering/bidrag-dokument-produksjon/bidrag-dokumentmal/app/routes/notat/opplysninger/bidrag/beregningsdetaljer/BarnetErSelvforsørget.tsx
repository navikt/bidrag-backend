import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import { FeltTabell, JusterteFeltrader } from "~/components/FeltTabell";
import { tekst } from "~/utils/tekst";
export const BarnetErSelvforsørget = () => {
  const { inntekter, forskuddssats } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <div>
        {tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.barnetErSelvforsørget.tittel",
          "Barnet er selvforsørget",
        ) && (
          <h4>
            {tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.barnetErSelvforsørget.tittel",
              "Barnet er selvforsørget",
            )}
          </h4>
        )}
        <FeltTabell>
          <JusterteFeltrader
            rows={[
              {
                label: tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.barnetErSelvforsørget.basInntekt",
                  "BAs inntekt",
                ),
                labelBold: true,
                result: formatterBeløpForBeregning(
                  inntekter!.inntektBarn ?? 0,
                  true,
                ),
              },
              {
                label: tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.barnetErSelvforsørget.grenseForSelvforsørget",
                  "Grense for selvforsørget",
                ),
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
