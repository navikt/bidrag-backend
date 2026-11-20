import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import ord from "~/tekster/ord.yaml";
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
    <DokumentTabell
      layoutAuto
      kolonner={[
        { tittel: ord.beskrivelse, bredde: "250px" },
        {
          tittel: tekster.tabell.felles.beregning,
          bredde: "250px",
        },
        { tittel: ord.beløp, bredde: "50px" },
      ]}
    >
      <Tabellrad index={0}>
        <Tabellcelle>{`${tekster.begreper.bidragspliktiges} inntekt`}</Tabellcelle>
        <Tabellcelle />
        <Tabellcelle>
          {formatterBeløpForBeregning(inntekter.inntektBP, true)}
        </Tabellcelle>
      </Tabellrad>
      <Tabellrad index={1}>
        <Tabellcelle>{`${tekster.begreper.bidragsmottakerens} inntekt`}</Tabellcelle>
        <Tabellcelle />
        <Tabellcelle>
          {formatterBeløpForBeregning(inntekter.inntektBM, true)}
        </Tabellcelle>
      </Tabellrad>
      <Tabellrad index={2}>
        <Tabellcelle>{`${tekster.begreper.barnets} inntekt`}</Tabellcelle>
        <Tabellcelle>
          {inntekter.inntektBarn! > 0 ? (
            <div>
              <MathValue value={inntekter.inntektBarn!} /> -{" "}
              <MathMultiplication left="30" right={forskuddssats!} />
            </div>
          ) : undefined}
        </Tabellcelle>
        <Tabellcelle>
          {formatterBeløpForBeregning(inntekter.inntektBarn, true)}
        </Tabellcelle>
      </Tabellrad>
      <Tabellrad index={3} borderBottom>
        <Tabellcelle colSpan={2} labelBold>
          Totalt
        </Tabellcelle>
        <Tabellcelle>
          {formatterBeløpForBeregning(inntekter.totalEndeligInntekt)}
        </Tabellcelle>
      </Tabellrad>
    </DokumentTabell>
  );
}
