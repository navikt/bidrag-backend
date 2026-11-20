import { useActionData, ActionFunctionArgs } from "react-router";
import "../../style/style.css";
import tekster from "~/tekster/vedtak/forside.yaml";
import HeaderFooter from "~/components/notat_felles/HeaderFooter";
import { parseRequestAction, VedtakRequest } from "~/routes/common";
import BrevAdresseKontaktinfo from "~/components/notat_felles/components/BrevAdresseKontaktinfo";
import { VedtakProvider } from "~/components/vedtak_felles/VedtakContext";
import { RenderMode } from "~/components/notat_felles/NotatContext";
import Soknaddetaljer from "~/components/notat_felles/components/Soknaddetaljer";
import { VedtakEndeligTable } from "~/routes/notat/begrunnelse/bidrag/Vedtak";
import { VedtakSignatur } from "~/components/vedtak_felles/VedtakSignatur";

export async function action(args: ActionFunctionArgs) {
  return await parseRequestAction(args);
}

export function meta() {
  return [
    { title: tekster.tittel },
    { name: "description", content: tekster.tittel },
    { property: "author", content: "bidrag-dokument-produksjon" },
    { property: "subject", content: tekster.tittel },
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
            <h4>{tekster.informasjonOmKlage.tittel}</h4>
            <p>
              {tekster.informasjonOmKlage.endringer}
              <p>{tekster.informasjonOmKlage.oversikt}</p>
            </p>
          </div>
          <VedtakEndeligTable data={data.vedtakDetaljer?.resultat} />
          <div className={"pt-4"}>
            <h4>{tekster.endringerEtterVedtaket.tittel}</h4>
            <p>{tekster.endringerEtterVedtaket.tekst}</p>
            <h4 className={"pt-2"}>{tekster.spørsmål.tittel}</h4>
            <p>{tekster.spørsmål.tekst}</p>
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
