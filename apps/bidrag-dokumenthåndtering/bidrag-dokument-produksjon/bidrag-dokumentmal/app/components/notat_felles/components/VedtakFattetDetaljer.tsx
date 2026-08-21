import { dateToDDMMYYYY } from "~/utils/date-utils";
import { DataViewTable } from "~/components/DataViewTable";
import { NotatVedtakDetaljerDto } from "~/types/Api";

export function VedtakFattetDetaljer({
  data,
}: {
  data: NotatVedtakDetaljerDto;
}) {
  if (!data.erFattet) return null;
  return (
    <div className={"mt-medium"}>
      <h2 style={{ marginBottom: "0" }}>Ferdigstilt</h2>
      <DataViewTable
        labelColWidth={"90px"}
        data={[
          {
            label: "Saksbehandler",
            value: data.fattetAvSaksbehandler,
          },
          {
            label: "Dato",
            value: dateToDDMMYYYY(data.fattetTidspunkt),
          },
        ]}
      />
    </div>
  );
}
