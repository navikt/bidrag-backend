import {
  CommonTable,
  RowContent,
  TableColumn,
  TableData,
  TableHeader,
  TableRow,
} from "~/components/CommonTable";
import { verdiForFelt } from "~/utils/felt";
import { Visningsformat } from "~/utils/visning";

/**
 * Kolonne i en tabell, f.eks.
 * `{ felt: "inntekt", tittel: t.tabellInnvilget.inntekt, bredde: "80px", format: "beløp" }`.
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
  ekstraRader?: TableData["rows"];
  layoutAuto?: boolean;
  className?: string;
};

export function kolonneoverskrifter(oppsett: Tabelloppsett): TableHeader[] {
  return oppsett.kolonner.map((k) => ({ name: k.tittel, width: k.bredde }));
}

export function kolonneceller<T extends object>(
  oppsett: Tabelloppsett,
  rad: T,
  celler: Record<string, Celleverdi<T>> = {},
): TableColumn[] {
  return oppsett.kolonner.map((k) => {
    const formatert = verdiForFelt(rad, k.felt, k.format);
    return {
      content: k.felt in celler ? celler[k.felt](rad, formatert) : formatert,
    };
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
  ekstraRader = [],
  layoutAuto,
  className,
}: KolonneTabellProps<T>) {
  const data: TableData = {
    headers: kolonneoverskrifter(oppsett),
    rows: rader
      .map((rad): TableRow => ({
        periodColumn: periodelinje(oppsett, rad),
        columns: kolonneceller(oppsett, rad, celler),
      }))
      .concat(ekstraRader),
  };
  return (
    <CommonTable
      layoutAuto={layoutAuto}
      width={oppsett.bredde}
      data={data}
      className={className}
    />
  );
}
