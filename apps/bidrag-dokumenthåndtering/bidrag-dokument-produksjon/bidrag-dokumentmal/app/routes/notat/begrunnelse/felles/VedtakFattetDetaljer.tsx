import { dateToDDMMYYYY } from "~/utils/date-utils";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import { NotatVedtakDetaljerDto } from "~/types/Api";
import { tekst } from "~/utils/tekst";
export function VedtakFattetDetaljer({
  data,
}: {
  data: NotatVedtakDetaljerDto;
}) {
  if (!data.erFattet) return null;
  return (
    <div className={"mt-medium"}>
      <h2 style={{ marginBottom: "0" }}>
        {tekst(
          "notat.begrunnelse.felles.vedtakFattetDetaljer.tittel",
          "Ferdigstilt",
        )}
      </h2>
      <div>
        <FeltTabell labelColWidth={"90px"}>
          <Feltrad
            label={tekst(
              "notat.begrunnelse.felles.vedtakFattetDetaljer.saksbehandler",
              "Saksbehandler",
            )}
          >
            {data.fattetAvSaksbehandler}
          </Feltrad>
          <Feltrad
            label={tekst(
              "notat.begrunnelse.felles.vedtakFattetDetaljer.dato",
              "Dato",
            )}
          >
            {dateToDDMMYYYY(data.fattetTidspunkt)}
          </Feltrad>
        </FeltTabell>
      </div>
    </div>
  );
}
