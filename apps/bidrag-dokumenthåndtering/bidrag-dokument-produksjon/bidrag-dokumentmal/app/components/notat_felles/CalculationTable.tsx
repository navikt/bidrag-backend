import ord from "~/tekster/ord.yaml";
import tekster from "~/tekster";
import {
  DokumentTabell,
  type RowContent,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";

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
      <DokumentTabell
        layoutAuto
        kolonner={[
          { tittel: ord.beskrivelse, bredde: "250px" },
          ...(hasCalulcation
            ? [{ tittel: tekster.tabell.felles.beregning, bredde: "250px" }]
            : []),
          { tittel: ord.beløp, bredde: "50px" },
        ]}
      >
        {data.map((rad, index) => (
          <Tabellrad key={index} index={index}>
            <Tabellcelle labelBold={rad.labelBold}>{rad.label}</Tabellcelle>
            {hasCalulcation && <Tabellcelle>{rad.calculation}</Tabellcelle>}
            <Tabellcelle>{rad.result}</Tabellcelle>
          </Tabellrad>
        ))}
      </DokumentTabell>
    </div>
  );
}
