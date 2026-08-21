import { CommonTable } from "~/components/CommonTable";
import tekster from "~/tekster";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import {
  MathValue,
  MathMultiplication,
} from "~/components/vedtak/CalculationTable";
import { NotatResultatBeregningInntekterDto } from "~/types/Api";

export default function BPsAndelInntekterTable({
  inntekter,
  forskuddssats,
}: {
  forskuddssats?: number;
  inntekter: NotatResultatBeregningInntekterDto;
}) {
  return (
    <CommonTable
      layoutAuto
      data={{
        headers: [
          {
            name: tekster.tabell.felles.beskrivelse,
            width: "250px",
          },
          {
            name: tekster.tabell.felles.beregning,
            width: "250px",
          },
          {
            name: tekster.tabell.felles.beløp,
            width: "50px",
          },
        ],
        rows: [
          {
            columns: [
              {
                content: `${tekster.begreper.bidragspliktiges} inntekt`,
              },

              {
                content: "",
              },
              {
                content: formatterBeløpForBeregning(inntekter.inntektBP, true),
              },
            ],
          },
          {
            columns: [
              {
                content: `${tekster.begreper.bidragsmottakerens} inntekt`,
              },
              { content: "" },
              {
                content: formatterBeløpForBeregning(inntekter.inntektBM, true),
              },
            ],
          },
          {
            columns: [
              {
                content: `${tekster.begreper.barnets} inntekt`,
              },
              {
                content:
                  inntekter.inntektBarn! > 0 ? (
                    <div>
                      <MathValue value={inntekter.inntektBarn!} /> -{" "}
                      <MathMultiplication left="30" right={forskuddssats!} />
                    </div>
                  ) : undefined,
              },
              {
                content: formatterBeløpForBeregning(
                  inntekter.inntektBarn,
                  true,
                ),
              },
            ],
          },
          {
            borderBottom: true,
            columns: [
              {
                content: `Totalt`,
                colSpan: 2,
                labelBold: true,
              },
              {
                content: formatterBeløpForBeregning(
                  inntekter.totalEndeligInntekt,
                ),
              },
            ],
          },
        ],
      }}
    />
  );
}
