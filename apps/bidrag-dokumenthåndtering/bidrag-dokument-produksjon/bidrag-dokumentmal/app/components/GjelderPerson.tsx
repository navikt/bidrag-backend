import { DokumentmalPersonDto } from "~/types/Api";
import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import { PersonV2 } from "~/components/Person";
import { rolleTilVisningsnavnV2 } from "~/utils/visningsnavn";

export default function GjelderPerson({
  rolle,
  visFødselsdato,
}: {
  rolle: DokumentmalPersonDto;
  visFødselsdato?: boolean;
}) {
  return (
    <DataViewTable
      className={"mb-2 mt-2"}
      data={
        [
          {
            label: rolleTilVisningsnavnV2(rolle),
            labelBold: true,
            value: <PersonV2 {...rolle} visFødselsdato={visFødselsdato} />,
          },
        ].filter((d) => d != null) as DataViewTableData[]
      }
    />
  );
}
