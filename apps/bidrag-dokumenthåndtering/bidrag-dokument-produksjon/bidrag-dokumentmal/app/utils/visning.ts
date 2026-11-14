import { RowContent } from "~/components/CommonTable";
import { dateToDDMMYYYY, deductDays, formatPeriode } from "~/utils/date-utils";
import {
  formatterBeløp,
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";

/**
 * Formatene som kan brukes for kolonner og felter i malene, f.eks.
 * `{ felt: "beløp", tittel: t.tabellInnvilget.forskudd, format: "beløp" }`.
 *
 * Funksjonene er bevisst enkle og uten React, slik at de kan skrives om til Typst.
 * Tall vises med norsk format: mellomrom som tusenskille og komma som desimaltegn.
 */
type Periode = { fom: string; til?: string | null };

export const visningsformater = {
  /** `1234.5` vises som `1 234,5`. Inntil to desimaler. Tomt vises som `0`. */
  beløp: (verdi: unknown) => formatterBeløp(verdi as number),
  /** `1234.5` vises som `1 235`. Avrundet til hele kroner. Tomt vises som `0`. */
  beløpAvrundet: (verdi: unknown) =>
    formatterBeløpForBeregning(verdi as number),
  /** `0.455` og `45.5` vises begge som `45,5 %`. Tomt vises som `0%`. */
  prosent: (verdi: unknown) => formatterProsent(verdi as number),
  /** `2024-03-15` vises som `15.03.2024`. */
  dato: (verdi: unknown) => dateToDDMMYYYY(verdi as string),
  /**
   * Månedsperiode der til-måneden ikke er med:
   * `{ fom: "2024-01", til: "2024-07" }` vises som `01.01.2024 - 30.06.2024`.
   * Løpende periode (uten til) vises bare med fra-dato.
   */
  periode: (verdi: unknown) => {
    const periode = verdi as Periode;
    return formatPeriode(periode.fom, deductDays(periode.til, 1));
  },
  /**
   * Månedsperiode der til-måneden er med:
   * `{ fom: "2024-01", til: "2024-07" }` vises som `01.01.2024 - 31.07.2024`.
   */
  periodeTilOgMedTilMåned: (verdi: unknown) => {
    const periode = verdi as Periode;
    return formatPeriode(periode.fom, periode.til);
  },
};

export type Visningsformat = keyof typeof visningsformater;

/** Formaterer en verdi fra input. Uten format vises verdien slik den er. */
export function formater(verdi: unknown, format?: Visningsformat): RowContent {
  return format ? visningsformater[format](verdi) : (verdi as RowContent);
}
