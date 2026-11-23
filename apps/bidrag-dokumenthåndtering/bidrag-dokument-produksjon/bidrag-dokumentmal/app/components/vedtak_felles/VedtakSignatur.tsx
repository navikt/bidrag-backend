import { dateToDDMMYYYY } from "~/utils/date-utils";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
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
      <div>
        <FeltTabell labelColWidth={"400px"}>
          <Feltrad label={""}>{enhet.navn}</Feltrad>
          <Feltrad label={""}>{saksbehandler.fornavnEtternavn}</Feltrad>
          <Feltrad label={""}>{dateToDDMMYYYY(new Date())}</Feltrad>
        </FeltTabell>
      </div>
    </div>
  );
}
