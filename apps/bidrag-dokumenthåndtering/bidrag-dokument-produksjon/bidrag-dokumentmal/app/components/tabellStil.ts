import { Styling } from "~/components/notat_felles/ThemeContext";

export const tabellCellekant =
  "border-b border-solid border-t-0 border-r-0 border-l-0";
export const tabellCelle =
  "pb-2 pt-2 pl-3 pr-3 border-b border-solid border-t-0 border-r-0 border-l-0";
export const tabellEnkelCelle =
  "pb-1 pt-1 border-b border-solid border-t-0 border-r-0 border-l-0";
export const tabellCelleUtenKant = "pb-2 pt-2 pl-3 pr-3";

type Radstil = {
  skipPadding?: boolean;
  skipBorderBottom?: boolean;
  periodColumn?: unknown;
  zebraStripe?: boolean;
  className?: string;
};

/** Gir dataceller samme ramme og striping i begge tabellvariantene. */
export function tabellCelleklasse(
  expressive: boolean,
  index: number,
  row: Radstil,
  isPeriodColumn = false,
) {
  return `
              ${
                expressive
                  ? `${
                      !row.skipPadding
                        ? row.skipBorderBottom || isPeriodColumn
                          ? tabellCelleUtenKant
                          : tabellCelle
                        : row.skipBorderBottom
                          ? ""
                          : tabellCellekant
                    } ${row.periodColumn ? false : (row.zebraStripe != false && index % 2 == 1) || row.zebraStripe == true ? "bg-table-bg-even" : ""} ${row.className}`
                  : tabellCellekant
              } `;
}

/** Gir tabellen samme bredde og layout uansett hvordan radene bygges. */
export function tabellStil(
  styling: Styling,
  size: "small" | "large",
  width?: string,
  layoutAuto?: boolean,
) {
  const expressive = styling === "V2" && size === "large";
  return {
    expressive,
    style: {
      ...(expressive ? { width: "650px" } : { width, maxWidth: "600px" }),
      tableLayout: layoutAuto || styling === "V2" ? "auto" : "fixed",
    } as const,
  };
}
