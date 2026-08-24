import { Fragment, ReactElement } from "react";
import { useTheme } from "~/components/notat_felles/ThemeContext";

export type RowContent = ReactElement | string | number | null | undefined;
export type TableColumn = {
  width?: string;
  colSpan?: number;
  colSpanNegative?: number;
  fullSpan?: boolean;
  labelBold?: boolean;
  textAlign?: "left" | "right" | "center";
  content: RowContent;
};

export type TableRow = {
  borderBottom?: boolean;
  skipBorderBottom?: boolean;
  skipPadding?: boolean;
  className?: string;
  periodColumn?: RowContent;
  zebraStripe?: boolean;
  columns: TableColumn[];
  expandableContent?: TableColumn[];
};
export type TableHeader = {
  name: string;
  width?: string;
  textAlign?: "left" | "right" | "center";
};
export type TableData = {
  headers: TableHeader[];
  rows: TableRow[];
};
type CommonTableProps = {
  width?: string;
  layoutAuto?: boolean;
  size?: "small" | "large";
  data: TableData;
  className?: string;
};
export function CommonTable({
  layoutAuto,
  size = "large",
  data: { headers, rows },
  className,
  width,
}: CommonTableProps) {
  const { styling } = useTheme();
  const cellV2StylingOnlyBorder =
    "border-b border-solid border-t-0 border-r-0 border-l-0";
  const expressiveStyling = styling == "V2" && size == "large";
  const cellV2Styling =
    "pb-2 pt-2 pl-3 pr-3 border-b border-solid border-t-0 border-r-0 border-l-0";
  const cellSimpleStyling =
    "pb-1 pt-1 border-b border-solid border-t-0 border-r-0 border-l-0";
  const cellV2StylingWithoutBorder = "pb-2 pt-2 pl-3 pr-3";
  const style = expressiveStyling
    ? { width: "650px" }
    : { width: width, maxWidth: "600px" };

  function renderRow(row: TableRow, index: number, isPeriodColumn: boolean) {
    return (
      <tr
        key={"row" + index.toString()}
        style={{
          borderBottom: row.borderBottom ? "1px solid black" : undefined,
        }}
      >
        {row.columns.map((column, colIndex) => {
          return (
            <td
              className={`
              ${
                expressiveStyling
                  ? `${
                      !row.skipPadding
                        ? row.skipBorderBottom || isPeriodColumn
                          ? cellV2StylingWithoutBorder
                          : cellV2Styling
                        : row.skipBorderBottom
                          ? ""
                          : cellV2StylingOnlyBorder
                    } ${row.periodColumn ? false : (row.zebraStripe != false && index % 2 == 1) || row.zebraStripe == true ? "bg-table-bg-even" : ""} ${row.className}`
                  : cellV2StylingOnlyBorder
              } `}
              key={column.content?.toString() + colIndex.toString()}
              colSpan={
                column.fullSpan
                  ? headers.length
                  : column.colSpanNegative
                    ? headers.length - column.colSpanNegative
                    : column.colSpan
              }
              style={{
                fontWeight: column.labelBold ? "bold" : "normal",
                textAlign: column.textAlign ?? "left",
              }}
            >
              {column.content}
            </td>
          );
        })}
      </tr>
    );
  }

  return (
    <table
      className={`table ${className}`}
      style={{
        ...style,
        tableLayout: layoutAuto || styling == "V2" ? "auto" : "fixed",
      }}
    >
      <colgroup>
        {headers.map((header, i) => (
          <col
            key={"colgroup" + header.name + i}
            style={{ width: header.width }}
          ></col>
        ))}
      </colgroup>
      <thead
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        //@ts-ignore
        style={{ wordBreak: "auto-phrase", textWrap: "wrap" }}
        className={expressiveStyling ? "bg-table-header" : ""}
      >
        <tr>
          {headers.map((header, i) => (
            <th
              key={header.name + i}
              className={expressiveStyling ? cellV2Styling : cellSimpleStyling}
              style={{
                width: header.width,
                verticalAlign: "top",
                textAlign: header.textAlign ?? "left",
              }}
            >
              {header.name}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((d, i) => {
          return (
            <Fragment key={"row" + i.toString()}>
              {d.periodColumn &&
                renderRow(
                  {
                    ...d,
                    periodColumn: null,
                    columns: [{ content: d.periodColumn, fullSpan: true }],
                  },
                  1,
                  true,
                )}
              {renderRow(d, i, false)}
              {d.expandableContent && (
                <tr>
                  {d.expandableContent?.map((column, j) => (
                    <td
                      key={
                        "expandable" + (column.content?.toString() ?? "") + j
                      }
                      colSpan={headers.length}
                      className={
                        expressiveStyling
                          ? `table_row_details pb-2 ${i % 2 == 1 ? "bg-table-bg-even" : ""}`
                          : "table_row_details"
                      }
                      style={{ paddingTop: "8px" }}
                    >
                      {column.content}
                    </td>
                  ))}
                </tr>
              )}
            </Fragment>
          );
        })}
      </tbody>
    </table>
  );
}
