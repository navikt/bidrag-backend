import { Fragment, type ReactNode } from "react";
import {
  DokumentTabell,
  type Kolonne as Tabellkolonne,
  type RowContent,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import { verdiForFelt } from "~/utils/felt";
import { Visningsformat } from "~/utils/visning";

/**
 * Kolonne i en tabell, f.eks.
 * `{ felt: "inntekt", tittel: tekst("ord.inntekt", "Inntekt"), bredde: "80px", format: "beløp" }`.
 * `felt` er navnet på feltet i input-JSON som skal vises i kolonnen, og `tittel` hentes fra tekstfilen.
 * `format` er valgfritt og bestemmer hvordan verdien vises, se `visningsformater`.
 */
export type Kolonne = {
  felt: string;
  tittel: string;
  bredde?: string;
  format?: Visningsformat;
};

export type Tabelloppsett = {
  bredde?: string;
  /** Periode som vises på en egen linje over hver rad, f.eks. `{ felt: "periode", format: "periode" }`. */
  periodelinje?: Omit<Kolonne, "tittel">;
  kolonner: Kolonne[];
};

/** Lager verdien som vises. `formatert` er verdien fra input, formatert etter oppsettet. */
export type Celleverdi<T> = (rad: T, formatert: RowContent) => RowContent;

type KolonneTabellProps<T> = {
  oppsett: Tabelloppsett;
  rader: T[];
  /** Overstyr innholdet i en kolonne når verdien ikke kan leses rett fra input. */
  celler?: Record<string, Celleverdi<T>>;
  /** Rader som legges til etter dataradene, f.eks. en sumrad. */
  ekstraRader?: ReactNode;
  layoutAuto?: boolean;
  className?: string;
};

export function kolonneoverskrifter(oppsett: Tabelloppsett): Tabellkolonne[] {
  return oppsett.kolonner.map((k) => ({
    tittel: k.tittel,
    bredde: k.bredde,
  }));
}

export function kolonneceller<T extends object>(
  oppsett: Tabelloppsett,
  rad: T,
  celler: Record<string, Celleverdi<T>> = {},
): RowContent[] {
  return oppsett.kolonner.map((k) => {
    const formatert = verdiForFelt(rad, k.felt, k.format);
    return k.felt in celler ? celler[k.felt](rad, formatert) : formatert;
  });
}

export function periodelinje<T extends object>(
  oppsett: Tabelloppsett,
  rad: T,
): RowContent {
  const linje = oppsett.periodelinje;
  return linje && verdiForFelt(rad, linje.felt, linje.format);
}

export function KolonneTabell<T extends object>({
  oppsett,
  rader,
  celler = {},
  ekstraRader,
  layoutAuto,
  className,
}: KolonneTabellProps<T>) {
  return (
    <DokumentTabell
      layoutAuto={layoutAuto}
      bredde={oppsett.bredde}
      kolonner={kolonneoverskrifter(oppsett)}
      className={className}
    >
      {rader.map((rad, index) => {
        const periode = periodelinje(oppsett, rad);
        return (
          <Fragment key={index}>
            {periode && (
              <Tabellrad index={1} periode>
                <Tabellcelle fullSpan>{periode}</Tabellcelle>
              </Tabellrad>
            )}
            <Tabellrad index={index} harPeriode={!!periode}>
              {kolonneceller(oppsett, rad, celler).map((celle, celleIndex) => (
                <Tabellcelle key={celleIndex}>{celle}</Tabellcelle>
              ))}
            </Tabellrad>
          </Fragment>
        );
      })}
      {ekstraRader}
    </DokumentTabell>
  );
}
