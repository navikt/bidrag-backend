import { DokumentmalDelberegningBidragspliktigesBeregnedeTotalbidragDto } from "~/types/Api";
import {
  formatterBeløp,
  formatterBeløpForBeregning,
} from "~/utils/visningsnavn";
import tekster from "~/tekster";
import {
  CommonTable,
  TableColumn,
  TableHeader,
  TableRow,
} from "~/components/CommonTable";
import { CalculationTable } from "~/components/vedtak/CalculationTable";

export const BPsBeregnedeTotalbidrag = ({
  delberegning,
}: {
  delberegning: DokumentmalDelberegningBidragspliktigesBeregnedeTotalbidragDto;
}) => {
  const løperIUtlandskValuta = delberegning.beregnetBidragPerBarnListe.some(
    ({ beregnetBidragPerBarn: row }) => row.valutakode !== "NOK",
  );

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
            løperIUtlandskValuta && {
              name: "Valutakurs",
            },
            løperIUtlandskValuta && {
              name: "Løpende bidrag (NOK)",
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
          ].filter((header) => header != null) as TableHeader[],
          rows: delberegning.beregnetBidragPerBarnListe
            .map<TableRow>(
              ({ beregnetBidragPerBarn: row, valutakurs, personidentBarn }) => {
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
                                      : (row.bruttoBidragEtterBarnetilleggBM ??
                                          0),
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
                                          : (row.bruttoBidragEtterBarnetilleggBP ??
                                              0),
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
                      content:
                        row.valutakode === "NOK"
                          ? formatterBeløpForBeregning(row.løpendeBeløp, true)
                          : `${formatterBeløp(row.løpendeBeløp)} (${row.valutakode})`,
                    },
                    løperIUtlandskValuta && {
                      content: formatterBeløp(valutakurs ?? 0),
                    },
                    løperIUtlandskValuta && {
                      content: formatterBeløp(row.beregnetBidragNOK ?? 0),
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
                        row.beregnetBidragNOK,
                        true,
                      ),
                    },
                  ].filter((column) => column != null) as TableColumn[],
                };
              },
            )
            .concat([
              {
                skipPadding: true,
                columns: [
                  {
                    content: "",
                    colSpan: løperIUtlandskValuta ? 5 : 4,
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
                    colSpan: løperIUtlandskValuta ? 7 : 6,
                  },
                ] as TableColumn[],
              } as TableRow,
            ]) as TableRow[],
        }}
      />
    </div>
  );
};