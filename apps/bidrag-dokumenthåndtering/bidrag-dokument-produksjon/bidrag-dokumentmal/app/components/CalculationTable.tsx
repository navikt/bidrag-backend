import type { RowContent } from "~/components/DokumentTabell";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import { tekst } from "~/utils/tekst";

export type CalculationTableRow = {
  label: RowContent;
  labelBold?: boolean;
  calculation?: RowContent;
  result: RowContent;
};

type CalculationTableProps = {
  data: CalculationTableRow[];
  title?: string;
  summary?: {
    label: RowContent;
    result: RowContent;
  };
  width?: string;
  simpleStyling?: boolean;
  className?: string;
  withContainer?: boolean;
  alignResults?: boolean;
  resultColumnWidth?: string;
};

export function CalculationTable({
  data,
  title,
  summary,
  width,
  simpleStyling,
  className,
  withContainer = true,
  alignResults = false,
  resultColumnWidth = "50px",
}: CalculationTableProps) {
  const hasCalculation = data.some(({ calculation }) => calculation);
  const table = (
    <DokumentTabell
      layoutAuto
      storrelse={simpleStyling ? "small" : "large"}
      bredde={width}
      className={className}
      kolonner={[
        { tittel: tekst("ord.beskrivelse", "Beskrivelse"), bredde: "250px" },
        ...(hasCalculation
          ? [
              {
                tittel: tekst("felles.tabell.felles.beregning", "Beregning"),
                bredde: "250px",
              },
            ]
          : []),
        {
          tittel: tekst("ord.beløp", "Beløp"),
          bredde: resultColumnWidth,
          tekstjustering: alignResults ? "right" : undefined,
        },
      ]}
    >
      {data.map((row, index) => (
        <Tabellrad key={index} index={index}>
          <Tabellcelle labelBold={row.labelBold}>{row.label}</Tabellcelle>
          {hasCalculation && <Tabellcelle>{row.calculation}</Tabellcelle>}
          <Tabellcelle textAlign={alignResults ? "right" : undefined}>
            {row.result}
          </Tabellcelle>
        </Tabellrad>
      ))}
      {summary && (
        <Tabellrad index={data.length}>
          <Tabellcelle colSpan={hasCalculation ? 2 : 1} labelBold>
            {summary.label}
          </Tabellcelle>
          <Tabellcelle textAlign={alignResults ? "right" : undefined}>
            {summary.result}
          </Tabellcelle>
        </Tabellrad>
      )}
    </DokumentTabell>
  );

  const titleElement = title && <h4>{title}</h4>;
  return withContainer ? (
    <div>
      {titleElement}
      {table}
    </div>
  ) : (
    <>
      {titleElement}
      {table}
    </>
  );
}

export function MathDivision({
  negativeValue,
  top,
  bottom,
}: {
  negativeValue?: boolean;
  top: string;
  bottom: number | string;
}) {
  return (
    <span>
      {negativeValue && <span>-</span>}
      <span>{top}</span>
      <span> &#247;</span> <span>{bottom}</span>
    </span>
  );
}

export function MathValue({
  value,
  negativeValue,
}: {
  value: string | number;
  negativeValue?: boolean;
}) {
  return (
    <span>
      {negativeValue && <span>-</span>}
      <span>{value}</span>
    </span>
  );
}

export function MathMultiplication({
  negativeValue,
  left,
  right,
}: {
  negativeValue?: boolean;
  left: string;
  right: string | number;
}) {
  return (
    <span>
      {negativeValue && <span>-</span>}
      <span>{left}</span>
      <span> &#xD7;</span> <span>{right}</span>
    </span>
  );
}
