import React, { ReactElement, CSSProperties } from "react";
import ord from "~/tekster/ord.yaml";
import tekster from "~/tekster";
import {
  DokumentTabell,
  type RowContent,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
//file:@ts-ignore
interface CalculationTableData {
  label: RowContent;
  calculation?: RowContent;
  value: RowContent;
}

interface CalculationTableProps {
  data: CalculationTableData[]; // Array of data objects
  title?: string;
  result?: {
    label: RowContent;
    value: RowContent;
  };
  message?: string | ReactElement;
  width?: string;
  simpleStyling?: boolean;
  className?: string;
  style?: CSSProperties;
}

export const CalculationTable: React.FC<CalculationTableProps> = ({
  data,
  result,
  simpleStyling,
  className,
  width,
}) => {
  const harBeregning = data.some((d) => d.calculation);

  return (
    <DokumentTabell
      layoutAuto
      storrelse={simpleStyling ? "small" : "large"}
      bredde={width}
      className={className}
      kolonner={[
        { tittel: ord.beskrivelse, bredde: "250px" },
        ...(harBeregning
          ? [{ tittel: tekster.tabell.felles.beregning, bredde: "250px" }]
          : []),
        { tittel: ord.beløp, bredde: "100px", tekstjustering: "right" },
      ]}
    >
      {data.map((rad, index) => (
        <Tabellrad key={index} index={index}>
          <Tabellcelle>{rad.label}</Tabellcelle>
          {harBeregning && <Tabellcelle>{rad.calculation}</Tabellcelle>}
          <Tabellcelle textAlign="right">{rad.value}</Tabellcelle>
        </Tabellrad>
      ))}
      {result && (
        <Tabellrad index={data.length}>
          <Tabellcelle colSpan={harBeregning ? 2 : 1} labelBold>
            {result.label}
          </Tabellcelle>
          <Tabellcelle textAlign="right">{result.value}</Tabellcelle>
        </Tabellrad>
      )}
    </DokumentTabell>
  );
};

interface MathDivisionProps {
  negativeValue?: boolean;
  top: string;
  bottom: number | string;
}

export const MathDivision: React.FC<MathDivisionProps> = ({
  negativeValue,
  top,
  bottom,
}) => {
  return (
    <span>
      {negativeValue && <span>-</span>}
      <span>{top}</span>
      <span> &#247;</span> {/* Multiplication symbol */}
      <span>{bottom}</span>
    </span>
  );
};

export const MathValue: React.FC<{
  value: string | number;
  negativeValue?: boolean;
}> = ({ value, negativeValue }) => {
  return (
    <span>
      {negativeValue && <span>-</span>}
      <span>{value}</span>
    </span>
  );
};
interface MathMultiplicationProps {
  negativeValue?: boolean;
  left: string;
  right: string | number;
}

export const MathMultiplication: React.FC<MathMultiplicationProps> = ({
  negativeValue,
  left,
  right,
}) => {
  return (
    <span>
      {negativeValue && <span>-</span>}
      <span>{left}</span>
      <span> &#xD7;</span> {/* Multiplication symbol */}
      <span>{right}</span>
    </span>
  );
};
