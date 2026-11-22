import { Fragment } from "react";
import {
  useDokumentFelles,
  TypeInnhold,
} from "~/components/vedtak_felles/FellesContext";
import {
  DokumentTabell,
  type Kolonne,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import {
  BarnOgIndeksår,
  ForklaringRad,
  resultatPerBarn,
  type Resultat,
} from "~/routes/notat/begrunnelse/bidrag/VedtakFelles";
import { formater } from "~/utils/visning";
import { tekst } from "~/utils/tekst";

// Brevet har en tom siste kolonne der notatet viser resultat.
const TOM_KOLONNE = { name: "", content: "" };

function overskrifterEndelig(
  vurderUgyldighet: boolean,
  visningForSaksbehandler: boolean,
): Kolonne[] {
  const bredde = (saksbehandler: string, brev: string) =>
    visningForSaksbehandler ? saksbehandler : brev;
  return [
    ...(vurderUgyldighet
      ? [
          {
            tittel: tekst(
              "notat.begrunnelse.bidrag.vedtak.tabellEndelig.vurderUgyldighet",
              "Vurder ugyldighet",
            ),
            bredde: "20px",
          },
        ]
      : []),
    { tittel: tekst("ord.periode", "Periode"), bredde: bredde("120px", "30%") },
    {
      tittel: visningForSaksbehandler
        ? tekst("ord.type", "Type")
        : tekst("ord.årsak", "Årsak"),
      bredde: bredde("100px", "40%"),
    },
    { tittel: tekst("ord.beløp", "Beløp"), bredde: bredde("120px", "30%") },
    visningForSaksbehandler
      ? { tittel: tekst("ord.resultat", "Resultat"), bredde: "130px" }
      : { tittel: TOM_KOLONNE.name },
  ];
}

export function VedtakEndeligTable({ data }: { data: Resultat[] }) {
  const { typeInnhold } = useDokumentFelles();
  if (
    data.length === 0 ||
    data.every((resultat) => resultat.orkestrertVedtak === null)
  ) {
    return null;
  }

  const visningForSaksbehandler = typeInnhold === TypeInnhold.NOTAT;
  return (
    <>
      {resultatPerBarn(data, true).map((resultat) => {
        const perioder = resultat.orkestrertVedtak?.perioder ?? [];
        const vurderUgyldighet = perioder.some(
          (periode) => periode.klageOmgjøringDetaljer?.kanOpprette35c === true,
        );
        return (
          <Fragment key={resultat.barn.ident}>
            <BarnOgIndeksår resultat={resultat} visIndeksår />
            <DokumentTabell
              kolonner={overskrifterEndelig(
                vurderUgyldighet,
                visningForSaksbehandler,
              )}
            >
              {perioder.map((periode, index) => (
                <Tabellrad key={index} index={index} skipBorderBottom>
                  {vurderUgyldighet && (
                    <Tabellcelle>
                      {periode.klageOmgjøringDetaljer?.kanOpprette35c
                        ? periode.klageOmgjøringDetaljer?.skalOpprette35c
                          ? tekst("ord.ja", "Ja")
                          : tekst("ord.nei", "Nei")
                        : ""}
                    </Tabellcelle>
                  )}
                  <Tabellcelle>
                    {formater(periode.periode, "periode")}
                  </Tabellcelle>
                  <Tabellcelle>
                    {periode.delvedtakstypeVisningsnavn}
                  </Tabellcelle>
                  <Tabellcelle>
                    {periode.erOpphør
                      ? visningForSaksbehandler
                        ? tekst(
                            "notat.begrunnelse.bidrag.vedtak.tabellEndelig.opphørForSaksbehandler",
                            "-",
                          )
                        : tekst("ord.opphør", "Opphør")
                      : formater(periode.faktiskBidrag, "beløpAvrundet")}
                  </Tabellcelle>
                  <Tabellcelle>
                    {visningForSaksbehandler
                      ? periode.resultatkodeVisningsnavn
                      : TOM_KOLONNE.content}
                  </Tabellcelle>
                </Tabellrad>
              ))}
              {visningForSaksbehandler && (
                <ForklaringRad index={perioder.length} />
              )}
            </DokumentTabell>
          </Fragment>
        );
      })}
    </>
  );
}
