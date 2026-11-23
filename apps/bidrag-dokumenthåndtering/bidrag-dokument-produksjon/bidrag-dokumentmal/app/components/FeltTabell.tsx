import { createContext, use, type ReactNode } from "react";

type FeltradProps = {
  label?: string;
  labelBold?: boolean;
  children?: ReactNode;
};

type Felt = Omit<FeltradProps, "children"> & {
  value?: ReactNode;
  result?: ReactNode;
};

type Rader = { rows: readonly (Felt | null | undefined)[] };

const LabelColWidth = createContext<string | undefined>(undefined);

/** Tabell for ledetekster og verdier. Tittel og plassering settes utenfor tabellen. */
export function FeltTabell({
  labelColWidth,
  children,
}: {
  labelColWidth?: string;
  children: ReactNode;
}) {
  return (
    <LabelColWidth.Provider value={labelColWidth}>
      <table style={{ borderSpacing: 0 }}>
        <tbody>{children}</tbody>
      </table>
    </LabelColWidth.Provider>
  );
}

/** Felles rad med valgfri ledetekst og resultatkolonne. */
function Rad({
  label,
  labelBold,
  children,
  colSpan,
  valueClassName,
  resultCell,
}: FeltradProps & {
  colSpan?: number;
  valueClassName?: string;
  resultCell?: ReactNode;
}) {
  const labelColWidth = use(LabelColWidth);
  return (
    <tr>
      {label && (
        <td
          style={{
            verticalAlign: "text-top",
            width: labelColWidth,
            paddingRight: "5px",
            fontWeight: labelBold ? "bold" : "normal",
          }}
        >
          {label}:{" "}
        </td>
      )}
      <td colSpan={colSpan} className={valueClassName}>
        {children}
      </td>
      {resultCell}
    </tr>
  );
}

/** Rad der verdien står i én celle, også uten ledetekst. */
export function Feltrad(props: FeltradProps) {
  return <Rad {...props} />;
}

/** Rad der verdicellen fyller plassen når ledeteksten mangler. */
export function JustertFeltrad(props: FeltradProps) {
  return (
    <Rad {...props} colSpan={props.label ? 1 : 3} valueClassName="text-left" />
  );
}

/** Rader fra beregnede felter. Resultatcellen vises bare på rader med resultat. */
export function Feltrader({ rows }: Rader) {
  const harResultat = rows.some((row) => Boolean(row?.result));
  return rows.map((row, index) =>
    row ? (
      <Rad
        key={index}
        label={row.label}
        labelBold={row.labelBold}
        resultCell={harResultat && row.result && <td>= {row.result}</td>}
      >
        {row.value}
      </Rad>
    ) : null,
  );
}

/** Rader med justerte celler og en felles resultatkolonne når minst én rad har resultat. */
export function JusterteFeltrader({ rows }: Rader) {
  const harResultat = rows.some((row) => Boolean(row?.result));
  return rows.map((row, index) =>
    row ? (
      <Rad
        key={index}
        label={row.label}
        labelBold={row.labelBold}
        colSpan={row.label ? 1 : row.result ? 2 : 3}
        valueClassName="text-left"
        resultCell={harResultat && <td> = {row.result}</td>}
      >
        {row.value}
      </Rad>
    ) : null,
  );
}
