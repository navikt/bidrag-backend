import { PropsWithChildren } from "react";
import {
  useNotatFelles,
  useVedleggNummer,
  Vedlegg,
} from "~/components/notat_felles/NotatContext";
import elementIds from "~/utils/elementIds";

const ankerForVedlegg: Record<Vedlegg, string> = {
  underholdskostnader: elementIds.vedleggUnderholdskostnader,
  boforhold: elementIds.vedleggBoforhold,
  inntekter: elementIds.vedleggInntekter,
  samvær: elementIds.vedleggSamvær,
  beregningsdetaljer: elementIds.vedleggBeregningsdetaljer,
};

type LenkeTilVedleggProps = {
  vedlegg: Vedlegg;
  /** Teksten i lenken. `{}` byttes ut med vedleggsnummeret. */
  lenketekst: string;
};

/** Lenke til et vedlegg. Vises bare i det komplette notatet, der vedlegget står i samme dokument. */
export function LenkeTilVedlegg({ vedlegg, lenketekst }: LenkeTilVedleggProps) {
  const { visLenkerTilVedlegg } = useNotatFelles();
  const vedleggNummer = useVedleggNummer(vedlegg);
  if (!visLenkerTilVedlegg) return null;
  return (
    <a href={`#${ankerForVedlegg[vedlegg]}`}>
      {lenketekst.replace("{}", `${vedleggNummer}`)}
    </a>
  );
}

type SeksjonProps = PropsWithChildren<{
  tittel: string;
  vedlegg: Vedlegg;
  /** Teksten i lenken til vedlegget. `{}` byttes ut med vedleggsnummeret. */
  lenketekst: string;
}>;

/** En hoveddel av notatet, med overskrift og lenke til vedlegget som hører til. */
export function Seksjon({
  tittel,
  vedlegg,
  lenketekst,
  children,
}: SeksjonProps) {
  return (
    <>
      <div className={"elements_inline section-title"}>
        <h2>{tittel}</h2>
        <LenkeTilVedlegg vedlegg={vedlegg} lenketekst={lenketekst} />
      </div>
      {children}
    </>
  );
}

type UnderseksjonProps = PropsWithChildren<{
  tittel: string;
  /** Sett til `false` når underseksjonen skal ligge tett inntil det som står over. */
  avstandOver?: boolean;
}>;

/** En mindre del av en seksjon, for eksempel én tabell med egen overskrift. */
export function Underseksjon({
  tittel,
  avstandOver = true,
  children,
}: UnderseksjonProps) {
  return (
    <div className={avstandOver ? "mb-4 mt-4" : "mb-4"}>
      <h4>{tittel}</h4>
      {children}
    </div>
  );
}
