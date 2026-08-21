import React, { ReactElement, CSSProperties } from "react";
import tekster from "~/tekster";
import {
  CommonTable,
  TableColumn,
  RowContent,
  TableHeader,
} from "~/components/CommonTable";
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
    <CommonTable
      layoutAuto
      size={simpleStyling ? "small" : "large"}
      width={width}
      className={className}
      data={{
        headers: [
          {
            name: tekster.tabell.felles.beskrivelse,
            width: "250px",
          },
          harBeregning && {
            name: tekster.tabell.felles.beregning,
            width: "250px",
          },
          {
            name: tekster.tabell.felles.beløp,
            width: "100px",
            textAlign: "right",
          },
        ].filter((d) => typeof d != "boolean") as TableHeader[],
        rows: data
          .map((content) => ({
            columns: [
              {
                content: content.label,
              },
              harBeregning && {
                content: content.calculation,
              },
              {
                textAlign: "right",
                content: content.value,
              },
            ].filter((d) => typeof d != "boolean") as TableColumn[],
          }))
          .concat(
            result
              ? [
                  {
                    columns: [
                      {
                        content: result!.label,
                        colSpan: harBeregning ? 2 : 1,
                        labelBold: true,
                      } as TableColumn,
                      {
                        textAlign: "right",
                        content: result!.value,
                      } as TableColumn,
                    ],
                  },
                ]
              : [],
          ),
      }}
    />
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
