import ord from "~/tekster/ord.json";
import tekster from "~/tekster";
import { CommonTable, RowContent } from "~/components/CommonTable";

type CalculationTableRowProps = {
  label: RowContent;
  labelBold?: boolean;
  calculation?: RowContent;
  result: RowContent;
};
type CalculationTableProps = {
  data: CalculationTableRowProps[];
  title?: string;
};
export default function CalculationTable({
  data,
  title,
}: CalculationTableProps) {
  const hasCalulcation = data.some((row) => row.calculation);
  return (
    <div>
      {title && <h4>{title}</h4>}
      <CommonTable
        layoutAuto
        data={{
          headers: [
            {
              name: ord.beskrivelse,
              width: "250px",
            },
            hasCalulcation
              ? {
                  name: tekster.tabell.felles.beregning,
                  width: "250px",
                }
              : undefined,
            {
              name: ord.beløp,
              width: "50px",
            },
          ].filter((d) => d != undefined),
          rows: data.map((r) => ({
            columns: [
              {
                content: r.label,
                labelBold: r.labelBold,
              },
              hasCalulcation && {
                content: r.calculation,
              },
              {
                content: r.result,
              },
            ].filter((d) => typeof d != "boolean"),
          })),
        }}
      />
    </div>
  );
}
