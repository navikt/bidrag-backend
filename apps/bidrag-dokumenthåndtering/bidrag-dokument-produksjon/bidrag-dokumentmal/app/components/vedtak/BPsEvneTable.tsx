import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import tekster from "~/tekster";
import {
  DokumentmalDelberegningBidragsevneDto,
  DokumentmalResultatBeregningInntekterDto,
} from "~/types/Api";
import { CommonTable } from "~/components/CommonTable";
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
                  content: `Inntekt`,
                },

                {
                  content: (
                    <MathDivision
                      top={`${formatterBeløpForBeregning(inntekter.inntektBP, true)}/år`}
                      bottom={12}
                    />
                  ),
                },
                {
                  content: (
                    <MathValue
                      value={formatterBeløpForBeregning(
                        inntekter.inntektBPMånedlig,
                        true,
                      )}
                    />
                  ),
                },
              ],
            },
            {
              columns: [
                {
                  content: `Skatt`,
                },
                {
                  content: (
                    <MathDivision
                      negativeValue
                      top={`${formatterBeløpForBeregning(delberegningBidragsevne.skatt.skattAlminneligInntekt, true)}/år`}
                      bottom={12}
                    />
                  ),
                },
                {
                  content: (
                    <MathValue
                      negativeValue
                      value={formatterBeløpForBeregning(
                        delberegningBidragsevne.skatt
                          .skattAlminneligInntektMånedsbeløp,
                        true,
                      )}
                    />
                  ),
                },
              ],
            },
            {
              columns: [
                {
                  content: `Trygdeavgift`,
                },
                {
                  content: (
                    <MathDivision
                      negativeValue
                      top={`${formatterBeløpForBeregning(delberegningBidragsevne.skatt.trygdeavgift, true)}/år`}
                      bottom={12}
                    />
                  ),
                },
                {
                  content: (
                    <MathValue
                      negativeValue
                      value={formatterBeløpForBeregning(
                        delberegningBidragsevne.skatt.trygdeavgiftMånedsbeløp,
                        true,
                      )}
                    />
                  ),
                },
              ],
            },
            {
              columns: [
                {
                  content: `Trinnskatt`,
                },
                {
                  content: (
                    <MathDivision
                      negativeValue
                      top={`${formatterBeløpForBeregning(delberegningBidragsevne.skatt.trinnskatt, true)}/år`}
                      bottom={12}
                    />
                  ),
                },
                {
                  content: (
                    <MathValue
                      negativeValue
                      value={formatterBeløpForBeregning(
                        delberegningBidragsevne.skatt.trinnskattMånedsbeløp,
                        true,
                      )}
                    />
                  ),
                },
              ],
            },
            {
              columns: [
                {
                  content: "Underhold egne barn i husstand",
                },
                {
                  content: (
                    <MathMultiplication
                      negativeValue
                      left={`${formatterBeløpForBeregning(delberegningBidragsevne.underholdEgneBarnIHusstand.sjablon, true)}`}
                      right={
                        delberegningBidragsevne.underholdEgneBarnIHusstand
                          .antallBarnIHusstanden
                      }
                    />
                  ),
                },
                {
                  content: (
                    <MathValue
                      negativeValue
                      value={formatterBeløpForBeregning(
                        delberegningBidragsevne.underholdEgneBarnIHusstand
                          .måndesbeløp,
                        true,
                      )}
                    />
                  ),
                },
              ],
            },
            {
              columns: [
                {
                  content: `Bougift`,
                },
                {
                  content: delberegningBidragsevne.utgifter.borMedAndreVoksne
                    ? "Bor med andre voksne"
                    : "Bor ikke med andre voksne",
                },
                {
                  content: (
                    <MathValue
                      negativeValue
                      value={formatterBeløpForBeregning(
                        delberegningBidragsevne.utgifter.boutgiftBeløp,
                        true,
                      )}
                    />
                  ),
                },
              ],
            },
            {
              columns: [
                {
                  content: `Eget underhold`,
                },
                {
                  content: delberegningBidragsevne.utgifter.borMedAndreVoksne
                    ? "Bor med andre voksne"
                    : "Bor ikke med andre voksne",
                },
                {
                  content: (
                    <MathValue
                      negativeValue
                      value={formatterBeløpForBeregning(
                        delberegningBidragsevne.utgifter.underholdBeløp,
                        true,
                      )}
                    />
                  ),
                },
              ],
            },
            {
              columns: [
                {
                  content: `Bidragsevne`,
                  colSpan: 2,
                  labelBold: true,
                },
                {
                  content: formatterBeløpForBeregning(
                    delberegningBidragsevne.bidragsevne,
                    true,
                  ),
                },
              ],
            },
          ],
        }}
      />
    </>
  );
};
