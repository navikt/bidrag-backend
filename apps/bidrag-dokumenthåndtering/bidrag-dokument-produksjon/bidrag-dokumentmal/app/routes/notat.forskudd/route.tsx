import { useActionData, ActionFunctionArgs } from "react-router";
import "../../style/style.css";
import Vedtak from "~/routes/notat.forskudd/Vedtak";
import forskuddTekster from "~/tekster/notat/forskudd/saksbehandler.yaml";
import {
  NotatProvider,
  RenderMode,
  Vedlegg,
} from "~/components/notat_felles/NotatContext";
import Soknaddetaljer from "~/components/notat_felles/components/Soknaddetaljer";
import Boforhold from "~/components/notat_felles/components/Boforhold";
import Inntekter from "~/components/notat_felles/components/Inntekter";
import VedleggBoforhold from "~/components/notat_felles/components/VedleggBoforhold";
import VedleggInntekter from "~/components/notat_felles/components/VedleggInntekter";
import NotatTittel from "~/components/NotatTittel";
import HeaderFooter from "~/components/notat_felles/HeaderFooter";
import { parseRequestAction, NotatRequest } from "~/routes/common";
import Virkningstidspunkt from "~/components/notat_felles/components/Virkningstidspunkt";

export async function action(args: ActionFunctionArgs) {
  return await parseRequestAction(args);
}

export function meta() {
  return [
    { title: forskuddTekster.tittel },
    { name: "description", content: forskuddTekster.tittel },
    { property: "author", content: "bidrag-dokument-produksjon" },
    { property: "subject", content: forskuddTekster.tittel },
  ];
}

// Vedleggene står i den rekkefølgen de kommer i notatet. Nummeret til hvert vedlegg følger av rekkefølgen.
const vedlegg: Vedlegg[] = ["boforhold", "inntekter"];

export default function NotatForskudd() {
  const response = useActionData<NotatRequest>();
  if (response === undefined) {
    return <div>Oops</div>;
  }

  const data = response.data;
  return (
    <div id="forskudd_notat">
      <NotatProvider
        data={data}
        styling={"V2"}
        renderPDFVersion={response.renderPDFVersion}
        renderMode={response.renderForPdf ? RenderMode.PDF : RenderMode.HTML}
        vedlegg={vedlegg}
      >
        <HeaderFooter
          saksnummer={response.data.saksnummer}
          renderMode={response.renderForPdf ? RenderMode.PDF : RenderMode.HTML}
        />
        <div className={"container page"}>
          <Soknaddetaljer gjelderFlereSaker={false} />
          <NotatTittel title={forskuddTekster.tittel} />
          <Virkningstidspunkt />
          <NotatSaksbehandlerForskudd />
          <NotatOpplysningerForskudd />
        </div>
      </NotatProvider>
    </div>
  );
}

function NotatSaksbehandlerForskudd() {
  return (
    <>
      <Boforhold />
      <Inntekter />
      <Vedtak />
    </>
  );
}

// Opplysningene (vedleggene) holdes samlet slik at de senere kan flyttes til en egen route.
function NotatOpplysningerForskudd() {
  return (
    <>
      <VedleggBoforhold />
      <VedleggInntekter />
    </>
  );
}
