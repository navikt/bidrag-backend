import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { MathValue, MathMultiplication } from "~/components/CalculationTable";
import { NotatResultatBeregningInntekterDto } from "~/types/Api";
import { tekst } from "~/utils/tekst";
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
        { tittel: tekst("ord.beskrivelse", "Beskrivelse"), bredde: "250px" },
        {
          tittel: tekst("felles.tabell.felles.beregning", "Beregning"),
          bredde: "250px",
        },
        { tittel: tekst("ord.beløp", "Beløp"), bredde: "50px" },
      ]}
    >
      <Tabellrad index={0}>
        <Tabellcelle>{`${tekst("felles.begreper.bidragspliktiges", "Bidragspliktiges")} inntekt`}</Tabellcelle>
        <Tabellcelle />
        <Tabellcelle>
          {formatterBeløpForBeregning(inntekter.inntektBP, true)}
        </Tabellcelle>
      </Tabellrad>
      <Tabellrad index={1}>
        <Tabellcelle>{`${tekst("felles.begreper.bidragsmottakerens", "Bidragsmottakerens")} inntekt`}</Tabellcelle>
        <Tabellcelle />
        <Tabellcelle>
          {formatterBeløpForBeregning(inntekter.inntektBM, true)}
        </Tabellcelle>
      </Tabellrad>
      <Tabellrad index={2}>
        <Tabellcelle>{`${tekst("felles.begreper.barnets", "Barnets")} inntekt`}</Tabellcelle>
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
