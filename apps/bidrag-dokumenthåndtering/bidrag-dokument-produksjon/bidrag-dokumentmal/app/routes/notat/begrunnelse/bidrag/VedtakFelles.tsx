import { groupBy } from "~/utils/array-utils";
import { sortByAge } from "~/utils/date-utils";
import { DokumentmalResultatBidragsberegningBarnDto } from "~/types/Api";
import { Tabellcelle, Tabellrad } from "~/components/DokumentTabell";
import { FeltTabell, Feltrad, Feltrader } from "~/components/FeltTabell";
import TableGjelderBarn from "~/components/TableGjelderBarn";
import { tekst } from "~/utils/tekst";

export type Resultat = DokumentmalResultatBidragsberegningBarnDto;

/** Ett resultat per barn, sortert etter alder når `sorter` er satt. */
export function resultatPerBarn(data: Resultat[], sorter = false): Resultat[] {
  const perBarn = groupBy(data, (resultat) => resultat.barn?.ident!).map(
    ([, resultater]) => resultater[0],
  );
  return sorter ? perBarn.sort((a, b) => sortByAge(a.barn, b.barn)) : perBarn;
}

export function BarnOgIndeksår({
  resultat,
  visIndeksår,
}: {
  resultat: Resultat;
  visIndeksår: boolean;
}) {
  const gjelderBarn = resultat.barn;
  return (
    <>
      <TableGjelderBarn gjelderBarn={gjelderBarn} />
      {resultat.indeksår && (
        <div className="mb-1 mt-2">
          <FeltTabell>
            <Feltrader
              rows={[
                visIndeksår
                  ? {
                      label: tekst(
                        "notat.begrunnelse.bidrag.vedtak.nesteIndeksår",
                        "Neste indeksår",
                      ),
                      value: resultat.indeksår,
                    }
                  : {},
              ]}
            />
          </FeltTabell>
        </div>
      )}
      {gjelderBarn.innbetaltBeløp && (
        <div className="mt-2 mb-2">
          <FeltTabell>
            <Feltrad
              label={tekst(
                "notat.begrunnelse.bidrag.vedtak.innbetaltBeløp",
                "Innbetalt beløp",
              )}
              labelBold
            >
              {gjelderBarn.innbetaltBeløp}
            </Feltrad>
          </FeltTabell>
        </div>
      )}
    </>
  );
}

export function ForklaringRad({ index }: { index: number }) {
  return (
    <Tabellrad
      index={index}
      skipBorderBottom
      zebraStripe={false}
      skipPadding
      className="pt-2"
    >
      <Tabellcelle fullSpan>
        {tekst(
          "notat.begrunnelse.bidrag.vedtak.tabellBeregning.forklaringForkortelser",
          "U = Underholdskostnad, BP = Bidragspliktig, BM = Bidragsmottaker",
        )}
      </Tabellcelle>
    </Tabellrad>
  );
}
