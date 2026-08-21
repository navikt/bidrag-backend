import React from "react";

export interface DataViewTableData {
  label?: string;
  labelBold?: boolean;
  value?: string | number | React.ReactNode;
  result?: string | number | React.ReactNode;
  additionalInfo?: string | number;
}

interface DataViewTableProps {
  data: DataViewTableData[]; // Array of data objects
  title?: string;
  width?: string;
  labelColWidth?: string;
  gap?: string;
  className?: string;
}

export const DataViewTable: React.FC<DataViewTableProps> = ({
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
              <tr
                key={rowIndex + "-" + row.value + "-" + title + "-" + row.label}
              >
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
                <td>
                  <>{row.value}</>
                </td>
                {harResultat && row.result && <td>= {row.result}</td>}
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
