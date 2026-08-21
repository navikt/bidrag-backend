import { NotatDelberegningBidragspliktigesBeregnedeTotalbidragDto } from "~/types/Api";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import tekster from "~/tekster";
import { CommonTable, TableColumn, TableRow } from "~/components/CommonTable";
import { CalculationTable } from "~/components/vedtak/CalculationTable";

export const BPsBeregnedeTotalbidrag = ({
  delberegning,
}: {
  delberegning: NotatDelberegningBidragspliktigesBeregnedeTotalbidragDto;
}) => {
  return (
    <div>
      <h4>{`${tekster.begreper.bidragspliktiges} beregnede totalbidrag`}</h4>
      <CommonTable
        data={{
          headers: [
            {
              name: "Barn",
            },
            {
              name: "Saksnummer",
            },
            {
              name: "Løpende bidrag",
            },
            {
              name: "Samvær",
            },
            {
              name: "Reduksjon av BPs andel av U",
            },
            {
              name: "Sum",
            },
          ],
          rows: delberegning.beregnetBidragPerBarnListe
            .map(({ beregnetBidragPerBarn: row, personidentBarn }) => {
              const showBeregningAvU =
                row.beregnetBidrag !== 0 || row.faktiskBeløp !== 0;
              const erVedtakFraBBM =
                row.erVedtakKildeBBM === undefined ||
                row.erVedtakKildeBBM === true;
              return {
                expandableContent: showBeregningAvU
                  ? [
                      {
                        content: (
                          <CalculationTable
                            width={"200px"}
                            simpleStyling
                            className={"ml-2"}
                            title={`Reduksjon av BPs andel av U`}
                            data={[
                              {
                                label: `BPs andel av U${
                                  !erVedtakFraBBM &&
                                  row.bidragJustertForNettoBarnetilleggBP
                                    ? " (justert opp til BPs netto barnetillegg)"
                                    : ""
                                }`,

                                value: formatterBeløpForBeregning(
                                  erVedtakFraBBM
                                    ? row.beregnetBeløp
                                    : row.bruttoBidragEtterBarnetilleggBM,
                                  true,
                                ),
                              },
                              {
                                label: `Faktisk bidrag`,
                                value: (
                                  <div>
                                    -{" "}
                                    {formatterBeløpForBeregning(
                                      erVedtakFraBBM
                                        ? row.faktiskBeløp
                                        : row.bruttoBidragEtterBarnetilleggBP,
                                      true,
                                    )}
                                  </div>
                                ),
                              },
                            ]}
                            result={{
                              label: "Resultat",
                              value: formatterBeløpForBeregning(
                                row.reduksjonUnderholdskostnad,
                                true,
                              ),
                            }}
                          />
                        ),
                      },
                    ]
                  : undefined,
                columns: [
                  {
                    content: personidentBarn,
                  },
                  {
                    content: row.saksnummer,
                  },
                  {
                    content: formatterBeløpForBeregning(row.løpendeBeløp, true),
                  },
                  {
                    content: formatterBeløpForBeregning(
                      row.samværsfradrag,
                      true,
                    ),
                  },
                  {
                    content: formatterBeløpForBeregning(
                      row.reduksjonUnderholdskostnad,
                      true,
                    ),
                  },
                  {
                    content: formatterBeløpForBeregning(
                      row.beregnetBidrag,
                      true,
                    ),
                  },
                ],
              };
            })
            .concat([
              {
                skipPadding: true,
                columns: [
                  {
                    content: "",
                    colSpan: 4,
                  } as TableColumn,
                  {
                    content: "Beregnet totalbidrag: ",
                    labelBold: true,
                    textAlign: "right",
                    colSpan: 1,
                  } as TableColumn,
                  {
                    textAlign: "center",
                    content: formatterBeløpForBeregning(
                      delberegning.bidragspliktigesBeregnedeTotalbidrag,
                      true,
                    ),
                  } as TableColumn,
                ] as TableColumn[],
              } as TableRow,
              {
                zebraStripe: false,
                skipPadding: true,
                skipBorderBottom: true,
                columns: [
                  {
                    content: "U = Underholdskostnad, BP = Bidragspliktig",
                    colSpan: 7,
                  },
                ] as TableColumn[],
              } as TableRow,
            ]) as TableRow[],
        }}
      />
    </div>
  );
};
