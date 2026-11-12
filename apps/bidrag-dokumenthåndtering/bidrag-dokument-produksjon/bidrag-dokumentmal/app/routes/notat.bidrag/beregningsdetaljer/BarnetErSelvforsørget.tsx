import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import { DataViewTableV2 } from "~/components/DataViewTableV2";

export const BarnetErSelvforsørget = () => {
  const { inntekter, forskuddssats } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <DataViewTableV2
        title={"Barnet er selvforsørget"}
        gap={"5px"}
        data={[
          {
            label: "BAs inntekt",
            labelBold: true,
            result: formatterBeløpForBeregning(
              inntekter!.inntektBarn ?? 0,
              true,
            ),
          },
          {
            label: "Grense for selvforsørget",
            labelBold: true,
            value: `100 x ${formatterBeløpForBeregning(forskuddssats)}`,
            result: formatterBeløpForBeregning(forskuddssats * 100, true),
          },
        ]}
      />
    </div>
  );
};
