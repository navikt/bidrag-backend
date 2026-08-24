import { useActionData, ActionFunctionArgs } from "react-router";
import "../../style/style.css";
import tekster from "~/tekster";
import HeaderFooter from "~/components/notat_felles/HeaderFooter";
import { parseRequestAction, VedtakRequest } from "~/routes/common";
import BrevAdresseKontaktinfo from "~/components/notat_felles/components/BrevAdresseKontaktinfo";
import { VedtakProvider } from "~/components/vedtak_felles/VedtakContext";
import { RenderMode } from "~/components/notat_felles/NotatContext";
import Soknaddetaljer from "~/components/notat_felles/components/Soknaddetaljer";
import { VedtakEndeligTable } from "~/routes/notat.bidrag/Vedtak";
import { VedtakSignatur } from "~/components/vedtak_felles/VedtakSignatur";

export async function action(args: ActionFunctionArgs) {
  return await parseRequestAction(args);
}

export function meta() {
  return [
    { title: tekster.titler.bidrag },
    { name: "description", content: tekster.titler.bidrag },
    { property: "author", content: "bidrag-dokument-produksjon" },
    { property: "subject", content: tekster.titler.bidrag },
  ];
}

export default function ForsideOrkestrertBidragsvedtak() {
  const response = useActionData<VedtakRequest>();
  if (response === undefined) {
    return <div>Oops</div>;
  }

  const data = response.data;
  return (
    <div id="bidrag_notat">
      <VedtakProvider data={data} styling={"V2"}>
        <HeaderFooter
          saksnummer={response.data.saksnummer!}
          renderMode={response.renderForPdf ? RenderMode.PDF : RenderMode.HTML}
        />
        <div className={"container page"}>
          <BrevAdresseKontaktinfo />
          <Soknaddetaljer gjelderFlereSaker={false} />
          <div className={"mb-4"}>
            <h4>Informasjon om klage</h4>
            <p>
              Behandlingen av klagen har ført til endringer utover
              klagevedtaket. Endringene skyldes automatiske justeringer, enten
              indeksregulering eller aldersjustering av bidraget. Eventuelt
              begge deler.
              <p>
                Nedenfor har vi laget en oversikt over nye perioder og
                bidragsbeløp. Du finner relevante vedtak vedlagt
              </p>
            </p>
          </div>
          <VedtakEndeligTable data={data.vedtakDetaljer?.resultat} />
          <div className={"pt-4"}>
            <h4>Endringer etter vedtaket</h4>
            <p>
              Hvis det har skjedd faktiske endringer etter at det påklagede
              vedtaket ble fattet, for eksempel i inntekt, bosted eller andre
              forhold som kan ha betydning for bidraget, må du sende en ny
              søknad om endring av barnebidraget.
            </p>
            <h4 className={"pt-2"}>Har du spørsmål?</h4>
            <p>
              Du kan finne nyttig generell informasjon på nav.no/barnebidrag.
            </p>
          </div>
          <VedtakSignatur
            saksbehandler={data.saksbehandler!}
            enhet={data.kontaktInfo!}
          />
        </div>
      </VedtakProvider>
    </div>
  );
}
