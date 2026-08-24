import { dateToDDMMYYYY } from "~/utils/date-utils";
import { DataViewTable } from "~/components/DataViewTable";
import { Saksbehandler, EnhetKontaktInfo } from "~/types/Api";

export function VedtakSignatur({
  saksbehandler,
  enhet,
}: {
  saksbehandler: Saksbehandler;
  enhet: EnhetKontaktInfo;
}) {
  return (
    <div className={"mt-medium"}>
      <DataViewTable
        labelColWidth={"400px"}
        data={[
          {
            label: "",
            value: enhet.navn,
          },
          {
            label: "",
            value: saksbehandler.fornavnEtternavn,
          },
          {
            label: "",
            value: dateToDDMMYYYY(new Date()),
          },
        ]}
      />
    </div>
  );
}
