import ord from "~/tekster/ord.json";
import { NotatDelberegningBidragspliktigesBeregnedeTotalbidragDto } from "~/types/Api";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import særbidragOpplysningerTekster from "~/tekster/notat/særbidrag/opplysninger.json";
import { CommonTable, TableColumn, TableRow } from "~/components/CommonTable";
import { CalculationTable } from "~/components/vedtak/CalculationTable";

const tekst = særbidragOpplysningerTekster.beregningsdetaljer;

const t = tekst.bpsBeregnedeTotalbidrag;

export const BPsBeregnedeTotalbidrag = ({
  delberegning,
}: {
  delberegning: NotatDelberegningBidragspliktigesBeregnedeTotalbidragDto;
}) => {
  return (
    <div>
      <h4>{t.tittel}</h4>
      <CommonTable
        data={{
          headers: [
            {
              name: ord.barn,
            },
            {
              name: ord.saksnummer,
            },
            {
              name: t.kolonner.løpendeBidrag,
            },
            {
              name: t.kolonner.samvær,
            },
            {
              name: t.kolonner.reduksjonAvBpsAndelAvU,
            },
            {
              name: ord.sum,
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
                            title={t.reduksjonAvBpsAndelAvU.tittel}
                            data={[
                              {
                                label: `${t.reduksjonAvBpsAndelAvU.bpsAndelAvU}${
                                  !erVedtakFraBBM &&
                                  row.bidragJustertForNettoBarnetilleggBP
                                    ? t.reduksjonAvBpsAndelAvU
                                        .justertOppTilBpsNettoBarnetillegg
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
                                label: t.reduksjonAvBpsAndelAvU.faktiskBidrag,
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
                              label: ord.resultat,
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
                    content: t.beregnetTotalbidrag,
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
                    content: t.forklaring,
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
