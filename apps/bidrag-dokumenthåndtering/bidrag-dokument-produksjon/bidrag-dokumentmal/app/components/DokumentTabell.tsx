import {
  createContext,
  use,
  type PropsWithChildren,
  type ReactElement,
} from "react";
import { useTheme } from "~/components/notat_felles/ThemeContext";
import {
  tabellCelle,
  tabellCelleklasse,
  tabellEnkelCelle,
  tabellStil,
} from "~/components/tabellStil";

export type RowContent = ReactElement | string | number | null | undefined;
export type Kolonne = {
  tittel: string;
  bredde?: string;
  tekstjustering?: "left" | "right" | "center";
};
const TabellContext = createContext<{
  expressive: boolean;
  antallKolonner: number;
} | null>(null);
type RadContextVerdi = {
  index: number;
  stil: {
    skipPadding?: boolean;
    skipBorderBottom?: boolean;
    periodColumn?: unknown;
    zebraStripe?: boolean;
    className?: string;
  };
  periode: boolean;
};
const RadContext = createContext<RadContextVerdi | null>(null);

function useDokumentTabell() {
  const context = use(TabellContext);
  if (context === null) {
    throw new Error("Tabellrader og celler må brukes i DokumentTabell");
  }
  return context;
}

/** Tabell med felles dokumentstil og rader skrevet som JSX. */
export function DokumentTabell({
  kolonner,
  bredde,
  layoutAuto,
  storrelse = "large",
  className,
  children,
}: PropsWithChildren<{
  kolonner: Kolonne[];
  bredde?: string;
  layoutAuto?: boolean;
  storrelse?: "small" | "large";
  className?: string;
}>) {
  const { styling } = useTheme();
  const { expressive, style } = tabellStil(
    styling,
    storrelse,
    bredde,
    layoutAuto,
  );
  return (
    <TabellContext.Provider
      value={{ expressive, antallKolonner: kolonner.length }}
    >
      <table className={`table ${className}`} style={style}>
        <colgroup>
          {kolonner.map((kolonne, index) => (
            <col key={index} style={{ width: kolonne.bredde }} />
          ))}
        </colgroup>
        <thead
          style={{ wordBreak: "auto-phrase", textWrap: "wrap" }}
          className={expressive ? "bg-table-header" : ""}
        >
          <tr>
            {kolonner.map((kolonne, index) => (
              <th
                key={index}
                className={expressive ? tabellCelle : tabellEnkelCelle}
                style={{
                  width: kolonne.bredde,
                  verticalAlign: "top",
                  textAlign: kolonne.tekstjustering ?? "left",
                }}
              >
                {kolonne.tittel}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </TabellContext.Provider>
  );
}

/** Rad med samme striping som de øvrige dokumenttabellene. */
export function Tabellrad({
  index,
  borderBottom,
  skipBorderBottom,
  skipPadding,
  zebraStripe,
  className,
  periode = false,
  harPeriode = false,
  children,
}: PropsWithChildren<{
  index: number;
  borderBottom?: boolean;
  skipBorderBottom?: boolean;
  skipPadding?: boolean;
  zebraStripe?: boolean;
  className?: string;
  periode?: boolean;
  harPeriode?: boolean;
}>) {
  useDokumentTabell();
  return (
    <RadContext.Provider
      value={{
        index,
        stil: {
          skipBorderBottom,
          skipPadding,
          zebraStripe,
          className,
          periodColumn: harPeriode ? true : null,
        },
        periode,
      }}
    >
      <tr
        style={{
          borderBottom: borderBottom ? "1px solid black" : undefined,
        }}
      >
        {children}
      </tr>
    </RadContext.Provider>
  );
}

/** Celle i en dokumenttabell. */
export function Tabellcelle({
  colSpan,
  colSpanNegative,
  fullSpan,
  labelBold,
  textAlign,
  children,
}: PropsWithChildren<{
  colSpan?: number;
  colSpanNegative?: number;
  fullSpan?: boolean;
  labelBold?: boolean;
  textAlign?: "left" | "right" | "center";
}>) {
  const { expressive, antallKolonner } = useDokumentTabell();
  const rad = use(RadContext);
  if (rad === null) {
    throw new Error("Tabellcelle må brukes i Tabellrad");
  }
  return (
    <td
      className={tabellCelleklasse(
        expressive,
        rad.index,
        rad.stil,
        rad.periode,
      )}
      colSpan={
        fullSpan
          ? antallKolonner
          : colSpanNegative
            ? antallKolonner - colSpanNegative
            : colSpan
      }
      style={{
        fontWeight: labelBold ? "bold" : "normal",
        textAlign: textAlign ?? "left",
      }}
    >
      {children}
    </td>
  );
}

/** Rad som viser detaljer under en tabellrad. */
export function Tabelldetaljer({
  index,
  children,
}: PropsWithChildren<{ index: number }>) {
  const { expressive, antallKolonner } = useDokumentTabell();
  return (
    <tr>
      <td
        colSpan={antallKolonner}
        className={
          expressive
            ? `table_row_details pb-2 ${index % 2 === 1 ? "bg-table-bg-even" : ""}`
            : "table_row_details"
        }
        style={{ paddingTop: "8px" }}
      >
        {children}
      </td>
    </tr>
  );
}
