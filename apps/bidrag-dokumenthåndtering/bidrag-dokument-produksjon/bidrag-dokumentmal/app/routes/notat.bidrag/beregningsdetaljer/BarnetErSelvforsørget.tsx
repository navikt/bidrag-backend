import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/bidrag/opplysninger.yaml";
import { DataViewTableV2 } from "~/components/DataViewTableV2";

const t = bidragOpplysningerTekster.beregningsdetaljer.barnetErSelvforsørget;

export const BarnetErSelvforsørget = () => {
  const { inntekter, forskuddssats } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <DataViewTableV2
        title={t.tittel}
        gap={"5px"}
        data={[
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
    </div>
  );
};
