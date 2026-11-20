import { dateToDDMMYYYY } from "~/utils/date-utils";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import { NotatVedtakDetaljerDto } from "~/types/Api";
import notatTekster from "~/tekster/notat/begrunnelse/felles.yaml";

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
      <div>
        <FeltTabell labelColWidth={"90px"}>
          <Feltrad label={t.saksbehandler}>
            {data.fattetAvSaksbehandler}
          </Feltrad>
          <Feltrad label={t.dato}>
            {dateToDDMMYYYY(data.fattetTidspunkt)}
          </Feltrad>
        </FeltTabell>
      </div>
    </div>
  );
}
