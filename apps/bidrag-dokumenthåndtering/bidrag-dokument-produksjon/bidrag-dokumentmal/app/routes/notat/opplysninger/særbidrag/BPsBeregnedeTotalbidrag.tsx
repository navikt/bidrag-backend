import { Fragment } from "react";
import { NotatDelberegningBidragspliktigesBeregnedeTotalbidragDto } from "~/types/Api";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import {
  DokumentTabell,
  Tabellcelle,
  Tabelldetaljer,
  Tabellrad,
} from "~/components/DokumentTabell";
import { CalculationTable } from "~/components/CalculationTable";
import { tekst } from "~/utils/tekst";
export const BPsBeregnedeTotalbidrag = ({
  delberegning,
}: {
  delberegning: NotatDelberegningBidragspliktigesBeregnedeTotalbidragDto;
}) => {
  return (
    <div>
      <h4>
        {tekst(
          "notat.opplysninger.særbidrag.beregningsdetaljer.bpsBeregnedeTotalbidrag.tittel",
          "Bidragspliktiges beregnede totalbidrag",
        )}
      </h4>
      <DokumentTabell
        kolonner={[
          { tittel: tekst("ord.barn", "Barn") },
          { tittel: tekst("ord.saksnummer", "Saksnummer") },
          {
            tittel: tekst(
              "notat.opplysninger.særbidrag.beregningsdetaljer.bpsBeregnedeTotalbidrag.kolonner.løpendeBidrag",
              "Løpende bidrag",
            ),
          },
          {
            tittel: tekst(
              "notat.opplysninger.særbidrag.beregningsdetaljer.bpsBeregnedeTotalbidrag.kolonner.samvær",
              "Samvær",
            ),
          },
          {
            tittel: tekst(
              "notat.opplysninger.særbidrag.beregningsdetaljer.bpsBeregnedeTotalbidrag.kolonner.reduksjonAvBpsAndelAvU",
              "Reduksjon av BPs andel av U",
            ),
          },
          { tittel: tekst("ord.sum", "Sum") },
        ]}
      >
        {delberegning.beregnetBidragPerBarnListe.map(
          ({ beregnetBidragPerBarn: row, personidentBarn }, index: number) => {
            const showBeregningAvU =
              row.beregnetBidrag !== 0 || row.faktiskBeløp !== 0;
            const erVedtakFraBBM =
              row.erVedtakKildeBBM === undefined ||
              row.erVedtakKildeBBM === true;
            return (
              <Fragment key={`${personidentBarn}-${row.saksnummer}-${index}`}>
                <Tabellrad index={index}>
                  <Tabellcelle>{personidentBarn}</Tabellcelle>
                  <Tabellcelle>{row.saksnummer}</Tabellcelle>
                  <Tabellcelle>
                    {formatterBeløpForBeregning(row.løpendeBeløp, true)}
                  </Tabellcelle>
                  <Tabellcelle>
                    {formatterBeløpForBeregning(row.samværsfradrag, true)}
                  </Tabellcelle>
                  <Tabellcelle>
                    {formatterBeløpForBeregning(
                      row.reduksjonUnderholdskostnad,
                      true,
                    )}
                  </Tabellcelle>
                  <Tabellcelle>
                    {formatterBeløpForBeregning(row.beregnetBidrag, true)}
                  </Tabellcelle>
                </Tabellrad>
                {showBeregningAvU && (
                  <Tabelldetaljer index={index}>
                    <CalculationTable
                      width={"200px"}
                      resultColumnWidth="100px"
                      alignResults
                      simpleStyling
                      withContainer={false}
                      className={"ml-2"}
                      data={[
                        {
                          label: `${tekst("notat.opplysninger.særbidrag.beregningsdetaljer.bpsBeregnedeTotalbidrag.reduksjonAvBpsAndelAvU.bpsAndelAvU", "BPs andel av U")}${
                            !erVedtakFraBBM &&
                            row.bidragJustertForNettoBarnetilleggBP
                              ? tekst(
                                  "notat.opplysninger.særbidrag.beregningsdetaljer.bpsBeregnedeTotalbidrag.reduksjonAvBpsAndelAvU.justertOppTilBpsNettoBarnetillegg",
                                  " (justert opp til BPs netto barnetillegg)",
                                )
                              : ""
                          }`,
                          result: formatterBeløpForBeregning(
                            erVedtakFraBBM
                              ? row.beregnetBeløp
                              : row.bruttoBidragEtterBarnetilleggBM,
                            true,
                          ),
                        },
                        {
                          label: tekst(
                            "notat.opplysninger.særbidrag.beregningsdetaljer.bpsBeregnedeTotalbidrag.reduksjonAvBpsAndelAvU.faktiskBidrag",
                            "Faktisk bidrag",
                          ),
                          result: (
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
                      summary={{
                        label: tekst("ord.resultat", "Resultat"),
                        result: formatterBeløpForBeregning(
                          row.reduksjonUnderholdskostnad,
                          true,
                        ),
                      }}
                    />
                  </Tabelldetaljer>
                )}
              </Fragment>
            );
          },
        )}
        <Tabellrad
          index={delberegning.beregnetBidragPerBarnListe.length}
          skipPadding
        >
          <Tabellcelle colSpan={4} />
          <Tabellcelle labelBold textAlign="right" colSpan={1}>
            {tekst(
              "notat.opplysninger.særbidrag.beregningsdetaljer.bpsBeregnedeTotalbidrag.beregnetTotalbidrag",
              "Beregnet totalbidrag: ",
            )}
          </Tabellcelle>
          <Tabellcelle textAlign="center">
            {formatterBeløpForBeregning(
              delberegning.bidragspliktigesBeregnedeTotalbidrag,
              true,
            )}
          </Tabellcelle>
        </Tabellrad>
        <Tabellrad
          index={delberegning.beregnetBidragPerBarnListe.length + 1}
          zebraStripe={false}
          skipPadding
          skipBorderBottom
        >
          <Tabellcelle colSpan={7}>
            {tekst(
              "notat.opplysninger.særbidrag.beregningsdetaljer.bpsBeregnedeTotalbidrag.forklaring",
              "U = Underholdskostnad, BP = Bidragspliktig",
            )}
          </Tabellcelle>
        </Tabellrad>
      </DokumentTabell>
    </div>
  );
};
