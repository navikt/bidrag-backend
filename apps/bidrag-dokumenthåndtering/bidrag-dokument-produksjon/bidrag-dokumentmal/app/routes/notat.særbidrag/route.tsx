import { useActionData, ActionFunctionArgs } from "react-router";
import "../../style/style.css";
import særbidragTekster from "~/tekster/notat/særbidrag/saksbehandler.json";
import {
  NotatProvider,
  RenderMode,
  Vedlegg,
} from "~/components/notat_felles/NotatContext";
import Boforhold from "~/components/notat_felles/components/Boforhold";
import Soknaddetaljer from "~/components/notat_felles/components/Soknaddetaljer";
import Utgifter from "~/routes/notat.særbidrag/Utgifter";
import Inntekter from "~/components/notat_felles/components/Inntekter";
import Vedtak from "~/routes/notat.særbidrag/Vedtak";
import VedleggBoforhold from "~/components/notat_felles/components/VedleggBoforhold";
import VedleggInntekter from "~/components/notat_felles/components/VedleggInntekter";
import NotatTittel from "~/components/NotatTittel";
import HeaderFooter from "~/components/notat_felles/HeaderFooter";
import VedleggBeregningsDetaljer from "~/routes/notat.særbidrag/VedleggBeregningsDetaljer";
import { parseRequestAction, NotatRequest } from "~/routes/common";

export async function action(args: ActionFunctionArgs) {
  return await parseRequestAction(args);
}

export function meta() {
  return [
    { title: særbidragTekster.tittel },
    { name: "description", content: særbidragTekster.tittel },
    { property: "author", content: "bidrag-dokument-produksjon" },
    { property: "subject", content: særbidragTekster.tittel },
  ];
}

// Vedleggene står i den rekkefølgen de kommer i notatet. Nummeret til hvert vedlegg følger av rekkefølgen.
const vedlegg: Vedlegg[] = ["inntekter", "boforhold", "beregningsdetaljer"];

export default function NotatSærbidrag() {
  const response = useActionData<NotatRequest>();
  if (response === undefined) {
    return <div>Oops</div>;
  }

  const data = response.data;
  return (
    <div id="særbidrag_notat">
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
          <NotatTittel title={særbidragTekster.tittel} />
          <NotatSaksbehandlerSærbidrag />
          <NotatOpplysningerSærbidrag />
        </div>
      </NotatProvider>
    </div>
  );
}

function NotatSaksbehandlerSærbidrag() {
  return (
    <>
      <Utgifter />
      <Inntekter />
      <Boforhold />
      <Vedtak />
    </>
  );
}

// Opplysningene (vedleggene) holdes samlet slik at de senere kan flyttes til en egen route.
function NotatOpplysningerSærbidrag() {
  return (
    <>
      <VedleggInntekter />
      <VedleggBoforhold />
      <VedleggBeregningsDetaljer />
    </>
  );
}
