import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import ord from "~/tekster/ord.yaml";
import tekster from "~/tekster";
import {
  DokumentmalDelberegningBidragsevneDto,
  DokumentmalResultatBeregningInntekterDto,
} from "~/types/Api";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import {
  MathDivision,
  MathValue,
  MathMultiplication,
} from "~/components/vedtak/CalculationTable";

export const BPsEvneTable = ({
  delberegningBidragsevne,
  inntekter,
}: {
  delberegningBidragsevne: DokumentmalDelberegningBidragsevneDto;
  inntekter: DokumentmalResultatBeregningInntekterDto;
}) => {
  return (
    <>
      <h4>{`Bidragspliktiges evne`}</h4>
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
          <Tabellcelle>Inntekt</Tabellcelle>
          <Tabellcelle>
            <MathDivision
              top={`${formatterBeløpForBeregning(inntekter.inntektBP, true)}/år`}
              bottom={12}
            />
          </Tabellcelle>
          <Tabellcelle>
            <MathValue
              value={formatterBeløpForBeregning(
                inntekter.inntektBPMånedlig,
                true,
              )}
            />
          </Tabellcelle>
        </Tabellrad>
        <Tabellrad index={1}>
          <Tabellcelle>Skatt</Tabellcelle>
          <Tabellcelle>
            <MathDivision
              negativeValue
              top={`${formatterBeløpForBeregning(delberegningBidragsevne.skatt.skattAlminneligInntekt, true)}/år`}
              bottom={12}
            />
          </Tabellcelle>
          <Tabellcelle>
            <MathValue
              negativeValue
              value={formatterBeløpForBeregning(
                delberegningBidragsevne.skatt.skattAlminneligInntektMånedsbeløp,
                true,
              )}
            />
          </Tabellcelle>
        </Tabellrad>
        {[
          {
            navn: "Trygdeavgift",
            årsbeløp: delberegningBidragsevne.skatt.trygdeavgift,
            månedsbeløp: delberegningBidragsevne.skatt.trygdeavgiftMånedsbeløp,
          },
          {
            navn: "Trinnskatt",
            årsbeløp: delberegningBidragsevne.skatt.trinnskatt,
            månedsbeløp: delberegningBidragsevne.skatt.trinnskattMånedsbeløp,
          },
        ].map((rad, index) => (
          <Tabellrad key={rad.navn} index={index + 2}>
            <Tabellcelle>{rad.navn}</Tabellcelle>
            <Tabellcelle>
              <MathDivision
                negativeValue
                top={`${formatterBeløpForBeregning(rad.årsbeløp, true)}/år`}
                bottom={12}
              />
            </Tabellcelle>
            <Tabellcelle>
              <MathValue
                negativeValue
                value={formatterBeløpForBeregning(rad.månedsbeløp, true)}
              />
            </Tabellcelle>
          </Tabellrad>
        ))}
        <Tabellrad index={4}>
          <Tabellcelle>Underhold egne barn i husstand</Tabellcelle>
          <Tabellcelle>
            <MathMultiplication
              negativeValue
              left={`${formatterBeløpForBeregning(delberegningBidragsevne.underholdEgneBarnIHusstand.sjablon, true)}`}
              right={
                delberegningBidragsevne.underholdEgneBarnIHusstand
                  .antallBarnIHusstanden
              }
            />
          </Tabellcelle>
          <Tabellcelle>
            <MathValue
              negativeValue
              value={formatterBeløpForBeregning(
                delberegningBidragsevne.underholdEgneBarnIHusstand.måndesbeløp,
                true,
              )}
            />
          </Tabellcelle>
        </Tabellrad>
        <Tabellrad index={5}>
          <Tabellcelle>Bougift</Tabellcelle>
          <Tabellcelle>
            {delberegningBidragsevne.utgifter.borMedAndreVoksne
              ? "Bor med andre voksne"
              : "Bor ikke med andre voksne"}
          </Tabellcelle>
          <Tabellcelle>
            <MathValue
              negativeValue
              value={formatterBeløpForBeregning(
                delberegningBidragsevne.utgifter.boutgiftBeløp,
                true,
              )}
            />
          </Tabellcelle>
        </Tabellrad>
        <Tabellrad index={6}>
          <Tabellcelle>Eget underhold</Tabellcelle>
          <Tabellcelle>
            {delberegningBidragsevne.utgifter.borMedAndreVoksne
              ? "Bor med andre voksne"
              : "Bor ikke med andre voksne"}
          </Tabellcelle>
          <Tabellcelle>
            <MathValue
              negativeValue
              value={formatterBeløpForBeregning(
                delberegningBidragsevne.utgifter.underholdBeløp,
                true,
              )}
            />
          </Tabellcelle>
        </Tabellrad>
        <Tabellrad index={7}>
          <Tabellcelle colSpan={2} labelBold>
            Bidragsevne
          </Tabellcelle>
          <Tabellcelle>
            {formatterBeløpForBeregning(
              delberegningBidragsevne.bidragsevne,
              true,
            )}
          </Tabellcelle>
        </Tabellrad>
      </DokumentTabell>
    </>
  );
};
