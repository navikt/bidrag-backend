import { Kilde, TypeArManedsperiode } from "~/types/Api";
import { formatPeriode } from "~/utils/date-utils";
import KildeIcon from "~/components/KildeIcon";
import tekster from "~/tekster";
import { CommonTable } from "~/components/CommonTable";

type TableData = {
  periode: TypeArManedsperiode;
  statusVisningsnavn?: string;
  kilde: Kilde;
};

type SimpleTableProps = {
  data: TableData[];
};

export function SimpleTable({ data }: SimpleTableProps) {
  if (data.length === 0) return "Ingen offentlige opplysninger";
  return (
    <CommonTable
      layoutAuto
      width={"400px"}
      data={{
        headers: [
          {
            name: tekster.tabell.felles.fraTilOgMed,
            width: "150px",
          },
          {
            name: tekster.tabell.felles.status,
            width: "150px",
          },
          {
            name: "Kilde",
          },
        ],
        rows: data.map((d) => ({
          columns: [
            { content: formatPeriode(d.periode.fom, d.periode.til) },
            { content: d.statusVisningsnavn },
            { content: <KildeIcon kilde={d.kilde} /> },
          ],
        })),
      }}
    />
  );
}
