import { Kilde, TypeArManedsperiode } from "~/types/Api";
import { formatPeriode } from "~/utils/date-utils";
import KildeIcon from "~/components/KildeIcon";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import { tekst } from "~/utils/tekst";
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
    <DokumentTabell
      layoutAuto
      bredde="400px"
      kolonner={[
        {
          tittel: tekst(
            "felles.tabell.felles.fraTilOgMed",
            "Fra og med - Til og med",
          ),
          bredde: "150px",
        },
        {
          tittel: tekst("felles.tabell.felles.status", "Status"),
          bredde: "150px",
        },
        { tittel: "Kilde" },
      ]}
    >
      {data.map((rad, index) => (
        <Tabellrad key={index} index={index}>
          <Tabellcelle>
            {formatPeriode(rad.periode.fom, rad.periode.til)}
          </Tabellcelle>
          <Tabellcelle>{rad.statusVisningsnavn}</Tabellcelle>
          <Tabellcelle>
            <KildeIcon kilde={rad.kilde} />
          </Tabellcelle>
        </Tabellrad>
      ))}
    </DokumentTabell>
  );
}
