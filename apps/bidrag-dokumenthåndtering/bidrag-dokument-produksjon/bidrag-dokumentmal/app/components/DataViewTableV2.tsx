import React from "react";

export interface DataViewTableV2Data {
  label?: string;
  labelBold?: boolean;
  value?: string | number | React.ReactNode;
  result?: string | number | React.ReactNode;
  additionalInfo?: string | number;
  textRight?: boolean;
}

interface DataViewTableV2Props {
  data: DataViewTableV2Data[]; // Array of data objects
  title?: string;
  width?: string;
  labelColWidth?: string;
  gap?: string;
  className?: string;
}

export const DataViewTableV2: React.FC<DataViewTableV2Props> = ({
  data,
  title,
  labelColWidth,
  gap = "5px",
  width,
  className,
}) => {
  const harResultat = data.some((d) => d.result);

  return (
    <div className={className} style={{ width: width }}>
      {title && <h4>{title}</h4>}
      <table style={{ borderSpacing: 0 }}>
        <tbody>
          {data.map((row, rowIndex) => (
            <>
              <tr key={rowIndex + "-" + row.value}>
                {row.label && (
                  <td
                    style={{
                      verticalAlign: "text-top",
                      width: labelColWidth,
                      paddingRight: gap,
                      fontWeight: row.labelBold ? "bold" : "normal",
                    }}
                  >
                    {row.label}:{" "}
                  </td>
                )}
                <td
                  colSpan={!row.label && !row.result ? 3 : !row.label ? 2 : 1}
                  className={
                    row.textRight === true ? "text-right" : "text-left"
                  }
                >
                  <>{row.value}</>
                </td>
                {harResultat && (
                  <td>
                    <> = {row.result}</>
                  </td>
                )}
              </tr>
              {row.additionalInfo && (
                <tr aria-colspan={2} key={rowIndex + "2"}>
                  <td colSpan={2}>{row.additionalInfo}</td>
                </tr>
              )}
            </>
          ))}
        </tbody>
      </table>
    </div>
  );
};
