import { DokumentmalPersonDto } from "~/types/Api";
import { dateToDDMMYYYY } from "~/utils/date-utils";
import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import { useTheme } from "~/components/notat_felles/ThemeContext";
import Person from "~/components/Person";
import { rolleTilVisningsnavnV2 } from "~/utils/visningsnavn";

export default function TableGjelderBarn({
  gjelderBarn,
}: {
  gjelderBarn: DokumentmalPersonDto;
}) {
  const { styling } = useTheme();
  if (styling == "V2") {
    return (
      <DataViewTable
        className={"mb-2 mt-2"}
        data={
          [
            {
              label: rolleTilVisningsnavnV2(gjelderBarn),
              labelBold: true,
              value: (
                <Person
                  fødselsdato={gjelderBarn.fødselsdato!}
                  navn={gjelderBarn.navn!}
                  erBeskyttet={gjelderBarn.erBeskyttet}
                />
              ),
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />
    );
  }
  return (
    <dl style={{ marginBottom: 0 }}>
      <dt>{gjelderBarn.navn}</dt>
      <dd style={{ display: "inline-table" }}>
        / {dateToDDMMYYYY(gjelderBarn.fødselsdato)}
      </dd>
    </dl>
  );
}
