import { RowContent } from "~/components/CommonTable";
import { formater, Visningsformat } from "~/utils/visning";
import ord from "~/tekster/ord.json";

/**
 * Henter verdien som skal vises for et felt i input-JSON.
 * `felt` kan peke inn i objekter med punktum, f.eks. `inntekter.inntektBM`.
 * Ja/nei-verdier vises som Ja eller Nei. Se `visningsformater` for gyldige formater.
 */
export function verdiForFelt(
  kilde: object,
  felt: string,
  format?: Visningsformat,
): RowContent {
  const verdi = felt
    .split(".")
    .reduce<unknown>(
      (objekt, nøkkel) =>
        (objekt as Record<string, unknown> | undefined)?.[nøkkel],
      kilde,
    );
  if (typeof verdi === "boolean") return verdi ? ord.ja : ord.nei;
  return formater(verdi, format);
}
