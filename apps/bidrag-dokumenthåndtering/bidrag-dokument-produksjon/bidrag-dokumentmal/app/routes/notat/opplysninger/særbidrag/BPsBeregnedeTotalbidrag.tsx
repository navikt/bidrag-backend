import { Fragment } from "react";
import ord from "~/tekster/ord.yaml";
import { NotatDelberegningBidragspliktigesBeregnedeTotalbidragDto } from "~/types/Api";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import særbidragOpplysningerTekster from "~/tekster/notat/opplysninger/særbidrag.yaml";
import {
  DokumentTabell,
  Tabellcelle,
  Tabelldetaljer,
  Tabellrad,
} from "~/components/DokumentTabell";
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
      <DokumentTabell
        kolonner={[
          { tittel: ord.barn },
          { tittel: ord.saksnummer },
          { tittel: t.kolonner.løpendeBidrag },
          { tittel: t.kolonner.samvær },
          { tittel: t.kolonner.reduksjonAvBpsAndelAvU },
          { tittel: ord.sum },
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
            {t.beregnetTotalbidrag}
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
          <Tabellcelle colSpan={7}>{t.forklaring}</Tabellcelle>
        </Tabellrad>
      </DokumentTabell>
    </div>
  );
};
