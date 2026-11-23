import { FeltTabell, JustertFeltrad } from "~/components/FeltTabell";
import { Celleverdi } from "~/components/KolonneTabell";
import { verdiForFelt } from "~/utils/felt";
import { Visningsformat } from "~/utils/visning";

/**
 * Ledetekst og verdi, f.eks.
 * `{ felt: "inntekter.inntektBM", tittel: tekst("notat.begrunnelse.særbidrag.vedtak.inntekter.inntektBidragsmottaker", "Inntekt Bidragsmottaker"), format: "beløp" }`.
 */
export type Felt = {
  felt: string;
  tittel: string;
  format?: Visningsformat;
};

export type Feltlisteoppsett = {
  tittel?: string;
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
};

export function Feltliste<T extends object>({
  oppsett,
  kilde,
  verdier = {},
  skjul = [],
}: FeltlisteProps<T>) {
  return (
    <>
      {oppsett.tittel && <h4>{oppsett.tittel}</h4>}
      <FeltTabell labelColWidth={oppsett.ledetekstBredde}>
        {oppsett.felter
          .filter(({ felt }) => !skjul.includes(felt))
          .map(({ felt, tittel, format }) => {
            const formatert = verdiForFelt(kilde, felt, format);
            return (
              <JustertFeltrad key={felt} label={tittel}>
                {felt in verdier ? verdier[felt](kilde, formatert) : formatert}
              </JustertFeltrad>
            );
          })}
      </FeltTabell>
    </>
  );
}
