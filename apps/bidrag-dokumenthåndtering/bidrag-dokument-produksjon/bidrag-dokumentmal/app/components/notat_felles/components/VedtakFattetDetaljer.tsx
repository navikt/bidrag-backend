import { dateToDDMMYYYY } from "~/utils/date-utils";
import { DataViewTable } from "~/components/DataViewTable";
import { NotatVedtakDetaljerDto } from "~/types/Api";
import notatTekster from "~/tekster/notat/felles/saksbehandler.json";

const t = notatTekster.vedtakFattetDetaljer;

export function VedtakFattetDetaljer({
  data,
}: {
  data: NotatVedtakDetaljerDto;
}) {
  if (!data.erFattet) return null;
  return (
    <div className={"mt-medium"}>
      <h2 style={{ marginBottom: "0" }}>{t.tittel}</h2>
      <DataViewTable
        labelColWidth={"90px"}
        data={[
          {
            label: t.saksbehandler,
            value: data.fattetAvSaksbehandler,
          },
          {
            label: t.dato,
            value: dateToDDMMYYYY(data.fattetTidspunkt),
          },
        ]}
      />
    </div>
  );
}
