import { DataViewTableV2 } from "~/components/DataViewTableV2";
import { Celleverdi } from "~/components/KolonneTabell";
import { verdiForFelt } from "~/utils/felt";
import { Visningsformat } from "~/utils/visning";

/**
 * Ledetekst og verdi, f.eks.
 * `{ felt: "inntekter.inntektBM", tittel: t.inntekter.inntektBidragsmottaker, format: "beløp" }`.
 */
export type Felt = {
  felt: string;
  tittel: string;
  format?: Visningsformat;
};

export type Feltlisteoppsett = {
  tittel?: string;
  bredde?: string;
  ledetekstBredde?: string;
  felter: Felt[];
};

type FeltlisteProps<T> = {
  oppsett: Feltlisteoppsett;
  kilde: T;
  /** Overstyr verdien for et felt når den ikke kan leses rett fra input. */
  verdier?: Record<string, Celleverdi<T>>;
  /** Felter som ikke skal vises. */
  skjul?: string[];
  className?: string;
};

export function Feltliste<T extends object>({
  oppsett,
  kilde,
  verdier = {},
  skjul = [],
  className,
}: FeltlisteProps<T>) {
  return (
    <DataViewTableV2
      title={oppsett.tittel}
      width={oppsett.bredde}
      labelColWidth={oppsett.ledetekstBredde}
      className={className}
      data={oppsett.felter
        .filter((f) => !skjul.includes(f.felt))
        .map((f) => {
          const formatert = verdiForFelt(kilde, f.felt, f.format);
          return {
            label: f.tittel,
            value:
              f.felt in verdier ? verdier[f.felt](kilde, formatert) : formatert,
          };
        })}
    />
  );
}
