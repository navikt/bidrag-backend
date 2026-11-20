import { Kilde, TypeArManedsperiode } from "~/types/Api";
import { formatPeriode } from "~/utils/date-utils";
import KildeIcon from "~/components/KildeIcon";
import tekster from "~/tekster";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";

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
        { tittel: tekster.tabell.felles.fraTilOgMed, bredde: "150px" },
        { tittel: tekster.tabell.felles.status, bredde: "150px" },
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
